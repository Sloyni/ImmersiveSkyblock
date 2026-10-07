package com.sloyni.main.world;

import com.hypixel.hytale.common.util.CompletableFutureUtil;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.VoidWorldGenProvider;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class WorldManager {
    private static final Color DEFAULT_TINT = new Color((byte) 85, (byte) 136, (byte) 26);
    private static final String DEFAULT_ENV = "Env_Zone1_Forests";

    public CompletableFuture<World> createOrLoadVoidWorld(String worldName) {
        World existing = Universe.get().getWorld(worldName);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        if (Universe.get().isWorldLoadable(worldName)) {
            return loadWorld(worldName);
        }

        Path savePath = Universe.get().validateWorldPath(worldName);
        CompletableFuture<World> output = new CompletableFuture<>();
        CompletableFutureUtil._catch(
            Universe.get().getWorldConfigProvider().load(savePath, worldName)
                .thenCompose(worldConfig -> {
                    worldConfig.setWorldGenProvider(new VoidWorldGenProvider(DEFAULT_TINT, DEFAULT_ENV));
                    worldConfig.markChanged();
                    return Universe.get().makeWorld(worldName, savePath, worldConfig);
                })
                .thenAccept(output::complete)
                .exceptionally(ex -> {
                    output.completeExceptionally(ex);
                    return null;
                })
        );
        return output;
    }

    private CompletableFuture<World> loadWorld(String worldName) {
        Path savePath = Universe.get().validateWorldPath(worldName);
        CompletableFuture<World> output = new CompletableFuture<>();
        CompletableFutureUtil._catch(
            Universe.get().getWorldConfigProvider().load(savePath, worldName)
                .thenCompose(cfg -> Universe.get().makeWorld(worldName, savePath, cfg))
                .thenAccept(output::complete)
                .exceptionally(ex -> {
                    output.completeExceptionally(ex);
                    return null;
                })
        );
        return output;
    }
}
