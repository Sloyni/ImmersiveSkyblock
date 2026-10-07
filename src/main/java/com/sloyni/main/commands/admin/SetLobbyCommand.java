package com.sloyni.main.commands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.util.Chat;
import java.util.Map;
import javax.annotation.Nonnull;

public class SetLobbyCommand extends AbstractPlayerCommand {
    private final ISkyblockPlugin plugin;

    public SetLobbyCommand(ISkyblockPlugin plugin) {
        super("setlobby", "Set lobby fallback point to your current position");
        this.plugin = plugin;
        requirePermission("iskyblock.setlobby");
    }


    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            Chat.send(context, plugin.tr("lobby.set_failed"));
            return;
        }

        Vector3d pos = transform.getPosition();
        int x = (int) Math.floor(pos.x());
        int y = (int) Math.floor(pos.y());
        int z = (int) Math.floor(pos.z());
        float pitch = transform.getRotation().pitch();
        float yaw = transform.getRotation().yaw();

        plugin.getConfigManager().getConfig().getLobby().setWorldName(world.getName());
        plugin.getConfigManager().getConfig().getLobby().setX(x);
        plugin.getConfigManager().getConfig().getLobby().setY(y);
        plugin.getConfigManager().getConfig().getLobby().setZ(z);
        plugin.getConfigManager().getConfig().getLobby().setPitch(pitch);
        plugin.getConfigManager().getConfig().getLobby().setYaw(yaw);
        plugin.getConfigManager().getConfig().getLobby().setConfigured(true);
        plugin.getConfigManager().save();

        Chat.send(context, plugin.tr("lobby.set_success", Map.of(
            "world", world.getName(),
            "x", String.valueOf(x),
            "y", String.valueOf(y),
            "z", String.valueOf(z)
        )));
    }
}
