package com.gothening.notyet.blacklist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server-authoritative per-player food blacklists.
 *
 * <p>Entries are item IDs (not stacks), keyed by player UUID, and persisted in the overworld
 * data storage so they survive dimension changes and server restarts. Unknown item IDs from
 * uninstalled mods are kept as plain strings so they can be removed from the GUI and recover
 * if the mod comes back.
 */
public final class PersonalFoodBlacklistData extends SavedData {
    public static final String DATA_NAME = "gothenings_not_yet_eat_all_personal_blacklist";

    private static final String PLAYERS_KEY = "players";
    private static final SavedData.Factory<PersonalFoodBlacklistData> FACTORY =
            new SavedData.Factory<>(PersonalFoodBlacklistData::new, PersonalFoodBlacklistData::load);

    private final Map<UUID, SortedSet<String>> entries = new HashMap<>();

    @Nullable
    public static PersonalFoodBlacklistData get(@Nullable MinecraftServer server) {
        return server == null ? null : server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    @Nullable
    public static PersonalFoodBlacklistData get(ServerPlayer player) {
        return get(player.getServer());
    }

    public boolean add(UUID playerId, String itemId) {
        return entries.computeIfAbsent(playerId, id -> new TreeSet<>()).add(itemId);
    }

    public boolean remove(UUID playerId, String itemId) {
        SortedSet<String> playerEntries = entries.get(playerId);
        if (playerEntries == null) {
            return false;
        }
        boolean removed = playerEntries.remove(itemId);
        if (playerEntries.isEmpty()) {
            entries.remove(playerId);
        }
        return removed;
    }

    public boolean contains(UUID playerId, String itemId) {
        SortedSet<String> playerEntries = entries.get(playerId);
        return playerEntries != null && playerEntries.contains(itemId);
    }

    public List<String> list(UUID playerId) {
        SortedSet<String> playerEntries = entries.get(playerId);
        return playerEntries == null ? List.of() : List.copyOf(playerEntries);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return save(tag);
    }

    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        entries.forEach((playerId, itemIds) -> {
            if (itemIds.isEmpty()) {
                return;
            }
            ListTag list = new ListTag();
            itemIds.forEach(itemId -> list.add(StringTag.valueOf(itemId)));
            players.put(playerId.toString(), list);
        });
        tag.put(PLAYERS_KEY, players);
        return tag;
    }

    private static PersonalFoodBlacklistData load(CompoundTag tag, HolderLookup.Provider registries) {
        return load(tag);
    }

    public static PersonalFoodBlacklistData load(CompoundTag tag) {
        PersonalFoodBlacklistData data = new PersonalFoodBlacklistData();
        CompoundTag players = tag.getCompound(PLAYERS_KEY);
        for (String key : players.getAllKeys()) {
            UUID playerId = parseUuid(key);
            if (playerId == null) {
                continue;
            }
            SortedSet<String> itemIds = readItemIds(players.getList(key, Tag.TAG_STRING));
            if (!itemIds.isEmpty()) {
                data.entries.put(playerId, itemIds);
            }
        }
        return data;
    }

    @Nullable
    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static SortedSet<String> readItemIds(ListTag list) {
        SortedSet<String> itemIds = new TreeSet<>();
        for (int index = 0; index < list.size(); index++) {
            String itemId = list.getString(index);
            if (!itemId.isEmpty()) {
                itemIds.add(itemId);
            }
        }
        return itemIds;
    }
}
