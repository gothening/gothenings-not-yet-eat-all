package com.gothening.notyet.storage;

import appeng.api.config.Actionable;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.IPortableTerminal;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.MEStorage;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.common.MEStorageMenu;
import com.gothening.notyet.GotheningsNotYetEatAll;
import com.gothening.notyet.food.FoodSelector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class AE2FoodStorageProvider implements FoodStorageProvider {
    public static final String MOD_ID = "ae2";
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(GotheningsNotYetEatAll.MOD_ID, "ae2");

    private record AeSource(MEStorage storage, IActionSource source) {
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
            GotheningsNotYetEatAll.LOGGER.warn("AE2 provider unavailable", error);
            return false;
        }
    }

    @Override
    public List<FoodEntry> searchFood(ServerPlayer player, Predicate<ItemStack> predicate) {
        if (!isAvailable(player)) {
            GotheningsNotYetEatAll.LOGGER.debug("AE2 provider unavailable");
            return List.of();
        }

        List<FoodEntry> entries = new ArrayList<>();
        for (AeSource source : findAccessibleSources(player)) {
            try {
                KeyCounter keyCounter = new KeyCounter();
                source.storage().getAvailableStacks(keyCounter);
                for (var entry : keyCounter) {
                    AEKey key = entry.getKey();
                    long amount = entry.getLongValue();
                    if (!(key instanceof AEItemKey itemKey) || amount <= 0) {
                        continue;
                    }
                    ItemStack stack = itemKey.toStack((int) Math.min(amount, Integer.MAX_VALUE));
                    if (stack.isEmpty() || !predicate.test(stack)) {
                        continue;
                    }
                    GotheningsNotYetEatAll.LOGGER.debug(
                            "AE2 food candidate: {} x{}",
                            FoodSelector.itemId(stack),
                            amount);
                    entries.add(new FoodEntry(
                            stack,
                            (int) Math.min(amount, Integer.MAX_VALUE),
                            this,
                            new AeSource(source.storage(), source.source())));
                }
            } catch (RuntimeException | LinkageError error) {
                GotheningsNotYetEatAll.LOGGER.warn("Failed to search AE2 network", error);
            }
        }
        return entries;
    }

    @Override
    public ItemStack extract(ServerPlayer player, FoodEntry entry, int amount) {
        if (entry.provider() != this || !(entry.providerKey() instanceof AeSource source)) {
            return ItemStack.EMPTY;
        }
        try {
            AEItemKey key = AEItemKey.of(entry.item());
            long available = source.storage().extract(key, 1, Actionable.SIMULATE, source.source());
            if (available <= 0) {
                GotheningsNotYetEatAll.LOGGER.debug("AE2 extraction failed: unavailable");
                return ItemStack.EMPTY;
            }

            GotheningsNotYetEatAll.LOGGER.debug("AE2 extracting: {}", FoodSelector.itemId(entry.item()));
            long extracted = source.storage().extract(key, 1, Actionable.MODULATE, source.source());
            if (extracted <= 0) {
                GotheningsNotYetEatAll.LOGGER.debug("AE2 extraction failed: extract returned 0");
                return ItemStack.EMPTY;
            }
            return key.toStack((int) Math.min(extracted, Integer.MAX_VALUE));
        } catch (RuntimeException | LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("AE2 extraction failed", error);
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void returnItems(ServerPlayer player, FoodEntry entry, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (entry.provider() != this || !(entry.providerKey() instanceof AeSource source)) {
            giveToPlayerOrDrop(player, stack);
            return;
        }
        try {
            AEItemKey key = AEItemKey.of(stack);
            long inserted = source.storage().insert(key, stack.getCount(), Actionable.MODULATE, source.source());
            long remaining = stack.getCount() - inserted;
            if (remaining <= 0) {
                GotheningsNotYetEatAll.LOGGER.debug("AE2 food returned: {}", FoodSelector.itemId(stack));
            } else {
                GotheningsNotYetEatAll.LOGGER.debug("AE2 food returned partially: {}", FoodSelector.itemId(stack));
                giveToPlayerOrDrop(player, key.toStack((int) Math.min(remaining, Integer.MAX_VALUE)));
            }
        } catch (RuntimeException | LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("Failed to return item to AE2", error);
            giveToPlayerOrDrop(player, stack);
        }
    }

    private List<AeSource> findAccessibleSources(ServerPlayer player) {
        List<AeSource> sources = new ArrayList<>();
        Set<MEStorage> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        findSourceFromOpenMenu(player, sources, seen);
        findSourcesFromWirelessItems(player, sources, seen);
        return sources;
    }

    private void findSourceFromOpenMenu(ServerPlayer player, List<AeSource> sources, Set<MEStorage> seen) {
        if (!(player.containerMenu instanceof MEStorageMenu menu)) {
            return;
        }
        try {
            ILinkStatus linkStatus = menu.getLinkStatus();
            if (linkStatus == null || !linkStatus.connected()) {
                return;
            }
            ITerminalHost host = menu.getHost();
            IGridNode gridNode = menu.getGridNode();
            if (host == null || gridNode == null || gridNode.getGrid() == null) {
                return;
            }
            MEStorage storage = host.getInventory();
            if (storage == null || !seen.add(storage)) {
                return;
            }
            IActionSource source = host instanceof IActionHost actionHost
                    ? IActionSource.ofPlayer(player, actionHost)
                    : IActionSource.ofPlayer(player);
            GotheningsNotYetEatAll.LOGGER.debug("AE2 network found");
            sources.add(new AeSource(storage, source));
        } catch (RuntimeException | LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("Failed to read AE2 terminal menu", error);
        }
    }

    private void findSourcesFromWirelessItems(ServerPlayer player, List<AeSource> sources, Set<MEStorage> seen) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof IMenuItem menuItem)) {
                continue;
            }
            try {
                ItemMenuHost<?> host = menuItem.getMenuHost(player, MenuLocators.forInventorySlot(slot), null);
                if (!(host instanceof IPortableTerminal portable)
                        || !(host instanceof IActionHost actionHost)
                        || !host.isValid()) {
                    continue;
                }
                ILinkStatus linkStatus = portable.getLinkStatus();
                if (linkStatus == null || !linkStatus.connected()) {
                    continue;
                }
                IGridNode gridNode = actionHost.getActionableNode();
                if (gridNode == null || gridNode.getGrid() == null) {
                    continue;
                }
                MEStorage storage = portable.getInventory();
                if (storage == null || !seen.add(storage)) {
                    continue;
                }
                GotheningsNotYetEatAll.LOGGER.debug("AE2 network found");
                sources.add(new AeSource(storage, IActionSource.ofPlayer(player, actionHost)));
            } catch (RuntimeException | LinkageError error) {
                GotheningsNotYetEatAll.LOGGER.warn("Failed to inspect AE2 wireless terminal", error);
            }
        }
    }

    private static void giveToPlayerOrDrop(ServerPlayer player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }
}
