package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.kylepitcock.legacyconnector.v1_7_10.gui.LegacyMenu;

/**
 * Hosts a legacy {@link GuiContainer}. Modern code draws slots and items and handles clicking/dragging; the
 * legacy screen draws its background (usually one textured rect), foreground text and buttons.
 */
public class LegacyContainerScreen extends AbstractContainerScreen<LegacyMenu> {
    private final GuiContainer legacy;
    private final boolean customKeys;

    public LegacyContainerScreen(LegacyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.legacy = (GuiContainer) menu.clientGui();
        this.customKeys = legacy != null && ClientGui.overrides(legacy, "keyTyped", char.class, int.class);
    }

    private boolean failed;

    public GuiContainer legacy() {
        return legacy;
    }

    @Override
    protected void init() {
        if (legacy != null) {
            imageWidth = legacy.mcc$xSize();
            imageHeight = legacy.mcc$ySize();
        }
        super.init();
        if (legacy != null) {
            failed = !ScreenGuard.run(this, () -> {
                legacy.setWorldAndResolution(Minecraft.getMinecraft(), width, height);
                legacy.mcc$setOrigin(leftPos, topPos);
            });
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (legacy == null || failed) return;
        legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen.mcc$beginFrame();
        RenderContext.beginGui(graphics);
        try {
            // Runs the mod's own drawScreen override (CoFH draws its tooltips and gauge hovers there); the base draws the buttons.
            failed = !ScreenGuard.run(this, () -> legacy.drawScreen(mouseX, mouseY, partialTick));
        } finally {
            RenderContext.end();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (legacy == null || failed) return;
        RenderContext.beginGui(graphics);
        try {
            failed = !ScreenGuard.run(this, () -> legacy.mcc$drawBackground(partialTick, mouseX, mouseY));
        } finally {
            RenderContext.end();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (legacy == null || failed) return;
        RenderContext.beginGui(graphics);
        try {
            failed = !ScreenGuard.run(this, () -> legacy.mcc$drawForeground(mouseX, mouseY));
        } finally {
            RenderContext.end();
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (legacy != null && !failed) {
            boolean[] onButton = {false};
            failed = !ScreenGuard.run(this, () -> {
                for (Object o : legacy.mcc$buttons()) {
                    if (((GuiButton) o).mousePressed(Minecraft.getMinecraft(), (int) x, (int) y)) onButton[0] = true;
                }
                legacy.mcc$mouseClicked((int) x, (int) y, button);
            });
            if (onButton[0]) return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (legacy != null && !failed) failed = !ScreenGuard.run(this, () -> legacy.mcc$mouseReleased((int) x, (int) y, button));
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (legacy != null && !failed) failed = !ScreenGuard.run(this, () -> legacy.mcc$mouseScrolled((int) x, (int) y, scrollY));
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (customKeys && !failed) {
            int legacyKey = LegacyKeys.toLegacy(key);
            if (legacyKey != 0 && legacyKey != 1) failed = !ScreenGuard.run(this, () -> legacy.mcc$keyTyped('\0', legacyKey));
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (customKeys && !failed) failed = !ScreenGuard.run(this, () -> legacy.mcc$keyTyped(c, 0));
        return super.charTyped(c, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (legacy != null && !failed) failed = !ScreenGuard.run(this, legacy::updateScreen);
    }

    @Override
    public void removed() {
        if (legacy != null && !failed) failed = !ScreenGuard.runQuiet(this, legacy::onGuiClosed);
        super.removed();
    }
}
