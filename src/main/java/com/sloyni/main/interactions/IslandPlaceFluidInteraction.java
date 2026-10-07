package com.sloyni.main.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.PlaceFluidInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Overrides the "PlaceFluid" interaction to block fluid placement on protected islands.
 * Registered via getCodecRegistry(Interaction.CODEC).register("PlaceFluid", ...) in ISkyblockPlugin.
 */
public class IslandPlaceFluidInteraction extends PlaceFluidInteraction {
    public static final BuilderCodec<IslandPlaceFluidInteraction> ISLAND_CODEC =
            BuilderCodec.builder(IslandPlaceFluidInteraction.class, IslandPlaceFluidInteraction::new, PlaceFluidInteraction.CODEC).build();

    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType type, @Nonnull InteractionContext context,
                                     @Nullable ItemStack heldItemStack, @Nonnull Vector3i targetBlock,
                                     @Nonnull CooldownHandler cooldownHandler) {
        if (IslandInteractionCheck.isAllowed(context, targetBlock.x(), targetBlock.z())) {
            super.interactWithBlock(world, commandBuffer, type, context, heldItemStack, targetBlock, cooldownHandler);
        } else {
            IslandInteractionCheck.reject(context, "protection.place_denied");
        }
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context,
                                              @Nullable ItemStack itemInHand, @Nonnull World world,
                                              @Nonnull Vector3i targetBlock) {
        if (IslandInteractionCheck.isAllowed(context, targetBlock.x(), targetBlock.z())) {
            super.simulateInteractWithBlock(type, context, itemInHand, world, targetBlock);
        } else {
            IslandInteractionCheck.reject(context, "protection.place_denied");
        }
    }
}
