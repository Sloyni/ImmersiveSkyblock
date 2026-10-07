package com.sloyni.main.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

public class ISkyblockAdminDashboardHomesGroupsPage
        extends InteractiveCustomUIPage<ISkyblockAdminDashboardHomesGroupsPage.PageData> {

    private static final int MAX_GROUP_ROWS = 8;

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final ISkyblockAdminDashboardPage parent;

    private String createGroupIdInput = "";
    private String createGroupHomesInput = "";
    private String errorMessage = null;
    private List<GroupSlot> groupSlots = new ArrayList<>();
    private final Map<String, String> groupHomesDraftInputs = new HashMap<>();
    private final Map<String, Boolean> groupHomesDirtyShown = new HashMap<>();

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
            .builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Back", Codec.STRING), (o, v) -> o.back = v, o -> o.back).add()
            .append(new KeyedCodec<>("AddGroup", Codec.STRING), (o, v) -> o.addGroup = v, o -> o.addGroup).add()
            .append(new KeyedCodec<>("UpdateGroup", Codec.STRING), (o, v) -> o.updateGroup = v, o -> o.updateGroup).add()
            .append(new KeyedCodec<>("DeleteGroup", Codec.STRING), (o, v) -> o.deleteGroup = v, o -> o.deleteGroup).add()
            .append(new KeyedCodec<>("@CreateGroupId", Codec.STRING), (o, v) -> o.createGroupId = v, o -> o.createGroupId).add()
            .append(new KeyedCodec<>("@CreateGroupHomes", Codec.STRING), (o, v) -> o.createGroupHomes = v, o -> o.createGroupHomes).add()
            .append(new KeyedCodec<>("@GroupHomes0", Codec.STRING), (o, v) -> o.groupHomes0 = v, o -> o.groupHomes0).add()
            .append(new KeyedCodec<>("@GroupHomes1", Codec.STRING), (o, v) -> o.groupHomes1 = v, o -> o.groupHomes1).add()
            .append(new KeyedCodec<>("@GroupHomes2", Codec.STRING), (o, v) -> o.groupHomes2 = v, o -> o.groupHomes2).add()
            .append(new KeyedCodec<>("@GroupHomes3", Codec.STRING), (o, v) -> o.groupHomes3 = v, o -> o.groupHomes3).add()
            .append(new KeyedCodec<>("@GroupHomes4", Codec.STRING), (o, v) -> o.groupHomes4 = v, o -> o.groupHomes4).add()
            .append(new KeyedCodec<>("@GroupHomes5", Codec.STRING), (o, v) -> o.groupHomes5 = v, o -> o.groupHomes5).add()
            .append(new KeyedCodec<>("@GroupHomes6", Codec.STRING), (o, v) -> o.groupHomes6 = v, o -> o.groupHomes6).add()
            .append(new KeyedCodec<>("@GroupHomes7", Codec.STRING), (o, v) -> o.groupHomes7 = v, o -> o.groupHomes7).add()
            .build();

        public String back;
        public String addGroup;
        public String updateGroup;
        public String deleteGroup;
        public String createGroupId;
        public String createGroupHomes;
        public String groupHomes0;
        public String groupHomes1;
        public String groupHomes2;
        public String groupHomes3;
        public String groupHomes4;
        public String groupHomes5;
        public String groupHomes6;
        public String groupHomes7;
    }

    private static final class GroupSlot {
        final String id;
        final int maxHomes;

        GroupSlot(String id, int maxHomes) {
            this.id = id;
            this.maxHomes = maxHomes;
        }
    }

    public ISkyblockAdminDashboardHomesGroupsPage(@Nonnull PlayerRef playerRef,
                                                  @Nonnull World sourceWorld,
                                                  @Nonnull ISkyblockPlugin plugin,
                                                  @Nonnull ISkyblockAdminDashboardPage parent) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.parent = parent;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {

        commandBuilder.append("Pages/ISkyblockAdminDashboard.HomesGroupsManager.ui");

        String configuredLanguage = plugin.getConfigManager().getConfig().getLanguage();
        String language = configuredLanguage == null || configuredLanguage.isBlank()
            ? "en"
            : configuredLanguage.trim().toLowerCase();
        commandBuilder.set("#HomesGroupsTitle.Text", plugin.getMessageManager().get("ui.admin_dashboard.homes.groups_title", language));
        commandBuilder.set("#HomesGroupsDescription.Text", plugin.getMessageManager().get("ui.admin_dashboard.homes.groups_description", language));
        commandBuilder.set("#HomesGroupsBackButton.Text", plugin.getMessageManager().get("ui.common.back", language));
        commandBuilder.set("#HomesGroupsError.Visible", errorMessage != null && !errorMessage.isBlank());
        commandBuilder.set("#HomesGroupsError.Text", errorMessage == null ? "" : errorMessage);
        commandBuilder.set("#HomesGroupCreateIdField.Value", createGroupIdInput == null ? "" : createGroupIdInput);
        commandBuilder.set("#HomesGroupCreateHomesField.Value", createGroupHomesInput == null ? "" : createGroupHomesInput);
        commandBuilder.set("#HomesGroupCreateButton.Text", plugin.getMessageManager().get("ui.common.create", language));
        commandBuilder.set("#HomesGroupCreateIdField.PlaceholderText", plugin.getMessageManager().get("ui.admin_dashboard.homes.groups_create_id_placeholder", language));
        commandBuilder.set("#HomesGroupCreateHomesField.PlaceholderText", plugin.getMessageManager().get("ui.admin_dashboard.homes.groups_create_homes_placeholder", language));

        Map<String, Integer> groups = plugin.getAddonManager().getHomesAddon().getHomeGroups();
        groupSlots = groups.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(entry -> new GroupSlot(entry.getKey(), Math.max(0, entry.getValue() == null ? 0 : entry.getValue())))
            .toList();

        for (int i = 0; i < MAX_GROUP_ROWS; i++) {
            if (i < groupSlots.size()) {
                GroupSlot slot = groupSlots.get(i);
                String draft = groupHomesDraftInputs.get(slot.id);
                String displayValue = getSettingsInputDisplayValue(draft, slot.maxHomes);
                boolean dirty = isSettingsInputDirty(draft, slot.maxHomes);
                groupHomesDirtyShown.put(slot.id, dirty);
                commandBuilder.set("#HomesGroupRow" + i + ".Visible", true);
                commandBuilder.set("#HomesGroupName" + i + ".Text", slot.id);
                commandBuilder.set("#HomesGroupPerm" + i + ".Text", "iskyblock.homes." + slot.id);
                commandBuilder.set("#HomesGroupHomesField" + i + ".Value", displayValue);
                commandBuilder.set("#HomesGroupHomesFieldBorder" + i + ".Background", dirty ? "#f2c94c" : "#223149");
                commandBuilder.set("#HomesGroupApplyBox" + i + ".Visible", dirty);
            } else {
                commandBuilder.set("#HomesGroupRow" + i + ".Visible", false);
            }
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomesGroupsBackButton",
            EventData.of("Back", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#HomesGroupCreateIdField",
            EventData.of("@CreateGroupId", "#HomesGroupCreateIdField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#HomesGroupCreateHomesField",
            EventData.of("@CreateGroupHomes", "#HomesGroupCreateHomesField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomesGroupCreateButton",
            EventData.of("AddGroup", "true"), false);

        for (int i = 0; i < MAX_GROUP_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#HomesGroupHomesField" + i,
                EventData.of("@GroupHomes" + i, "#HomesGroupHomesField" + i + ".Value"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomesGroupApply" + i,
                EventData.of("UpdateGroup", String.valueOf(i)), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomesGroupDelete" + i,
                EventData.of("DeleteGroup", String.valueOf(i)), false);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (data.back != null) {
            player.getPageManager().openCustomPage(ref, store, parent);
            return;
        }

        if (data.createGroupId != null) {
            createGroupIdInput = data.createGroupId;
        }
        if (data.createGroupHomes != null) {
            createGroupHomesInput = data.createGroupHomes;
        }

        for (int i = 0; i < MAX_GROUP_ROWS; i++) {
            String rawDraft = getGroupHomesDraftValue(data, i);
            if (rawDraft != null) {
                if (i >= 0 && i < groupSlots.size()) {
                    GroupSlot slot = groupSlots.get(i);
                    String trimmedDraft = rawDraft.trim();
                    groupHomesDraftInputs.put(slot.id, trimmedDraft);
                    boolean nowDirty = isSettingsInputDirty(trimmedDraft, slot.maxHomes);
                    boolean wasDirty = groupHomesDirtyShown.getOrDefault(slot.id, false);
                    groupHomesDirtyShown.put(slot.id, nowDirty);
                    if (nowDirty != wasDirty) {
                        player.getPageManager().openCustomPage(ref, store, this);
                    }
                }
                return;
            }
        }

        if (data.addGroup != null) {
            String groupId = normalizeGroupId(createGroupIdInput);
            if (groupId.isBlank() || "default".equals(groupId)) {
                errorMessage = "Invalid group id";
                player.getPageManager().openCustomPage(ref, store, this);
                return;
            }

            int homes;
            try {
                homes = Integer.parseInt(createGroupHomesInput == null || createGroupHomesInput.isBlank() ? "0" : createGroupHomesInput.trim());
            } catch (Exception ignored) {
                errorMessage = "Invalid homes value";
                player.getPageManager().openCustomPage(ref, store, this);
                return;
            }

            plugin.getAddonManager().getHomesAddon().setHomeGroup(groupId, Math.max(0, homes));
            createGroupIdInput = "";
            createGroupHomesInput = "";
            errorMessage = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.updateGroup != null) {
            int index = parseIndex(data.updateGroup);
            if (index >= 0 && index < groupSlots.size()) {
                GroupSlot slot = groupSlots.get(index);
                String rawHomes = groupHomesDraftInputs.get(slot.id);
                if (rawHomes == null) {
                    rawHomes = getGroupHomesDraftValue(data, index);
                }
                int homes;
                try {
                    homes = Integer.parseInt(rawHomes == null || rawHomes.isBlank() ? String.valueOf(slot.maxHomes) : rawHomes.trim());
                } catch (Exception ignored) {
                    errorMessage = "Invalid homes value";
                    player.getPageManager().openCustomPage(ref, store, this);
                    return;
                }
                plugin.getAddonManager().getHomesAddon().setHomeGroup(slot.id, Math.max(0, homes));
                groupHomesDraftInputs.remove(slot.id);
                groupHomesDirtyShown.put(slot.id, false);
                errorMessage = null;
            }
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.deleteGroup != null) {
            int index = parseIndex(data.deleteGroup);
            if (index >= 0 && index < groupSlots.size()) {
                GroupSlot slot = groupSlots.get(index);
                plugin.getAddonManager().getHomesAddon().removeHomeGroup(slot.id);
                groupHomesDraftInputs.remove(slot.id);
                groupHomesDirtyShown.remove(slot.id);
                errorMessage = null;
            }
            player.getPageManager().openCustomPage(ref, store, this);
        }
    }

    private static int parseIndex(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static String normalizeGroupId(String rawGroupId) {
        if (rawGroupId == null) {
            return "";
        }
        String normalized = rawGroupId.trim().toLowerCase();
        return normalized.matches("^[a-z0-9]+$") ? normalized : "";
    }

    private static String getSettingsInputDisplayValue(String draftValue, int persistedValue) {
        if (draftValue == null || draftValue.isBlank()) {
            return String.valueOf(persistedValue);
        }
        return draftValue.trim();
    }

    private static boolean isSettingsInputDirty(String draftValue, int persistedValue) {
        if (draftValue == null || draftValue.isBlank()) {
            return false;
        }
        return !draftValue.trim().equals(String.valueOf(persistedValue));
    }

    private static String getGroupHomesDraftValue(PageData data, int index) {
        return switch (index) {
            case 0 -> data.groupHomes0;
            case 1 -> data.groupHomes1;
            case 2 -> data.groupHomes2;
            case 3 -> data.groupHomes3;
            case 4 -> data.groupHomes4;
            case 5 -> data.groupHomes5;
            case 6 -> data.groupHomes6;
            case 7 -> data.groupHomes7;
            default -> null;
        };
    }
}
