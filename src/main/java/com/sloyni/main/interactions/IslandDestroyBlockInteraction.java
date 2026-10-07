package com.sloyni.main.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.DestroyBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;

/**
 * Overrides the "DestroyBlock" interaction (full block destruction) to block breaking on protected islands.
 */
public class IslandDestroyBlockInteraction extends DestroyBlockInteraction {
    public static final BuilderCodec<IslandDestroyBlockInteraction> ISLAND_CODEC =
            BuilderCodec.builder(IslandDestroyBlockInteraction.class, IslandDestroyBlockInteraction::new, DestroyBlockInteraction.CODEC).build();

    @Override
    protected void firstRun(InteractionType type, InteractionContext context, CooldownHandler cooldownHandler) {
        int[] coords = IslandInteractionCheck.extractCoords(context);
        if (coords != null) {
            World world = null;
            try {
                Player player = context.getEntity().getStore().getComponent(context.getEntity(), Player.getComponentType());
                world = player != null ? player.getWorld() : null;
            } catch (Exception ignored) {
            }
            if (IslandInteractionCheck.isBedrockAt(world, coords[0], coords[1], coords[2])) {
                IslandInteractionCheck.reject(context, "protection.bedrock_denied");
                return;
            }
            if (IslandInteractionCheck.isProtectedWallAt(world, coords[0], coords[1], coords[2])) {
                IslandInteractionCheck.reject(context, "protection.wall_denied");
                return;
            }
            if (!IslandInteractionCheck.isAllowed(context, coords[0], coords[2])) {
                IslandInteractionCheck.reject(context, "protection.break_denied");
                return;
            }
        }
        super.firstRun(type, context, cooldownHandler);
    }
}
