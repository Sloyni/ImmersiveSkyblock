package com.sloyni.main.protection;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.util.Chat;
import javax.annotation.Nonnull;

/**
 * Protects interactive block actions on is lands: containers, fluid placement, harvest, rubble.
 * Mirrors the logic of IslandInteractionProtectionHandler from HTSkyBlock.
 */
public class IslandUseBlockProtection extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    public IslandUseBlockProtection() {
        super(UseBlockEvent.Pre.class);
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull UseBlockEvent.Pre event) {
        try {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            Player player = chunk.getComponent(index, Player.getComponentType());
            if (player != null && com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island.admin")) {
                return;
            }

            // Only intercept specific interaction categories
            InteractionType interactionType = event.getInteractionType();
            InteractionContext context = event.getContext();
            String blockTypeName = event.getBlockType().toString().toLowerCase();
            String rootId = (context == null || interactionType == null) ? "" : context.getRootInteractionId(interactionType);

            boolean shouldProtect = isContainerInteraction(blockTypeName, interactionType)
                || isFluidInteraction(rootId, interactionType)
                || isHarvestInteraction(blockTypeName)
                || isRubbleInteraction(blockTypeName);

            if (!shouldProtect) {
                return;
            }

            EntityStore ext = (EntityStore) store.getExternalData();
            World world = ext != null ? ext.getWorld() : null;
            String worldName = world != null ? world.getName() : null;

            ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
            if (!plugin.getProtectionService().isIslandWorld(worldName)) {
                return;
            }

            if (plugin.getIslandAccessController().enforcePendingLobbyTeleport(playerRef, world)) {
                event.setCancelled(true);
                return;
            }
            if (plugin.getIslandAccessController().enforceLockedIslandAccess(playerRef, world)) {
                event.setCancelled(true);
                return;
            }

            // Use the interacted block's position for zone lookup
            int bx = event.getTargetBlock().x();
            int bz = event.getTargetBlock().z();
            IslandData island = plugin.getProtectionService().getIslandAtBlock(bx, bz);

            if (island == null) {
                event.setCancelled(true);
                Chat.send(playerRef, plugin.tr(playerRef, "protection.outside_boundary"));
                return;
            }

            if (!plugin.getProtectionService().canModify(island, playerRef.getUuid())) {
                event.setCancelled(true);
                Chat.send(playerRef, plugin.tr(playerRef, "protection.interact_denied"));
            }
        } catch (Exception ex) {
            System.err.println("[ISkyblock] IslandUseBlockProtection error: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Category helpers (ported from htskyblock)
    // ------------------------------------------------------------------

    private boolean isContainerInteraction(String blockName, InteractionType type) {
        if (!containsAny(blockName, "chest", "barrel", "shulker", "hopper", "dropper",
                "dispenser", "furnace", "smoker", "blast", "brewing",
                "storage", "crate", "locker", "container", "inventory")) {
            return false;
        }
        if (type == null) {
            return false;
        }
        String t = type.name().toLowerCase();
        return t.contains("use") || t.contains("interact");
    }

    private boolean isFluidInteraction(String rootId, InteractionType type) {
        String id = rootId == null ? "" : rootId.toLowerCase();
        String typeName = type == null ? "" : type.name().toLowerCase();
        String combined = id + " " + typeName;
        return containsAny(combined, "bucket", "lbucket", "water", "lava",
                "fluid", "pour", "fill", "empty", "container_");
    }

    private boolean isHarvestInteraction(String blockName) {
        return containsAny(blockName, "plant", "crop", "berry", "flower",
                "mushroom", "seed", "sapling", "vine", "herb", "harvest");
    }

    private boolean isRubbleInteraction(String blockName) {
        return containsAny(blockName, "rubble", "debris", "scrap", "pile", "loot");
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String k : keywords) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
