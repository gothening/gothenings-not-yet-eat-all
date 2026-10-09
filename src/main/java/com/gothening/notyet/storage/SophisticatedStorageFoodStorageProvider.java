package com.gothening.notyet.storage;

import com.gothening.notyet.GotheningsNotYetEatAll;
import com.gothening.notyet.food.FoodSelector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageIOBlockEntity;

public final class SophisticatedStorageFoodStorageProvider implements FoodStorageProvider {
    public static final String MOD_ID = "sophisticatedstorage";
    public static final int DEFAULT_SEARCH_RADIUS = 8;
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(GotheningsNotYetEatAll.MOD_ID, "sophisticated_storage");

    private record StorageSource(BlockPos pos, Block block, int slot) {
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
            GotheningsNotYetEatAll.LOGGER.warn("Sophisticated Storage provider unavailable", error);
            return false;
        }
    }

    @Override
    public List<FoodEntry> searchFood(ServerPlayer player, Predicate<ItemStack> predicate) {
        if (!isAvailable(player)) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Storage provider unavailable");
            return List.of();
        }

        List<FoodEntry> entries = new ArrayList<>();
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        List<BlockPos> sources = findSources(level, center);
        boolean foundController = sources.stream().anyMatch(pos -> level.getBlockEntity(pos) instanceof ControllerBlockEntity);
        Set<BlockPos> controllerPositions = new HashSet<>();
        for (BlockPos pos : sources) {
            if (level.getBlockEntity(pos) instanceof ControllerBlockEntity) {
                controllerPositions.add(pos);
            }
        }
        if (!sources.isEmpty()) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Storage found");
        }

        for (BlockPos pos : sources) {
            try {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof StorageBlockEntity storage && foundController && storage.isLinked()) {
                    continue;
                }
                if (blockEntity instanceof StorageIOBlockEntity io
                        && io.getControllerPos().filter(controllerPositions::contains).isPresent()) {
                    continue;
                }
                Optional<IItemHandler> handler = resolveHandler(level, pos);
                if (handler.isEmpty()) {
                    continue;
                }
                Block block = level.getBlockState(pos).getBlock();
                IItemHandler inventory = handler.get();
                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    ItemStack stack = inventory.getStackInSlot(slot);
                    if (stack.isEmpty() || !predicate.test(stack)) {
                        continue;
                    }
                    GotheningsNotYetEatAll.LOGGER.debug(
                            "Sophisticated Storage food candidate: {} x{}",
                            FoodSelector.itemId(stack),
                            stack.getCount());
                    entries.add(new FoodEntry(
                            stack,
                            stack.getCount(),
                            this,
                            new StorageSource(pos.immutable(), block, slot)));
                }
            } catch (RuntimeException | LinkageError error) {
                GotheningsNotYetEatAll.LOGGER.warn("Failed to search Sophisticated Storage", error);
            }
        }
        return entries;
    }

    @Override
    public ItemStack extract(ServerPlayer player, FoodEntry entry, int amount) {
        if (entry.provider() != this || !(entry.providerKey() instanceof StorageSource source)) {
            return ItemStack.EMPTY;
        }
        Optional<IItemHandler> inventory = resolveSource(player.serverLevel(), source);
        if (inventory.isEmpty()) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Storage extraction failed: storage unavailable");
            return ItemStack.EMPTY;
        }
        IItemHandler handler = inventory.get();
        if (source.slot() < 0 || source.slot() >= handler.getSlots()) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Storage extraction failed: slot unavailable");
            return ItemStack.EMPTY;
        }
        ItemStack current = handler.getStackInSlot(source.slot());
        if (!ItemStack.isSameItemSameComponents(current, entry.item()) || current.getCount() <= 0) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Storage extraction failed: item changed");
            return ItemStack.EMPTY;
        }

        GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Storage extracting: {}", FoodSelector.itemId(entry.item()));
        ItemStack extracted = handler.extractItem(source.slot(), 1, false);
        if (extracted.isEmpty()) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Storage extraction failed: extract returned empty");
            return ItemStack.EMPTY;
        }
        return extracted;
    }

    @Override
    public void returnItems(ServerPlayer player, FoodEntry entry, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (entry.provider() != this || !(entry.providerKey() instanceof StorageSource source)) {
            giveToPlayerOrDrop(player, stack);
            return;
        }
        Optional<IItemHandler> inventory = resolveSource(player.serverLevel(), source);
        if (inventory.isEmpty()) {
            giveToPlayerOrDrop(player, stack);
            return;
        }
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory.get(), stack, false);
        if (remainder.isEmpty()) {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Storage food returned: {}", FoodSelector.itemId(stack));
        } else {
            GotheningsNotYetEatAll.LOGGER.debug("Sophisticated Storage food returned partially: {}", FoodSelector.itemId(stack));
            giveToPlayerOrDrop(player, remainder);
        }
    }

    private static List<BlockPos> findSources(ServerLevel level, BlockPos center) {
        List<BlockPos> sources = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.getX() - DEFAULT_SEARCH_RADIUS,
                center.getY() - DEFAULT_SEARCH_RADIUS,
                center.getZ() - DEFAULT_SEARCH_RADIUS,
                center.getX() + DEFAULT_SEARCH_RADIUS,
                center.getY() + DEFAULT_SEARCH_RADIUS,
                center.getZ() + DEFAULT_SEARCH_RADIUS)) {
            if (!level.isLoaded(pos) || level.getBlockState(pos).isAir()) {
                continue;
            }
            Block block = level.getBlockState(pos).getBlock();
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(MOD_ID)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ControllerBlockEntity) {
                sources.add(pos.immutable());
            } else if (blockEntity instanceof ChestBlockEntity chest) {
                if (chest.isMainChest()) {
                    sources.add(pos.immutable());
                }
            } else if (blockEntity instanceof StorageIOBlockEntity) {
                sources.add(pos.immutable());
            } else if (blockEntity instanceof StorageBlockEntity) {
                sources.add(pos.immutable());
            }
        }
        return sources;
    }

    private static Optional<IItemHandler> resolveSource(ServerLevel level, StorageSource source) {
        if (!level.isLoaded(source.pos()) || level.getBlockState(source.pos()).getBlock() != source.block()) {
            return Optional.empty();
        }
        return resolveHandler(level, source.pos());
    }

    private static Optional<IItemHandler> resolveHandler(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || blockEntity.isRemoved()) {
            return Optional.empty();
        }
        if (blockEntity instanceof ChestBlockEntity chest) {
            if (!chest.isMainChest()) {
                return Optional.empty();
            }
            return Optional.of(chest.getMainStorageWrapper().getInventoryForInputOutput());
        }
        if (blockEntity instanceof StorageBlockEntity storage) {
            return Optional.of(storage.getExternalItemHandler(null));
        }
        if (blockEntity instanceof StorageIOBlockEntity io) {
            return Optional.ofNullable(io.getExternalItemHandler(null));
        }
        if (blockEntity instanceof ControllerBlockEntity controller) {
            return Optional.of((IItemHandler) controller);
        }
        return Optional.empty();
    }

    private static void giveToPlayerOrDrop(ServerPlayer player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }
}
