package com.gothening.notyet.diversity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface FoodDiversityProvider {
    boolean isAvailable();

    boolean hasEaten(ServerPlayer player, ItemStack food);
}
