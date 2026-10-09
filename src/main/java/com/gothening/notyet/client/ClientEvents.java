package com.gothening.notyet.client;

import com.gothening.notyet.config.GYNEConfig;
import com.gothening.notyet.network.BlacklistRequestPayload;
import com.gothening.notyet.network.EatRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    private static boolean shiftClickPending;

    private ClientEvents() {
    }

    /**
     * Raw key input is used to remember whether the eat key was pressed with shift held.
     * Vanilla only queues a key binding click when no screen is open, so the inventory case
     * is handled here directly; the in-game case is routed on the next client tick where the
     * queued click is consumed. This keeps Shift+V strictly ahead of the eat request.
     */
    public static void onKeyInput(InputEvent.Key event) {
        if (!GYNEConfig.KEY_ENABLED.get()) {
            return;
        }
        int action = event.getAction();
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) {
            return;
        }
        if ((event.getModifiers() & GLFW.GLFW_MOD_SHIFT) == 0) {
            return;
        }
        if (!ClientKeybinds.EAT_ALL_KEY.matches(event.getKey(), event.getScanCode())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BlacklistScreen) {
            return;
        }
        if (minecraft.screen == null) {
            shiftClickPending = true;
            return;
        }
        if (minecraft.screen instanceof InventoryScreen || minecraft.screen instanceof CreativeModeInventoryScreen) {
            openBlacklist(minecraft);
        }
    }

    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!GYNEConfig.KEY_ENABLED.get()) {
            shiftClickPending = false;
            return;
        }

        boolean clicked = false;
        while (ClientKeybinds.EAT_ALL_KEY.consumeClick()) {
            clicked = true;
        }
        if (!clicked) {
            shiftClickPending = false;
            return;
        }

        boolean shiftPressed = shiftClickPending;
        shiftClickPending = false;
        switch (EatKeyRouter.decide(contextOf(minecraft.screen), shiftPressed)) {
            case EAT -> {
                if (minecraft.player != null) {
                    PacketDistributor.sendToServer(new EatRequestPayload());
                }
            }
            case OPEN_BLACKLIST -> openBlacklist(minecraft);
            case IGNORE -> {
            }
        }
    }

    private static EatKeyRouter.Context contextOf(Screen screen) {
        if (screen == null) {
            return EatKeyRouter.Context.IN_GAME;
        }
        if (screen instanceof BlacklistScreen) {
            return EatKeyRouter.Context.BLACKLIST_SCREEN;
        }
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
            return EatKeyRouter.Context.PLAYER_INVENTORY;
        }
        return EatKeyRouter.Context.OTHER_SCREEN;
    }

    private static void openBlacklist(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        if (minecraft.screen instanceof InventoryScreen || minecraft.screen instanceof CreativeModeInventoryScreen) {
            player.closeContainer();
        }
        PacketDistributor.sendToServer(new BlacklistRequestPayload());
        minecraft.setScreen(new BlacklistScreen());
    }
}
