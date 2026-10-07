package com.sloyni.main.protection;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import javax.annotation.Nonnull;

/**
 * Prevents player-vs-player damage inside island zones.
 * All islands share one world (single-world mode), so PvP is disabled globally on any island cell.
 */
public class IslandPvpProtection extends DamageEventSystem {

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

            // Both must be players
            if (store.getComponent(attackerRef, EntityModule.get().getPlayerComponentType()) == null
                    || store.getComponent(targetRef, EntityModule.get().getPlayerComponentType()) == null) {
                return;
            }

            EntityStore entityStore = (EntityStore) store.getExternalData();
            World world = entityStore != null ? entityStore.getWorld() : null;
            String worldName = world != null ? world.getName() : null;

            ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
            if (!plugin.getProtectionService().isIslandWorld(worldName)) {
                return;
            }

            // Determine target's position to find their island
            TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }

            int targetX = (int) transform.getPosition().x;
            int targetZ = (int) transform.getPosition().z;
            IslandData island = plugin.getProtectionService().getIslandAtBlock(targetX, targetZ);

            if (island != null) {
                event.setCancelled(true);
                event.setAmount(0.0f);
                // Neutralize knockback from both the event metadata and the command buffer
                neutralizeKnockback(event, buffer, targetRef);
            }
        } catch (Exception ex) {
            System.err.println("[ISkyblock] IslandPvpProtection error: " + ex.getMessage());
        }
    }

    private void neutralizeKnockback(Damage event, CommandBuffer<EntityStore> buffer, Ref<EntityStore> targetRef) {
        // 1) Zero the KnockbackComponent attached as Damage event metadata (same object as on entity)
        try {
            KnockbackComponent knockback = event.getMetaStore().getMetaObject(Damage.KNOCKBACK_COMPONENT);
            if (knockback != null) {
                knockback.setVelocity(new Vector3d(0, 0, 0));
                knockback.setDuration(0f);
                knockback.addModifier(0.0);
            }
        } catch (Exception ignored) {}
        // 2) Also zero via the command buffer (catches weapon knockback put before event fired)
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
