package com.gothening.notyet.storage;

import com.gothening.notyet.GotheningsNotYetEatAll;
import com.gothening.notyet.food.FoodSelector;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

public final class SophisticatedBackpacksFoodStorageProvider implements FoodStorageProvider {
    public static final String MOD_ID = "sophisticatedbackpacks";
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(GotheningsNotYetEatAll.MOD_ID, "sophisticated_backpacks");

    private record BackpackSource(
            String handlerName,
            String identifier,
            int backpackSlot,
            ItemStack backpackSnapshot,
            int inventorySlot) {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public boolean isAvailable(ServerPlayer player) {
        try {
            return ModList.get().isLoaded(MOD_ID);
        } catch (LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("Sophisticated Backpacks provider unavailable", error);
            return false;
        }
    }

    @Override
    public List<FoodEntry> searchFood(ServerPlayer player, Predicate<ItemStack> predicate) {
        if (!isAvailable(player)) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Backpacks provider unavailable");
            return List.of();
        }

        List<FoodEntry> entries = new ArrayList<>();
        try {
            PlayerInventoryProvider.get().runOnBackpacks(player, (backpackStack, handlerName, identifier, slot) -> {
                try {
                    if (!(backpackStack.getItem() instanceof BackpackItem)) {
                        return false;
                    }
                    IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpackStack);
                    wrapper.onInit(player.level());
                    IItemHandler inventory = wrapper.getInventoryForInputOutput();
                    for (int inventorySlot = 0; inventorySlot < inventory.getSlots(); inventorySlot++) {
                        ItemStack stack = inventory.getStackInSlot(inventorySlot);
                        if (stack.isEmpty() || !predicate.test(stack)) {
                            continue;
                        }
                        GotheningsNotYetEatAll.LOGGER.debug(
                                "Sophisticated Backpack food candidate: {} x{}",
                                FoodSelector.itemId(stack),
                                stack.getCount());
                        entries.add(new FoodEntry(
                                stack,
                                stack.getCount(),
                                this,
                                new BackpackSource(handlerName, identifier, slot, backpackStack.copy(), inventorySlot)));
                    }
                } catch (RuntimeException | LinkageError error) {
                    GotheningsNotYetEatAll.LOGGER.warn("Failed to search Sophisticated Backpack", error);
                }
                return false;
            });
        } catch (RuntimeException | LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("Failed to scan player backpacks", error);
        }
        return entries;
    }

    @Override
    public ItemStack extract(ServerPlayer player, FoodEntry entry, int amount) {
        if (entry.provider() != this || !(entry.providerKey() instanceof BackpackSource source)) {
            return ItemStack.EMPTY;
        }
        Optional<IItemHandler> inventory = resolveInventory(player, source);
        if (inventory.isEmpty()) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Backpack extraction failed: backpack unavailable");
            return ItemStack.EMPTY;
        }
        IItemHandler handler = inventory.get();
        if (source.inventorySlot() < 0 || source.inventorySlot() >= handler.getSlots()) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Backpack extraction failed: slot unavailable");
            return ItemStack.EMPTY;
        }
        ItemStack current = handler.getStackInSlot(source.inventorySlot());
        if (!ItemStack.isSameItemSameComponents(current, entry.item()) || current.getCount() <= 0) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Backpack extraction failed: item changed");
            return ItemStack.EMPTY;
        }

        GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Backpack extracting: {}", FoodSelector.itemId(entry.item()));
        ItemStack extracted = handler.extractItem(source.inventorySlot(), 1, false);
        if (extracted.isEmpty()) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Backpack extraction failed: extract returned empty");
            return ItemStack.EMPTY;
        }
        return extracted;
    }

    @Override
    public void returnItems(ServerPlayer player, FoodEntry entry, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (entry.provider() != this || !(entry.providerKey() instanceof BackpackSource source)) {
            giveToPlayerOrDrop(player, stack);
            return;
        }
        Optional<IItemHandler> inventory = resolveInventory(player, source);
        if (inventory.isEmpty()) {
            giveToPlayerOrDrop(player, stack);
            return;
        }
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory.get(), stack, false);
        if (remainder.isEmpty()) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Backpack food returned: {}", FoodSelector.itemId(stack));
        } else {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Backpack food returned partially: {}", FoodSelector.itemId(stack));
            giveToPlayerOrDrop(player, remainder);
        }
    }

    private static Optional<IItemHandler> resolveInventory(ServerPlayer player, BackpackSource source) {
        try {
            Optional<PlayerInventoryHandler> handler = PlayerInventoryProvider.get().getPlayerInventoryHandler(source.handlerName());
            if (handler.isEmpty()) {
                return Optional.empty();
            }
            ItemStack backpackStack = handler.get().getStackInSlot(player, source.identifier(), source.backpackSlot());
            if (backpackStack.isEmpty() || !(backpackStack.getItem() instanceof BackpackItem)) {
                return Optional.empty();
            }
            if (backpackStack.getItem() != source.backpackSnapshot().getItem()) {
                return Optional.empty();
            }
            IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpackStack);
            wrapper.onInit(player.level());
            return Optional.of(wrapper.getInventoryForInputOutput());
        } catch (RuntimeException | LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("Failed to resolve Sophisticated Backpack", error);
            return Optional.empty();
        }
    }

    private static void giveToPlayerOrDrop(ServerPlayer player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }
}
