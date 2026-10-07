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
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * Prevents damage to visiting players on islands they don't own.
 * Visiting players are immune to all damage except from other players (if PvP is enabled on the island).
 */
public class VisitorDamageImmunityProtection extends DamageEventSystem {

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull Damage event) {
        if (event.isCancelled()) {
            return;
        }
        try {
            Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
            Player targetPlayer = store.getComponent(targetRef, Player.getComponentType());
            PlayerRef targetPlayerRef = store.getComponent(targetRef, PlayerRef.getComponentType());

            if (targetPlayer == null || targetPlayerRef == null) {
                return;
            }

            EntityStore entityStore = (EntityStore) store.getExternalData();
            World world = entityStore != null ? entityStore.getWorld() : null;
            String worldName = world != null ? world.getName() : null;

            ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
            if (!plugin.getProtectionService().isIslandWorld(worldName)) {
                return;
            }

            // Check if damage source is from another player (PvP is handled separately)
            Damage.Source source = event.getSource();
            if (source instanceof Damage.EntitySource) {
                Ref<EntityStore> attackerRef = ((Damage.EntitySource) source).getRef();
                if (attackerRef != null && store.getComponent(attackerRef, EntityModule.get().getPlayerComponentType()) != null) {
                    // This is PvP, skip this handler (handled by IslandPvpProtection)
                    return;
                }
            }

            // Determine target's position to find which island they're on
            TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }

            int targetX = (int) transform.getPosition().x;
            int targetZ = (int) transform.getPosition().z;
            IslandData island = plugin.getProtectionService().getIslandAtBlock(targetX, targetZ);

            UUID targetUuid = targetPlayerRef.getUuid();

            if (island == null) {
                // Player is in the island world but not on any island zone (void gap).
                // Block ALL damage here so players don't die before VoidKillSystem can teleport them.
                event.setCancelled(true);
                event.setAmount(0.0f);
                neutralizeKnockback(event, buffer, targetRef);
                return;
            }

            // If the target owns or is a member of the island, allow damage
            if (island.canModify(targetUuid)) {
                return;
            }

            // Visitor — always immune to all non-PvP damage (regardless of mob damage toggle)
            event.setCancelled(true);
            event.setAmount(0.0f);
            neutralizeKnockback(event, buffer, targetRef);

        } catch (Exception ex) {
            System.err.println("[ISkyblock] VisitorDamageImmunityProtection error: " + ex.getMessage());
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
