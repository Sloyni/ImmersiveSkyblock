package com.sloyni.main.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.RefillContainerInteraction;

/**
 * Overrides the "RefillContainer" interaction to block bucket-pickup of fluids on protected islands.
 * "RefillContainer" fires when a player uses an empty bucket to collect water or lava from the world.
 */
public class IslandRefillContainerInteraction extends RefillContainerInteraction {
    public static final BuilderCodec<IslandRefillContainerInteraction> ISLAND_CODEC =
            BuilderCodec.builder(IslandRefillContainerInteraction.class, IslandRefillContainerInteraction::new, RefillContainerInteraction.CODEC).build();

    @Override
    protected void firstRun(InteractionType type, InteractionContext context, CooldownHandler cooldownHandler) {
        int[] coords = IslandInteractionCheck.extractCoords(context);
        if (coords != null && !IslandInteractionCheck.isAllowed(context, coords[0], coords[2])) {
            IslandInteractionCheck.reject(context, "protection.interact_denied");
            return;
        }
        super.firstRun(type, context, cooldownHandler);
    }
}
