package com.gothening.notyet.food;

import com.gothening.notyet.diversity.FoodDiversityProvider;
import com.gothening.notyet.storage.FoodEntry;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class FoodPreferenceSelector {
    private FoodPreferenceSelector() {
    }

    public static Optional<FoodEntry> select(
            ServerPlayer player,
            List<FoodEntry> entries,
            FoodDiversityProvider diversityProvider) {
        FoodEntry fallback = null;
        for (FoodEntry entry : entries) {
            if (!FoodSelector.canEat(player, entry.item())) {
                continue;
            }
            if (fallback == null) {
                fallback = entry;
            }
            boolean alreadyEaten = diversityProvider != null && diversityProvider.hasEaten(player, entry.item());
            if (!alreadyEaten) {
                return Optional.of(entry);
            }
        }
        return Optional.ofNullable(fallback);
    }
}
