package com.sloyni.main.interactions;

import com.hypixel.hytale.builtin.adventure.farming.interactions.HarvestCropInteraction;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Overrides the "HarvestCrop" interaction to block harvesting on protected islands.
 */
public class IslandHarvestCropInteraction extends HarvestCropInteraction {
    public static final BuilderCodec<IslandHarvestCropInteraction> ISLAND_CODEC =
            BuilderCodec.builder(IslandHarvestCropInteraction.class, IslandHarvestCropInteraction::new, HarvestCropInteraction.CODEC).build();

    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType type, @Nonnull InteractionContext context,
                                     @Nullable ItemStack heldItem, @Nonnull Vector3i targetBlock,
                                     @Nonnull CooldownHandler cooldownHandler) {
        if (IslandInteractionCheck.isAllowed(context, targetBlock.x(), targetBlock.z())) {
            super.interactWithBlock(world, commandBuffer, type, context, heldItem, targetBlock, cooldownHandler);
        } else {
            IslandInteractionCheck.reject(context, "protection.interact_denied");
        }
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context,
                                              @Nullable ItemStack itemInHand, @Nonnull World world,
                                              @Nonnull Vector3i targetBlock) {
        if (IslandInteractionCheck.isAllowed(context, targetBlock.x(), targetBlock.z())) {
            super.simulateInteractWithBlock(type, context, itemInHand, world, targetBlock);
        } else {
            IslandInteractionCheck.reject(context, "protection.interact_denied");
        }
    }
}
