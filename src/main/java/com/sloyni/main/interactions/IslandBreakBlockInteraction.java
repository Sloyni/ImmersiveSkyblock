package com.sloyni.main.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.BreakBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Overrides the "BreakBlock" interaction (initial mining hit) to block breaking on protected islands.
 */
public class IslandBreakBlockInteraction extends BreakBlockInteraction {
    public static final BuilderCodec<IslandBreakBlockInteraction> ISLAND_CODEC =
            BuilderCodec.builder(IslandBreakBlockInteraction.class, IslandBreakBlockInteraction::new, BreakBlockInteraction.CODEC).build();

    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType type, @Nonnull InteractionContext context,
                                     @Nullable ItemStack heldItem, @Nonnull Vector3i targetBlock,
                                     @Nonnull CooldownHandler cooldownHandler) {
        // Only enforce bedrock/wall/island rules in the skyblock world
        ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
        if (plugin != null && plugin.getProtectionService().isIslandWorld(world.getName())) {
            if (IslandInteractionCheck.isBedrockAt(world, targetBlock.x(), targetBlock.y(), targetBlock.z())) {
                IslandInteractionCheck.reject(context, "protection.bedrock_denied");
                return;
            }
            if (IslandInteractionCheck.isProtectedWallAt(world, targetBlock.x(), targetBlock.y(), targetBlock.z())) {
                IslandInteractionCheck.reject(context, "protection.wall_denied");
                return;
            }
            if (!IslandInteractionCheck.isAllowed(context, targetBlock.x(), targetBlock.z())) {
                IslandInteractionCheck.reject(context, "protection.break_denied");
                return;
            }
        }
        super.interactWithBlock(world, commandBuffer, type, context, heldItem, targetBlock, cooldownHandler);
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context,
                                              @Nullable ItemStack itemInHand, @Nonnull World world,
                                              @Nonnull Vector3i targetBlock) {
        if (IslandInteractionCheck.isBedrockAt(world, targetBlock.x(), targetBlock.y(), targetBlock.z())) {
            IslandInteractionCheck.reject(context, "protection.bedrock_denied");
            return;
        }
        if (IslandInteractionCheck.isProtectedWallAt(world, targetBlock.x(), targetBlock.y(), targetBlock.z())) {
            IslandInteractionCheck.reject(context, "protection.wall_denied");
            return;
        }
        if (IslandInteractionCheck.isAllowed(context, targetBlock.x(), targetBlock.z())) {
            super.simulateInteractWithBlock(type, context, itemInHand, world, targetBlock);
        } else {
            IslandInteractionCheck.reject(context, "protection.break_denied");
        }
    }
}
