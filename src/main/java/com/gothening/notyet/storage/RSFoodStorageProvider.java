package com.gothening.notyet.storage;

import com.gothening.notyet.GotheningsNotYetEatAll;
import com.gothening.notyet.food.FoodSelector;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.node.grid.GridExtractMode;
import com.refinedmods.refinedstorage.api.network.node.grid.GridOperations;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.InsertableStorage;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.api.security.PlatformSecurityNetworkComponent;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.api.storage.root.FuzzyRootStorage;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemContext;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemHelper;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemTargetBlockEntity;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.grid.FuzzyGridOperations;
import com.refinedmods.refinedstorage.common.grid.SecuredGridOperations;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.resource.ResourceTypes;
import com.refinedmods.refinedstorage.common.support.slotreference.InventorySlotReferenceProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class RSFoodStorageProvider implements FoodStorageProvider {
    public static final String MOD_ID = "refinedstorage";
    public static final int DEFAULT_RS_SEARCH_RADIUS = 8;
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(GotheningsNotYetEatAll.MOD_ID, "refined_storage");

    private record RsSource(Network network, ItemResource resource) {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public boolean isAvailable(ServerPlayer player) {
        try {
            return ModList.get().isLoaded(MOD_ID) && RefinedStorageApi.INSTANCE != null;
        } catch (LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("RS provider unavailable", error);
            return false;
        }
    }

    @Override
    public List<FoodEntry> searchFood(ServerPlayer player, Predicate<ItemStack> predicate) {
        if (!isAvailable(player)) {
            GotheningsNotYetEatAll.LOGGER.debug("RS provider unavailable");
            return List.of();
        }

        List<FoodEntry> entries = new ArrayList<>();
        for (Network network : findAccessibleNetworks(player)) {
            try {
                StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
                if (storage == null) {
                    continue;
                }
                for (ResourceAmount resourceAmount : storage.getAll()) {
                    if (!(resourceAmount.resource() instanceof ItemResource itemResource)) {
                        continue;
                    }
                    ItemStack stack = itemResource.toItemStack(resourceAmount.amount());
                    if (stack.isEmpty() || !predicate.test(stack)) {
                        continue;
                    }
                    GotheningsNotYetEatAll.LOGGER.debug(
                            "RS food candidate: {} x{}",
                            FoodSelector.itemId(stack),
                            resourceAmount.amount());
                    entries.add(new FoodEntry(
                            stack,
                            (int) Math.min(resourceAmount.amount(), Integer.MAX_VALUE),
                            this,
                            new RsSource(network, itemResource)));
                }
            } catch (RuntimeException | LinkageError error) {
                GotheningsNotYetEatAll.LOGGER.warn("Failed to search RS network", error);
            }
        }
        return entries;
    }

    @Override
    public ItemStack extract(ServerPlayer player, FoodEntry entry, int amount) {
        if (entry.provider() != this || !(entry.providerKey() instanceof RsSource source)) {
            return ItemStack.EMPTY;
        }
        try {
            StorageNetworkComponent storage = source.network().getComponent(StorageNetworkComponent.class);
            if (storage == null || storage.get(source.resource()) <= 0) {
                GotheningsNotYetEatAll.LOGGER.debug("RS extraction failed: unavailable");
                return ItemStack.EMPTY;
            }

            GotheningsNotYetEatAll.LOGGER.debug("RS extracting: {}", FoodSelector.itemId(source.resource().toItemStack()));
            SingleItemInsertableStorage target = new SingleItemInsertableStorage();
            GridOperations operations = createGridOperations(player, source.network());
            boolean extracted = operations.extract(source.resource(), GridExtractMode.SINGLE_RESOURCE, target);
            if (!extracted || target.stack().isEmpty()) {
                GotheningsNotYetEatAll.LOGGER.debug("RS extraction failed: {}", extracted ? "no item inserted" : "extract returned false");
                return ItemStack.EMPTY;
            }
            return target.stack();
        } catch (RuntimeException | LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("RS extraction failed", error);
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void returnItems(ServerPlayer player, FoodEntry entry, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (entry.provider() != this || !(entry.providerKey() instanceof RsSource source)) {
            giveToPlayerOrDrop(player, stack);
            return;
        }
        try {
            StorageNetworkComponent storage = source.network().getComponent(StorageNetworkComponent.class);
            if (storage == null) {
                giveToPlayerOrDrop(player, stack);
                return;
            }
            ItemResource resource = ItemResource.ofItemStack(stack);
            long inserted = storage.insert(resource, stack.getCount(), Action.EXECUTE, new PlayerActor(player));
            long remaining = stack.getCount() - inserted;
            if (remaining <= 0) {
                GotheningsNotYetEatAll.LOGGER.debug("RS food returned: {}", FoodSelector.itemId(stack));
            } else {
                GotheningsNotYetEatAll.LOGGER.debug("RS food returned partially: {}", FoodSelector.itemId(stack));
                giveToPlayerOrDrop(player, resource.toItemStack(remaining));
            }
        } catch (RuntimeException | LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("Failed to return item to RS", error);
            giveToPlayerOrDrop(player, stack);
        }
    }

    private List<Network> findAccessibleNetworks(ServerPlayer player) {
        List<Network> networks = new ArrayList<>();
        Set<Network> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        findNetworksFromItems(player, networks, seen);
        findNetworksFromNearbyGrids(player, networks, seen);
        return networks;
    }

    private void findNetworksFromItems(ServerPlayer player, List<Network> networks, Set<Network> seen) {
        try {
            NetworkItemHelper helper = RefinedStorageApi.INSTANCE.getNetworkItemHelper();
            InventorySlotReferenceProvider slotReferenceProvider = new InventorySlotReferenceProvider();
            var inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty() || !helper.isBound(stack)) {
                    continue;
                }
                List<SlotReference> references = slotReferenceProvider.find(player, Set.of(stack.getItem()));
                if (references.isEmpty()) {
                    continue;
                }
                NetworkItemContext context = helper.createContext(stack, player, references.get(0));
                context.resolveNetwork(false).ifPresent(network -> {
                    if (seen.add(network)) {
                        GotheningsNotYetEatAll.LOGGER.debug("RS network found");
                        networks.add(network);
                    }
                });
            }
        } catch (RuntimeException | LinkageError error) {
            GotheningsNotYetEatAll.LOGGER.warn("Failed to find RS network from items", error);
        }
    }

    private void findNetworksFromNearbyGrids(ServerPlayer player, List<Network> networks, Set<Network> seen) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.getX() - DEFAULT_RS_SEARCH_RADIUS,
                center.getY() - DEFAULT_RS_SEARCH_RADIUS,
                center.getZ() - DEFAULT_RS_SEARCH_RADIUS,
                center.getX() + DEFAULT_RS_SEARCH_RADIUS,
                center.getY() + DEFAULT_RS_SEARCH_RADIUS,
                center.getZ() + DEFAULT_RS_SEARCH_RADIUS)) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof Grid grid) || !(blockEntity instanceof NetworkItemTargetBlockEntity target)) {
                continue;
            }
            try {
                if (!grid.isGridActive() || !grid.canMenuStayOpen(player)) {
                    continue;
                }
                Network network = target.getNetworkForItem();
                if (network != null && seen.add(network)) {
                    GotheningsNotYetEatAll.LOGGER.debug("RS network found");
                    networks.add(network);
                }
            } catch (RuntimeException | LinkageError error) {
                GotheningsNotYetEatAll.LOGGER.warn("Failed to inspect nearby RS grid", error);
            }
        }
    }

    private static GridOperations createGridOperations(ServerPlayer player, Network network) {
        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        PlatformSecurityNetworkComponent security = network.getComponent(PlatformSecurityNetworkComponent.class);
        GridOperations operations = ResourceTypes.ITEM.createGridOperations(storage, new PlayerActor(player));
        operations = new SecuredGridOperations(player, security, operations);
        if (storage instanceof FuzzyRootStorage fuzzy) {
            operations = new FuzzyGridOperations(player, fuzzy, operations);
        }
        return operations;
    }

    private static void giveToPlayerOrDrop(ServerPlayer player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }

    private static final class SingleItemInsertableStorage implements InsertableStorage {
        private ItemStack stack = ItemStack.EMPTY;

        @Override
        public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
            if (!stack.isEmpty() || !(resource instanceof ItemResource itemResource) || amount <= 0) {
                return 0;
            }
            long inserted = Math.min(amount, 1);
            if (action == Action.EXECUTE) {
                stack = itemResource.toItemStack(inserted);
            }
            return inserted;
        }

        ItemStack stack() {
            return stack;
        }
    }
}
