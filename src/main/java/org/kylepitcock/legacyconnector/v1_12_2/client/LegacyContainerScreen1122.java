package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyMenu1122;
import org.kylepitcock.legacyconnector.v1_12_2.gui.LegacySlot1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.ScreenGuard;

/**
 * Hosts a 1.12.2 {@link GuiContainer}. The legacy {@code drawScreen} runs first and its base calls back into the modern container
 * rendering ({@link #mcc$renderContainer}), which draws the legacy background (and buttons), the slots and items, the legacy foreground and
 * the carried stack. Clicking and dragging slots is modern; screens that handle keys themselves get every key through their keyTyped.
 */
public class LegacyContainerScreen1122 extends AbstractContainerScreen<LegacyMenu1122> implements GuiContainer.Host {
    private final GuiContainer legacy;
    private final boolean customKeys;
    private final KeyRouter1122 keys = new KeyRouter1122();
    private boolean failed;
    private GuiGraphics graphics;

    public LegacyContainerScreen1122(LegacyMenu1122 menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.legacy = menu.clientGui() instanceof GuiContainer gc ? gc : null;
        this.customKeys = legacy != null && (LegacyMenu1122.overrides(legacy, "keyTyped", char.class, int.class)
                || LegacyMenu1122.overrides(legacy, "handleKeyboardInput"));
        if (legacy != null) legacy.mcc$attach(this);
    }

    public GuiContainer legacy() {
        return legacy;
    }

    private void guard(Runnable code) {
        if (!failed) failed = !ScreenGuard.run(this, code);
    }

    @Override
    protected void init() {
        if (legacy != null) {
            imageWidth = legacy.mcc$xSize();
            imageHeight = legacy.mcc$ySize();
        }
        super.init();
        if (legacy != null) {
            failed = false;
            guard(() -> {
                legacy.setWorldAndResolution(Minecraft.getMinecraft(), width, height);
                // a mod's initGui may change the size: follow it
                imageWidth = legacy.mcc$xSize();
                imageHeight = legacy.mcc$ySize();
                leftPos = (width - imageWidth) / 2;
                topPos = (height - imageHeight) / 2;
                legacy.mcc$setOrigin(leftPos, topPos);
            });
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (legacy == null || failed) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        java.util.List<legacyconnector.v1_12_2.net.minecraft.inventory.Slot> dragged = new java.util.ArrayList<>();
        for (var slot : quickCraftSlots) if (slot instanceof LegacySlot1122 ls) dragged.add(ls.legacy());
        legacy.mcc$setDragSplitting(isQuickCrafting, dragged);
        this.graphics = graphics;
        RenderContext.beginGui(graphics);
        try {
            guard(() -> {
                if (ScreenEvents1122.drawPre(legacy, mouseX, mouseY, partialTick)) {
                    legacy.drawScreen(mouseX, mouseY, partialTick);
                    ScreenEvents1122.drawPost(legacy, mouseX, mouseY, partialTick);
                }
            });
        } finally {
            RenderContext.end();
            this.graphics = null;
        }
    }

    @Override
    public void mcc$renderContainer(int mouseX, int mouseY, float partialTicks) {
        if (graphics != null) super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void mcc$renderTooltip(int mouseX, int mouseY) {
        if (graphics != null) renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.inventory.Slot mcc$hoveredSlot() {
        return hoveredSlot instanceof LegacySlot1122 ls ? ls.legacy() : null;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // legacy drawScreen draws its own darkening (drawDefaultBackground); only the container background layer is drawn here
        if (legacy == null) super.renderBackground(graphics, mouseX, mouseY, partialTick);
        else renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (legacy == null || failed) return;
        RenderContext.beginGui(graphics);
        try {
            guard(() -> legacy.mcc$drawBackground(partialTick, mouseX, mouseY));
        } finally {
            RenderContext.end();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (legacy == null || failed) return;
        RenderContext.beginGui(graphics);
        try {
            guard(() -> legacy.mcc$drawForeground(mouseX, mouseY));
        } finally {
            RenderContext.end();
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (legacy != null && !failed) {
            keys.flush(legacy);
            boolean[] onButton = {false};
            guard(() -> {
                for (GuiButton b : legacy.mcc$buttons()) if (b.mousePressed(Minecraft.getMinecraft(), (int) x, (int) y)) onButton[0] = true;
                legacy.mcc$mouseClicked((int) x, (int) y, button);
            });
            if (onButton[0]) return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (legacy != null) guard(() -> legacy.mcc$mouseReleased((int) x, (int) y, button));
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (legacy != null) guard(() -> legacy.mcc$mouseDragged((int) x, (int) y));
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (legacy != null) guard(() -> legacy.mcc$mouseScrolled((int) x, (int) y, scrollY));
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    /** Screens with their own key handling see every key (their base keyTyped closes on Escape or the inventory key, as 1.12's did). */
    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (customKeys && !failed) {
            guard(() -> keys.keyPressed(legacy, key, modifiers));
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        if (customKeys && !failed) guard(() -> keys.keyReleased(legacy, key));
        return super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (customKeys && !failed) {
            guard(() -> keys.charTyped(legacy, c));
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (legacy != null) guard(() -> {
            keys.flush(legacy);
            legacy.updateScreen();
        });
    }

    @Override
    public void removed() {
        if (legacy != null && !failed) ScreenGuard.runQuiet(this, legacy::onGuiClosed);
        super.removed();
    }
}
