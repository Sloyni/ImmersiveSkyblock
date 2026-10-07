package com.sloyni.main.commands.user;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.util.Chat;
import javax.annotation.Nonnull;

public class IslandCommand extends AbstractPlayerCommand {
    private final ISkyblockPlugin plugin;

    public IslandCommand(ISkyblockPlugin plugin) {
        super("island", "Teleport to your island or create one");
        this.plugin = plugin;
        addAliases(new String[]{"is"});
        setAllowsExtraArguments(true);
    }


    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        String input = context.getInputString() == null ? "" : context.getInputString().trim();
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "init.required_use_iskyblock"));
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        String[] rawTokens = input.isBlank() ? new String[0] : input.split("\\s+");
        plugin.getIslandCommandHandler().dispatchIslandTokens(player, ref, store, playerRef, world, rawTokens);
    }
}

