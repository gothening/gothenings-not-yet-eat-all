package com.gothening.notyet.blacklist;

import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Pure decision logic for the unified food exclusion rule:
 * a candidate item is excluded when the server global blacklist contains it or the
 * current player's personal blacklist contains it.
 */
public final class FoodBlacklistRules {
    private FoodBlacklistRules() {
    }

    public static boolean isExcluded(
            Collection<? extends String> globalBlacklist,
            @Nullable PersonalFoodBlacklistData personalData,
            UUID playerId,
            String itemId) {
        return globalBlacklist.contains(itemId) || (personalData != null && personalData.contains(playerId, itemId));
    }
}
