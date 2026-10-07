package com.sloyni.main.commands;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class CommandCollectionUtil {
    private CommandCollectionUtil() {
    }

    public static void removeSubCommand(AbstractCommandCollection collection, AbstractCommand command) {
        if (collection == null || command == null) {
            return;
        }
        mutateCommandTree(collection, command, false);
    }

    public static void ensureSubCommand(AbstractCommandCollection collection, AbstractCommand command) {
        if (collection == null || command == null) {
            return;
        }
        if (containsDirectSubCommand(collection, command)) {
            return;
        }
        try {
            collection.addSubCommand(command);
        } catch (Exception ignored) {
        }
    }

    private static void mutateCommandTree(AbstractCommandCollection root, AbstractCommand command, boolean add) {
        if (add) {
            ensureSubCommand(root, command);
            return;
        }

        Set<Object> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Queue<AbstractCommandCollection> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            AbstractCommandCollection current = queue.poll();
            if (current == null || !visited.add(current)) {
                continue;
            }

            if (removeDirectSubCommand(current, command)) {
                return;
            }

            for (AbstractCommandCollection child : extractChildCollections(current)) {
                if (child != null && !visited.contains(child)) {
                    queue.add(child);
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean removeDirectSubCommand(AbstractCommandCollection current, AbstractCommand command) {
        Class<?> type = current.getClass();
        boolean removed = false;
        while (type != null && type != Object.class) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(current);
                    if (value == null) {
                        continue;
                    }

                    if (value instanceof Collection<?> entries) {
                        removed = entries.remove(command) || removed;
                        continue;
                    }

                    if (value instanceof Map<?, ?> entries) {
                        for (Object key : entries.keySet().toArray()) {
                            Object entryValue = ((Map) entries).get(key);
                            if (entryValue == command) {
                                ((Map) entries).remove(key);
                                removed = true;
                            } else if (key == command || command.toString().equals(String.valueOf(key))) {
                                ((Map) entries).remove(key);
                                removed = true;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return removed;
    }

    @SuppressWarnings("unchecked")
    private static boolean containsDirectSubCommand(AbstractCommandCollection current, AbstractCommand command) {
        Class<?> type = current.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(current);
                    if (value instanceof Collection<?> entries && entries.contains(command)) {
                        return true;
                    }
                    if (value instanceof Map<?, ?> entries) {
                        if (entries.containsValue(command) || entries.containsKey(command)) {
                            return true;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static Set<AbstractCommandCollection> extractChildCollections(AbstractCommandCollection current) {
        Set<AbstractCommandCollection> children = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Class<?> type = current.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(current);
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof Collection<?> entries) {
                        for (Object entry : entries) {
                            if (entry instanceof AbstractCommandCollection collection) {
                                children.add(collection);
                            }
                        }
                        continue;
                    }
                    if (value instanceof Map<?, ?> entries) {
                        for (Object entry : entries.values()) {
                            if (entry instanceof AbstractCommandCollection collection) {
                                children.add(collection);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return children;
    }
}
