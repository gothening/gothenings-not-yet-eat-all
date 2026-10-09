package com.gothening.notyet.storage;

import com.gothening.notyet.GotheningsNotYetEatAll;
import com.gothening.notyet.food.FoodSelector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class VanillaContainerProvider implements FoodStorageProvider {
    public static final int DEFAULT_CONTAINER_SEARCH_RADIUS = 8;
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(GotheningsNotYetEatAll.MOD_ID, "vanilla_container");

    private record ContainerSlot(BlockPos pos, int slot, Block block) {
    }

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
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        Set<BlockPos> processedChests = new HashSet<>();

        for (BlockPos candidate : BlockPos.betweenClosed(
                center.getX() - DEFAULT_CONTAINER_SEARCH_RADIUS,
                center.getY() - DEFAULT_CONTAINER_SEARCH_RADIUS,
                center.getZ() - DEFAULT_CONTAINER_SEARCH_RADIUS,
                center.getX() + DEFAULT_CONTAINER_SEARCH_RADIUS,
                center.getY() + DEFAULT_CONTAINER_SEARCH_RADIUS,
                center.getZ() + DEFAULT_CONTAINER_SEARCH_RADIUS)) {
            if (!level.isLoaded(candidate)) {
                continue;
            }
            BlockState state = level.getBlockState(candidate);
            if (state.isAir()) {
                continue;
            }
            Block block = state.getBlock();
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft")) {
                continue;
            }
            if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) {
                ChestType chestType = state.getValue(ChestBlock.TYPE);
                BlockPos canonical = chestType == ChestType.LEFT
                        ? candidate.relative(ChestBlock.getConnectedDirection(state)).immutable()
                        : candidate.immutable();
                if (!processedChests.add(canonical)) {
                    continue;
                }
            }

            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, candidate, null);
            if (handler == null) {
                continue;
            }
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty() || !predicate.test(stack)) {
                    continue;
                }
                GotheningsNotYetEatAll.LOGGER.debug(
                        "Found food: {} from: {}",
                        FoodSelector.itemId(stack),
                        candidate.toShortString());
                entries.add(new FoodEntry(
                        stack,
                        stack.getCount(),
                        this,
                        new ContainerSlot(candidate.immutable(), slot, block)));
            }
        }
        return entries;
    }

    @Override
    public ItemStack extract(ServerPlayer player, FoodEntry entry, int amount) {
        if (entry.provider() != this || !(entry.providerKey() instanceof ContainerSlot key)) {
            return ItemStack.EMPTY;
        }
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(key.pos())) {
            return ItemStack.EMPTY;
        }
        if (level.getBlockState(key.pos()).getBlock() != key.block()) {
            return ItemStack.EMPTY;
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, key.pos(), null);
        if (handler == null || key.slot() < 0 || key.slot() >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = handler.getStackInSlot(key.slot());
        if (!ItemStack.isSameItemSameComponents(current, entry.item())) {
            return ItemStack.EMPTY;
        }
        int count = Math.min(amount, current.getCount());
        return count > 0 ? handler.extractItem(key.slot(), count, false) : ItemStack.EMPTY;
    }

    @Override
    public void returnItems(ServerPlayer player, FoodEntry entry, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (entry.provider() != this || !(entry.providerKey() instanceof ContainerSlot key)) {
            giveToPlayerOrDrop(player, stack);
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(key.pos())) {
            giveToPlayerOrDrop(player, stack);
            return;
        }
        if (level.getBlockState(key.pos()).getBlock() != key.block()) {
            giveToPlayerOrDrop(player, stack);
            return;
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, key.pos(), null);
        if (handler == null || key.slot() < 0 || key.slot() >= handler.getSlots()) {
            giveToPlayerOrDrop(player, stack);
            return;
        }

        ItemStack remainder = handler.insertItem(key.slot(), stack, false);
        if (!remainder.isEmpty()) {
            remainder = ItemHandlerHelper.insertItemStacked(handler, remainder, false);
        }
        if (!remainder.isEmpty()) {
            giveToPlayerOrDrop(player, remainder);
        }
    }

    private static void giveToPlayerOrDrop(ServerPlayer player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }
}
