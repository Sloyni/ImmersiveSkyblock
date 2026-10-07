package com.sloyni.main.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class Chat {
    private Chat() {}

    public static Message text(String value) {
        return Message.raw(value);
    }

    public static void send(CommandContext context, String value) {
        context.sendMessage(Message.raw(value));
    }

    public static void send(PlayerRef playerRef, String value) {
        playerRef.sendMessage(Message.raw(value));
    }

    public static void send(CommandContext context, Message message) {
        context.sendMessage(message);
    }

    public static void send(PlayerRef playerRef, Message message) {
        playerRef.sendMessage(message);
    }
}
