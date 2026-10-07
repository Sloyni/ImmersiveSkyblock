package com.sloyni.main.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.teleport.PendingTeleport;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import java.util.concurrent.CompletableFuture;

public final class TeleportUtil {
    private TeleportUtil() {}

    public static CompletableFuture<Void> teleport(PlayerRef playerRef, World sourceWorld, World targetWorld, int x, int y, int z, float pitch, float yaw) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        long chunkPos = ChunkUtil.indexChunkFromBlock(x, z);
        targetWorld.getChunkAsync(chunkPos).thenRun(() -> {
            sourceWorld.execute(() -> {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref == null) {
                    done.completeExceptionally(new IllegalStateException("Player reference not available"));
                    return;
                }

                ref.getStore().tryRemoveComponent(ref, PendingTeleport.getComponentType());
                // Player transform pitch uses the opposite sign of teleport transform pitch.
                Transform transform = new Transform(x + 0.5D, y, z + 0.5D, -pitch, yaw, 0f);
                boolean crossWorld = !sourceWorld.getName().equals(targetWorld.getName());
                if (crossWorld) {
                    ref.getStore().addComponent(ref, Teleport.getComponentType(), Teleport.createForPlayer(targetWorld, transform));
                } else {
                    ref.getStore().addComponent(ref, Teleport.getComponentType(), Teleport.createForPlayer(transform));
                }
                done.complete(null);
            });
        });

        return done;
    }
}
