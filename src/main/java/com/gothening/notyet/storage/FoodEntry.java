package com.gothening.notyet.storage;

import net.minecraft.world.item.ItemStack;

public record FoodEntry(ItemStack item, int availableCount, FoodStorageProvider provider, Object providerKey) {
    public FoodEntry {
        item = item.copy();
    }
}
