package com.gothening.notyet.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class StorageProviderManager {
    private final List<FoodStorageProvider> providers = new ArrayList<>();

    public void register(FoodStorageProvider provider) {
        providers.add(provider);
    }

    public List<FoodEntry> searchFood(ServerPlayer player, Predicate<ItemStack> predicate) {
        List<FoodEntry> entries = new ArrayList<>();
        for (FoodStorageProvider provider : providers) {
            if (provider.isAvailable(player)) {
                entries.addAll(provider.searchFood(player, predicate));
            }
        }
        return entries;
    }

    public ItemStack extract(ServerPlayer player, FoodEntry entry, int amount) {
        return entry.provider().extract(player, entry, amount);
    }

    public void returnItems(ServerPlayer player, FoodEntry entry, ItemStack stack) {
        entry.provider().returnItems(player, entry, stack);
    }

    public List<FoodStorageProvider> getProviders() {
        return List.copyOf(providers);
    }
}
