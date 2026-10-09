package com.gothening.notyet.food;

import com.gothening.notyet.blacklist.FoodBlacklistRules;
import com.gothening.notyet.blacklist.PersonalFoodBlacklistData;
import com.gothening.notyet.config.GYNEConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public final class FoodSelector {
    private FoodSelector() {
    }

    public static boolean isFood(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        FoodProperties foodProperties = stack.getFoodProperties(player);
        return foodProperties != null
                && !stack.useOnRelease()
                && stack.isItemEnabled(player.level().enabledFeatures())
                && !FoodBlacklistRules.isExcluded(
                        GYNEConfig.FOOD_BLACKLIST.get(),
                        PersonalFoodBlacklistData.get(player),
                        player.getUUID(),
                        itemId(stack));
    }

    public static boolean canEat(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        FoodProperties foodProperties = stack.getFoodProperties(player);
        return foodProperties != null && player.canEat(foodProperties.canAlwaysEat());
    }

    public static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
