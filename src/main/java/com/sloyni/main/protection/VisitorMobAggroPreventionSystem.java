package com.sloyni.main.protection;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import java.lang.reflect.Field;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Prevents mobs from targeting/aggroing visiting players on foreign islands.
 * Every tick, checks all NPCs and clears their target when it points at a visitor.
 */
public class VisitorMobAggroPreventionSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, NPCEntity> npcComponentType;
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;
    private final ComponentType<EntityStore, Player> playerComponentType;

    public VisitorMobAggroPreventionSystem() {
        this.npcComponentType = NPCEntity.getComponentType();
        this.transformComponentType = TransformComponent.getComponentType();
        this.playerComponentType = Player.getComponentType();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return (Query<EntityStore>) Query.and(
            (Query) npcComponentType,
            (Query) transformComponentType
        );
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> buffer) {
        ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
        if (plugin == null) {
            return;
        }

        EntityStore entityStore = store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        if (world == null || !plugin.getIslandAccessController().isIslandRoutingWorld(world)) {
            return;
        }

        NPCEntity npc = (NPCEntity) chunk.getComponent(index, npcComponentType);
        if (npc == null) {
            return;
        }

        Role role = npc.getRole();
        if (role == null) {
            return;
        }

        MarkedEntitySupport markedSupport = getMarkedEntitySupportViaReflection(role);
        if (markedSupport == null) {
            return;
        }

        Ref<EntityStore>[] targets = markedSupport.getEntityTargets();
        if (targets == null || targets.length == 0) {
            return;
        }

        TransformComponent transform = (TransformComponent) chunk.getComponent(index, transformComponentType);
        if (transform == null) {
            return;
        }

        int npcX = (int) transform.getPosition().x;
        int npcZ = (int) transform.getPosition().z;
        IslandData island = plugin.getProtectionService().getIslandAtBlock(npcX, npcZ);
        if (island == null) {
            return;
        }

        int slotCount = markedSupport.getMarkedEntitySlotCount();
        for (int slot = 0; slot < slotCount; slot++) {
            Ref<EntityStore> targetRef = markedSupport.getMarkedEntityRef(slot);
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }

            Player targetPlayer = store.getComponent(targetRef, playerComponentType);
            if (targetPlayer == null) {
                continue;
            }

            PlayerRef targetPlayerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
            if (targetPlayerRef == null) {
                continue;
            }

            UUID targetUuid = targetPlayerRef.getUuid();
            if (targetUuid == null) {
                continue;
            }

            if (!island.canModify(targetUuid)) {
                // Visitor — let mobs target them only if visitor mob damage is enabled
                if (!island.isVisitorMobDamageEnabled()) {
                    markedSupport.clearMarkedEntity(slot);
                }
            }
        }
    }

    /**
     * Accède à MarkedEntitySupport via réflexion car la méthode Role.getMarkedEntitySupport()
     * a été supprimée dans la dernière version de Hytale.
     * On parcourt les champs déclarés de l'objet Role pour trouver celui de type MarkedEntitySupport.
     */
    @Nullable
    private static MarkedEntitySupport getMarkedEntitySupportViaReflection(Role role) {
        Class<?> roleClass = role.getClass();
        // On remonte dans la hiérarchie de classes au cas où le champ est dans une superclasse
        while (roleClass != null && roleClass != Object.class) {
            for (Field field : roleClass.getDeclaredFields()) {
                if (MarkedEntitySupport.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        return (MarkedEntitySupport) field.get(role);
                    } catch (Exception ignored) {
                        return null;
                    }
                }
            }
            roleClass = roleClass.getSuperclass();
        }
        return null;
    }
}

