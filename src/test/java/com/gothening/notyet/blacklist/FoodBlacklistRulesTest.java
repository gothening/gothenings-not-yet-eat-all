package com.gothening.notyet.blacklist;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FoodBlacklistRulesTest {
    private static final String APPLE = "minecraft:apple";
    private static final String BREAD = "minecraft:bread";

    private final UUID player = UUID.randomUUID();

    @Test
    void globalBlacklistExcludesAnItemWithoutAPersonalEntry() {
        PersonalFoodBlacklistData data = new PersonalFoodBlacklistData();
        assertTrue(FoodBlacklistRules.isExcluded(List.of(APPLE), data, player, APPLE));
    }

    @Test
    void personalBlacklistExcludesOnlyItsOwnPlayer() {
        PersonalFoodBlacklistData data = new PersonalFoodBlacklistData();
        data.add(player, APPLE);
        assertTrue(FoodBlacklistRules.isExcluded(List.of(), data, player, APPLE));
        assertFalse(FoodBlacklistRules.isExcluded(List.of(), data, UUID.randomUUID(), APPLE));
    }

    @Test
    void unrelatedItemsStayCandidates() {
        PersonalFoodBlacklistData data = new PersonalFoodBlacklistData();
        data.add(player, APPLE);
        assertFalse(FoodBlacklistRules.isExcluded(List.of(), data, player, BREAD));
    }

    @Test
    void missingPersonalDataFallsBackToGlobalRules() {
        assertFalse(FoodBlacklistRules.isExcluded(List.of(), null, player, APPLE));
        assertTrue(FoodBlacklistRules.isExcluded(List.of(APPLE), null, player, APPLE));
    }
}
