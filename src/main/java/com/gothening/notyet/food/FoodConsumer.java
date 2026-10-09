package com.gothening.notyet.food;

import com.gothening.notyet.GotheningsNotYetEatAll;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.EventHooks;

public final class FoodConsumer {
    private FoodConsumer() {
    }

    public enum Status {
        EATEN,
        FAILED
    }

    public record ConsumeResult(Status status, ItemStack remaining) {
    }

    public static ConsumeResult consume(ServerPlayer player, ItemStack food) {
        if (food.isEmpty() || player.isRemoved() || !player.isAlive() || player.isUsingItem()) {
            return new ConsumeResult(Status.FAILED, food);
        }
        if (!FoodSelector.canEat(player, food)) {
            return new ConsumeResult(Status.FAILED, food);
        }

        var inventory = player.getInventory();
        int mainHandSlot = inventory.selected;
        ItemStack originalMainHand = inventory.getItem(mainHandSlot).copy();
        inventory.setItem(mainHandSlot, food);
        ItemStack outcome = food;
        Status status = Status.FAILED;

        try {
            // Start the same server-side item-use flow as a normal right-click, then finish it
            // immediately so the request cannot leave a half-eaten use state behind.
            player.gameMode.useItem(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
            if (!player.isUsingItem()) {
                return new ConsumeResult(status, outcome);
            }

            ItemStack useItem = player.getUseItem();
            ItemStack beforeUse = useItem.copy();
            ItemStack finished = useItem.finishUsingItem(player.level(), player);
            outcome = finished;
            ItemStack result = EventHooks.onItemUseFinish(player, beforeUse, 0, finished);
            if (result != useItem) {
                player.setItemInHand(InteractionHand.MAIN_HAND, result);
            }
            player.stopUsingItem();

            outcome = player.getMainHandItem();
            status = Status.EATEN;
        } catch (RuntimeException exception) {
            GotheningsNotYetEatAll.LOGGER.error(
                    "Failed to eat {} for {}",
                    FoodSelector.itemId(food),
                    player.getGameProfile().getName(),
                    exception);
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
        } finally {
            inventory.setItem(mainHandSlot, originalMainHand);
            player.inventoryMenu.sendAllDataToRemote();
        }

        return new ConsumeResult(status, outcome);
    }
}
