package com.sloyni.main.protection;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * Prevents damage to players from mobs/animals on islands they don't own.
 * Protects against:
 * - Tamed mobs (role name starts with "tamed_")
 * - Wild animals/mobs on another player's island
 */
public class IslandMobProtectionHandler extends DamageEventSystem {

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull Damage event) {
        if (event.isCancelled()) {
            return;
        }
        try {
            Damage.Source source = event.getSource();
            if (!(source instanceof Damage.EntitySource)) {
                return;
            }

            Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
            Ref<EntityStore> attackerRef = ((Damage.EntitySource) source).getRef();

            if (targetRef == null || attackerRef == null || !targetRef.isValid() || !attackerRef.isValid()) {
                return;
            }

            // Target must be a player
            Player targetPlayer = store.getComponent(targetRef, Player.getComponentType());
            PlayerRef targetPlayerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
            if (targetPlayer == null || targetPlayerRef == null) {
                return;
            }

            // Attacker must be an NPC (mob/animal)
            NPCEntity attackerNpc = store.getComponent(attackerRef, NPCEntity.getComponentType());
            if (attackerNpc == null) {
                return;
            }

            EntityStore entityStore = (EntityStore) store.getExternalData();
            World world = entityStore != null ? entityStore.getWorld() : null;
            String worldName = world != null ? world.getName() : null;

            ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
            if (!plugin.getProtectionService().isIslandWorld(worldName)) {
                return;
            }

            // Determine target's position to find which island they're on
            TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetTransform == null) {
                return;
            }

            int targetX = (int) targetTransform.getPosition().x;
            int targetZ = (int) targetTransform.getPosition().z;
            IslandData island = plugin.getProtectionService().getIslandAtBlock(targetX, targetZ);

            if (island == null) {
                // Not on any island, allow damage
                return;
            }

            UUID targetUuid = targetPlayerRef.getUuid();

            // If the target owns or is a member of the island, allow damage
            if (island.canModify(targetUuid)) {
                return;
            }

            // Visitor — always immune to mob damage
            event.setCancelled(true);
            event.setAmount(0.0f);
            neutralizeKnockback(event, buffer, targetRef);

        } catch (Exception ex) {
            System.err.println("[ISkyblock] IslandMobProtectionHandler error: " + ex.getMessage());
        }
    }

    private void neutralizeKnockback(Damage event, CommandBuffer<EntityStore> buffer, Ref<EntityStore> targetRef) {
        try {
            KnockbackComponent knockback = event.getMetaStore().getMetaObject(Damage.KNOCKBACK_COMPONENT);
            if (knockback != null) {
                knockback.setVelocity(new Vector3d(0, 0, 0));
                knockback.setDuration(0f);
                knockback.addModifier(0.0);
            }
        } catch (Exception ignored) {}
        try {
            KnockbackComponent knockback = (KnockbackComponent) buffer.getComponent(targetRef, KnockbackComponent.getComponentType());
            if (knockback != null) {
                knockback.setVelocity(new Vector3d(0, 0, 0));
                knockback.setDuration(0f);
                knockback.addModifier(0.0);
            }
        } catch (Exception ignored) {}
        buffer.tryRemoveComponent(targetRef, KnockbackComponent.getComponentType());
    }
}
