package com.gothening.notyet.storage;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface FoodStorageProvider {
    ResourceLocation id();

    boolean isAvailable(ServerPlayer player);

    List<FoodEntry> searchFood(ServerPlayer player, Predicate<ItemStack> predicate);

    ItemStack extract(ServerPlayer player, FoodEntry entry, int amount);

    void returnItems(ServerPlayer player, FoodEntry entry, ItemStack stack);
}
