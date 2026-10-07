package com.sloyni.main.events;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Rotation3f;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.addons.oregenerator.OreGeneratorAddon;
import com.sloyni.main.addons.oregenerator.OreGeneratorAddonConfig;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.annotation.Nonnull;

public class OreGeneratorHandler extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final String COBBLESTONE = "Rock_Stone_Cobble";
    private static int LAVA_FLUID_ID = -1;
    private static int WATER_FLUID_ID = -1;
    private static final Random RANDOM = new Random();

    public OreGeneratorHandler() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {
        try {
            ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
            if (plugin == null) return;

            OreGeneratorAddon addon = plugin.getAddonManager().getOreGeneratorAddon();
            if (!addon.isEnabled()) return;

            Vector3i pos = event.getTargetBlock();
            BlockType blockType = event.getBlockType();
            if (pos == null || blockType == null) return;

            String blockKey = blockType.getId();
            if (!COBBLESTONE.equals(blockKey) && !blockKey.contains("Cobble") && !blockKey.contains("cobble")) {
                return;
            }

            int x = pos.x();
            int y = pos.y();
            int z = pos.z();

            EntityStore entityStore = (EntityStore) store.getExternalData();
            if (entityStore == null) return;

            World world = entityStore.getWorld();
            if (world == null) return;

            Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
            Player player = entityRef != null ? (Player) store.getComponent(entityRef, Player.getComponentType()) : null;
            Predicate<String> hasPermission = perm -> {
                if (player == null) return false;
                try {
                    return com.sloyni.main.util.PermissionUtil.has(player, perm);
                } catch (Exception e) {
                    return false;
                }
            };

            boolean hasLava = hasNearbyFluid(world, x, y, z, true);
            boolean hasWater = hasNearbyFluid(world, x, y, z, false);

            if (hasLava && hasWater) {
                scheduleRegeneration(world, x, y, z, hasPermission);
            }
        } catch (Exception e) {
            System.err.println("[OreGenerator] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    private static void ensureFluidIds() {
        if (LAVA_FLUID_ID < 0) {
            try {
                LAVA_FLUID_ID = Fluid.getAssetMap().getIndex("Lava");
            } catch (Exception e) {
                LAVA_FLUID_ID = 1;
            }
        }
        if (WATER_FLUID_ID < 0) {
            try {
                WATER_FLUID_ID = Fluid.getAssetMap().getIndex("Water");
            } catch (Exception e) {
                WATER_FLUID_ID = 2;
            }
        }
    }

    private static boolean hasNearbyFluid(World world, int x, int y, int z, boolean lookingForLava) {
        ensureFluidIds();
        int targetFluidId = lookingForLava ? LAVA_FLUID_ID : WATER_FLUID_ID;
        int[][] offsets = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 0, 1}, {0, 0, -1},
            {0, -1, 0}
        };
        for (int[] offset : offsets) {
            try {
                int fluidId = world.getFluidId(x + offset[0], y + offset[1], z + offset[2]);
                if (fluidId == targetFluidId) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static void scheduleRegeneration(World world, int x, int y, int z, Predicate<String> hasPermission) {
        ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
        if (plugin == null) return;

        long delayMs = plugin.getAddonManager().getOreGeneratorAddon().getRegenDelayMs();

        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() -> {
            world.execute(() -> {
                try {
                    OreGeneratorAddon addon = plugin.getAddonManager().getOreGeneratorAddon();
                    if (!addon.isEnabled()) return;

                    OreGeneratorAddonConfig generatorConfig = addon.getGeneratorConfig();
                    double oreChance = generatorConfig.getEffectiveOreChance(hasPermission);
                    double roll = RANDOM.nextDouble();
                    String blockToPlace;

                    if (roll < oreChance) {
                        blockToPlace = selectRandomOre(generatorConfig, hasPermission);
                    } else {
                        blockToPlace = COBBLESTONE;
                    }

                    try {
                        world.setBlock(x, y, z, blockToPlace, 0);
                    } catch (IllegalArgumentException e) {
                        System.out.println("[OreGenerator] Unknown block '" + blockToPlace + "', falling back to cobblestone. Remove this block from ore_generator config.");
                        world.setBlock(x, y, z, COBBLESTONE, 0);
                        blockToPlace = COBBLESTONE;
                    }

                    if (generatorConfig.isSpawnRubble()) {
                        spawnRubbleOnTop(world, x, y, z);
                    }
                } catch (Exception e) {
                    System.err.println("[OreGenerator] Error in regeneration: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
    }

    private static String selectRandomOre(OreGeneratorAddonConfig generatorConfig, Predicate<String> hasPermission) {
        List<OreGeneratorAddonConfig.OreEntry> ores = generatorConfig.getEffectiveOres(hasPermission);
        double totalWeight = generatorConfig.getEffectiveTotalWeight(hasPermission);

        if (ores == null || ores.isEmpty() || totalWeight <= 0.0d) {
            return COBBLESTONE;
        }

        double roll = RANDOM.nextDouble() * totalWeight;
        double cumulative = 0.0d;
        for (OreGeneratorAddonConfig.OreEntry entry : ores) {
            cumulative += entry.getWeight();
            if (roll < cumulative) {
                String blockId = entry.getBlockId();
                return blockId != null ? blockId.trim() : COBBLESTONE;
            }
        }
        return COBBLESTONE;
    }

    private static void spawnRubbleOnTop(World world, int x, int y, int z) {
        try {
            Vector3d position = new Vector3d(x + 0.5d, y + 1.2d, z + 0.5d);
            float vx = (RANDOM.nextFloat() - 0.5f) * 0.5f;
            float vy = 0.5f + (RANDOM.nextFloat() * 0.3f);
            float vz = (RANDOM.nextFloat() - 0.5f) * 0.5f;
            ItemStack rubbleItem = new ItemStack("Rubble_Stone", 1);
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            Holder<EntityStore> itemHolder = ItemComponent.generateItemDrop(store, rubbleItem, position, Rotation3f.ZERO, vx, vy, vz);
            if (itemHolder != null) {
                store.addEntity(itemHolder, AddReason.SPAWN);
            }
        } catch (Exception ignored) {
        }
    }
}
