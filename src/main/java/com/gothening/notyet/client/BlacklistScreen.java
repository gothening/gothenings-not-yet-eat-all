package com.gothening.notyet.client;

import com.gothening.notyet.blacklist.BlacklistItemValidator;
import com.gothening.notyet.network.BlacklistUpdatePayload;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Chest-style personal food blacklist screen.
 *
 * <p>The grid and the player inventory are pure render surfaces: this screen is not a
 * container menu and creates no slots, so there is no code path that could move, copy or
 * consume items. Every interaction only sends add/remove requests to the server, which
 * answers with an authoritative sync payload.
 */
public final class BlacklistScreen extends Screen {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 222;
    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_MAIN_Y = 140;
    private static final int INVENTORY_HOTBAR_Y = 198;
    private static final int HOVER_COLOR = 0x80FFFFFF;
    private static final int DROP_TARGET_COLOR = 0x8000FF00;
    private static final int GLOBAL_DISABLED_COLOR = 0x55FF0000;
    private static final int TEXT_COLOR = 0x404040;

    private int leftPos;
    private int topPos;
    private int page;
    private int draggedInventorySlot = -1;
    private String draggedItemId;

    public BlacklistScreen() {
        super(Component.translatable("gui.gothenings_not_yet_eat_all.blacklist.title"));
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(this.leftPos + IMAGE_WIDTH - 44, this.topPos + 2, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.gothenings_not_yet_eat_all.blacklist.previous_page")))
                .build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(this.leftPos + IMAGE_WIDTH - 24, this.topPos + 2, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.gothenings_not_yet_eat_all.blacklist.next_page")))
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        int pageCount = pageCount();
        if (this.page >= pageCount) {
            this.page = pageCount - 1;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        guiGraphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 6, TEXT_COLOR, false);

        List<String> entries = ClientBlacklistCache.entries();
        renderEntries(guiGraphics, entries, mouseX, mouseY);
        renderPlayerInventory(guiGraphics, mouseX, mouseY);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("gui.gothenings_not_yet_eat_all.blacklist.page", this.page + 1, pageCount()),
                this.leftPos + IMAGE_WIDTH / 2,
                this.topPos + 129,
                TEXT_COLOR);

        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        renderDraggedItem(guiGraphics, mouseX, mouseY);
        renderTooltips(guiGraphics, entries, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        LocalPlayer player = localPlayer();
        if (player == null) {
            return false;
        }

        int entryIndex = entryIndexAt(mouseX, mouseY);
        if (entryIndex >= 0) {
            requestUpdate(ClientBlacklistCache.entries().get(entryIndex), false);
            playClickSound();
            return true;
        }

        int inventorySlot = inventorySlotAt(mouseX, mouseY);
        if (inventorySlot >= 0) {
            ItemStack stack = player.getInventory().getItem(inventorySlot);
            if (!stack.isEmpty() && isLocalFoodCandidate(player, stack)) {
                this.draggedInventorySlot = inventorySlot;
                this.draggedItemId = itemId(stack);
                return true;
            }
            return !stack.isEmpty();
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handledByWidget = super.mouseReleased(mouseX, mouseY, button);
        if (button != 0) {
            return handledByWidget;
        }

        int draggedSlot = this.draggedInventorySlot;
        String draggedId = this.draggedItemId;
        this.draggedInventorySlot = -1;
        this.draggedItemId = null;
        if (draggedSlot < 0 || draggedId == null) {
            return handledByWidget;
        }

        boolean overGrid = entrySlotIndexAt(mouseX, mouseY) >= 0;
        boolean overOriginSlot = inventorySlotAt(mouseX, mouseY) == draggedSlot;
        if (overGrid || overOriginSlot) {
            requestUpdate(draggedId, true);
            playClickSound();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return button == 0 && this.draggedInventorySlot >= 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Defense in depth: the eat key must never act while this screen is open.
        if (ClientKeybinds.EAT_ALL_KEY.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void changePage(int delta) {
        this.page = Math.max(0, Math.min(pageCount() - 1, this.page + delta));
    }

    private int pageCount() {
        int size = ClientBlacklistCache.entries().size();
        return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void renderEntries(GuiGraphics guiGraphics, List<String> entries, int mouseX, int mouseY) {
        int start = this.page * PAGE_SIZE;
        for (int index = 0; index < PAGE_SIZE; index++) {
            int slotX = this.leftPos + GRID_X + (index % COLUMNS) * SLOT_SIZE;
            int slotY = this.topPos + GRID_Y + (index / COLUMNS) * SLOT_SIZE;
            if (isInside(mouseX, mouseY, slotX, slotY)) {
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, HOVER_COLOR);
            }

            int entryIndex = start + index;
            if (entryIndex >= entries.size()) {
                continue;
            }
            String itemId = entries.get(entryIndex);
            ItemStack stack = stackFor(itemId);
            if (stack.isEmpty()) {
                guiGraphics.drawString(this.font, "?", slotX + 5, slotY + 4, TEXT_COLOR, false);
            } else {
                guiGraphics.renderItem(stack, slotX, slotY);
            }
            if (ClientBlacklistCache.isGloballyDisabled(itemId)) {
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, GLOBAL_DISABLED_COLOR);
            }
        }
    }

    private void renderPlayerInventory(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        LocalPlayer player = localPlayer();
        if (player == null) {
            return;
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int inventorySlot = 9 + row * COLUMNS + column;
                renderInventorySlot(
                        guiGraphics,
                        player,
                        inventorySlot,
                        this.leftPos + INVENTORY_X + column * SLOT_SIZE,
                        this.topPos + INVENTORY_MAIN_Y + row * SLOT_SIZE,
                        mouseX,
                        mouseY);
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            renderInventorySlot(
                    guiGraphics,
                    player,
                    column,
                    this.leftPos + INVENTORY_X + column * SLOT_SIZE,
                    this.topPos + INVENTORY_HOTBAR_Y,
                    mouseX,
                    mouseY);
        }
    }

    private void renderInventorySlot(
            GuiGraphics guiGraphics,
            LocalPlayer player,
            int inventorySlot,
            int slotX,
            int slotY,
            int mouseX,
            int mouseY) {
        if (isInside(mouseX, mouseY, slotX, slotY)) {
            guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, HOVER_COLOR);
        }
        ItemStack stack = player.getInventory().getItem(inventorySlot);
        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, slotX, slotY);
        }
    }

    private void renderDraggedItem(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        LocalPlayer player = localPlayer();
        if (player == null || this.draggedInventorySlot < 0) {
            return;
        }
        ItemStack stack = player.getInventory().getItem(this.draggedInventorySlot);
        if (stack.isEmpty()) {
            return;
        }
        int slot = entrySlotIndexAt(mouseX, mouseY);
        if (slot >= 0) {
            int slotX = this.leftPos + GRID_X + (slot % COLUMNS) * SLOT_SIZE;
            int slotY = this.topPos + GRID_Y + (slot / COLUMNS) * SLOT_SIZE;
            guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, DROP_TARGET_COLOR);
        }
        guiGraphics.renderItem(stack, mouseX - 8, mouseY - 8);
    }

    private void renderTooltips(GuiGraphics guiGraphics, List<String> entries, int mouseX, int mouseY) {
        int entryIndex = entryIndexAt(mouseX, mouseY);
        if (entryIndex >= 0 && entryIndex < entries.size()) {
            String itemId = entries.get(entryIndex);
            ItemStack stack = stackFor(itemId);
            List<Component> lines = new ArrayList<>();
            lines.add(stack.isEmpty() ? Component.literal(itemId) : stack.getHoverName());
            if (ClientBlacklistCache.isGloballyDisabled(itemId)) {
                lines.add(Component.translatable("gui.gothenings_not_yet_eat_all.blacklist.globally_disabled")
                        .withStyle(ChatFormatting.RED));
            }
            if (stack.isEmpty()) {
                lines.add(Component.translatable("gui.gothenings_not_yet_eat_all.blacklist.unknown_item")
                        .withStyle(ChatFormatting.GRAY));
            }
            guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }

        LocalPlayer player = localPlayer();
        int inventorySlot = inventorySlotAt(mouseX, mouseY);
        if (player != null && inventorySlot >= 0) {
            ItemStack stack = player.getInventory().getItem(inventorySlot);
            if (!stack.isEmpty()) {
                guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }
    }

    private int entrySlotIndexAt(double mouseX, double mouseY) {
        double relativeX = mouseX - (this.leftPos + GRID_X);
        double relativeY = mouseY - (this.topPos + GRID_Y);
        if (relativeX < 0 || relativeY < 0) {
            return -1;
        }
        int column = (int) (relativeX / SLOT_SIZE);
        int row = (int) (relativeY / SLOT_SIZE);
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) {
            return -1;
        }
        return row * COLUMNS + column;
    }

    private int entryIndexAt(double mouseX, double mouseY) {
        int slot = entrySlotIndexAt(mouseX, mouseY);
        if (slot < 0) {
            return -1;
        }
        int entryIndex = this.page * PAGE_SIZE + slot;
        return entryIndex < ClientBlacklistCache.entries().size() ? entryIndex : -1;
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        double relativeX = mouseX - (this.leftPos + INVENTORY_X);
        if (relativeX < 0) {
            return -1;
        }
        int column = (int) (relativeX / SLOT_SIZE);
        if (column < 0 || column >= COLUMNS) {
            return -1;
        }
        double relativeY = mouseY - this.topPos;
        double mainRelativeY = relativeY - INVENTORY_MAIN_Y;
        if (mainRelativeY >= 0) {
            int row = (int) (mainRelativeY / SLOT_SIZE);
            if (row < 3) {
                return 9 + row * COLUMNS + column;
            }
        }
        double hotbarRelativeY = relativeY - INVENTORY_HOTBAR_Y;
        if (hotbarRelativeY >= 0 && hotbarRelativeY < SLOT_SIZE) {
            return column;
        }
        return -1;
    }

    private static boolean isInside(int mouseX, int mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16;
    }

    @Nullable
    private LocalPlayer localPlayer() {
        Minecraft minecraft = this.minecraft;
        return minecraft == null ? null : minecraft.player;
    }

    private static boolean isLocalFoodCandidate(LocalPlayer player, ItemStack stack) {
        return BlacklistItemValidator.isEdibleCandidate(stack, player);
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static ItemStack stackFor(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static void requestUpdate(String itemId, boolean add) {
        PacketDistributor.sendToServer(new BlacklistUpdatePayload(itemId, add));
    }

    private void playClickSound() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
