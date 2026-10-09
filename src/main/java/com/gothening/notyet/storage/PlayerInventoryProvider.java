package com.gothening.notyet.storage;

import com.gothening.notyet.GotheningsNotYetEatAll;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class PlayerInventoryProvider implements FoodStorageProvider {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(GotheningsNotYetEatAll.MOD_ID, "player_inventory");
    private static final int MAIN_INVENTORY_SIZE = 36;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public boolean isAvailable(ServerPlayer player) {
        return !player.isRemoved() && player.isAlive();
    }

    @Override
    public List<FoodEntry> searchFood(ServerPlayer player, Predicate<ItemStack> predicate) {
        List<FoodEntry> entries = new ArrayList<>();
        var inventory = player.getInventory();
        for (int slot = 0; slot < MAIN_INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                entries.add(new FoodEntry(stack, stack.getCount(), this, slot));
            }
        }
        return entries;
    }

    @Override
    public ItemStack extract(ServerPlayer player, FoodEntry entry, int amount) {
        if (entry.provider() != this || !(entry.providerKey() instanceof Integer slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack current = player.getInventory().getItem(slot);
        if (!ItemStack.isSameItemSameComponents(current, entry.item())) {
            return ItemStack.EMPTY;
        }
        int count = Math.min(amount, current.getCount());
        return count > 0 ? player.getInventory().removeItem(slot, count) : ItemStack.EMPTY;
    }

    @Override
    public void returnItems(ServerPlayer player, FoodEntry entry, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
