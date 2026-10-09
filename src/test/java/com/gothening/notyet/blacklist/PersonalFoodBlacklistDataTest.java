package com.gothening.notyet.blacklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class PersonalFoodBlacklistDataTest {
    private static final String APPLE = "minecraft:apple";
    private static final String BREAD = "minecraft:bread";
    private static final String MISSING = "example:removed_food";

    private final UUID playerA = UUID.randomUUID();
    private final UUID playerB = UUID.randomUUID();

    @Test
    void addIsIdempotentAndDeduplicates() {
        PersonalFoodBlacklistData data = new PersonalFoodBlacklistData();
        assertTrue(data.add(playerA, APPLE));
        assertFalse(data.add(playerA, APPLE));
        assertEquals(List.of(APPLE), data.list(playerA));
    }

    @Test
    void removeDropsTheEntryAndReportsNoOpRemovals() {
        PersonalFoodBlacklistData data = new PersonalFoodBlacklistData();
        data.add(playerA, APPLE);
        assertTrue(data.contains(playerA, APPLE));
        assertTrue(data.remove(playerA, APPLE));
        assertFalse(data.contains(playerA, APPLE));
        assertFalse(data.remove(playerA, APPLE));
        assertTrue(data.list(playerA).isEmpty());
    }

    @Test
    void playersAreIsolated() {
        PersonalFoodBlacklistData data = new PersonalFoodBlacklistData();
        data.add(playerA, APPLE);
        assertTrue(data.contains(playerA, APPLE));
        assertFalse(data.contains(playerB, APPLE));

        data.add(playerB, BREAD);
        assertEquals(List.of(APPLE), data.list(playerA));
        assertEquals(List.of(BREAD), data.list(playerB));
    }

    @Test
    void listIsSortedForStablePages() {
        PersonalFoodBlacklistData data = new PersonalFoodBlacklistData();
        data.add(playerA, BREAD);
        data.add(playerA, APPLE);
        assertEquals(List.of(APPLE, BREAD), data.list(playerA));
    }

    @Test
    void saveLoadRoundTripKeepsEntriesIncludingUnknownIds() {
        PersonalFoodBlacklistData data = new PersonalFoodBlacklistData();
        data.add(playerA, APPLE);
        data.add(playerA, MISSING);
        data.add(playerB, BREAD);

        CompoundTag tag = data.save(new CompoundTag());
        PersonalFoodBlacklistData loaded = PersonalFoodBlacklistData.load(tag);

        assertEquals(List.of(MISSING, APPLE), loaded.list(playerA));
        assertEquals(List.of(BREAD), loaded.list(playerB));
        assertTrue(loaded.contains(playerA, MISSING));
    }

    @Test
    void loadToleratesMalformedEntries() {
        CompoundTag players = new CompoundTag();
        players.putString("not-a-uuid", "broken");
        ListTag wrongType = new ListTag();
        wrongType.add(IntTag.valueOf(1));
        players.put(playerA.toString(), wrongType);
        CompoundTag root = new CompoundTag();
        root.put("players", players);

        PersonalFoodBlacklistData data = PersonalFoodBlacklistData.load(root);

        assertTrue(data.list(playerA).isEmpty());
        assertFalse(data.contains(playerA, "1"));
    }

    @Test
    void savingEmptyDataStillProducesALoadableSnapshot() {
        PersonalFoodBlacklistData data = new PersonalFoodBlacklistData();
        CompoundTag tag = data.save(new CompoundTag());
        assertTrue(PersonalFoodBlacklistData.load(tag).list(playerB).isEmpty());
    }
}
