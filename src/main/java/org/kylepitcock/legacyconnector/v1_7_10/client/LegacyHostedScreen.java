package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.kylepitcock.legacyconnector.v1_7_10.gui.LegacyMenu;

/**
 * Hosts a legacy GUI that is a plain {@link GuiScreen} with a container of its own (Tinkers' Construct's {@code NewContainerGui}: a copy of
 * GuiContainer that draws its slots and handles its clicks itself, through {@code playerController.windowClick}). The server still opens a modern
 * menu for the container; this screen leaves everything to the legacy code and only forwards input, so slots are not drawn or clicked twice.
 */
public class LegacyHostedScreen extends AbstractContainerScreen<LegacyMenu> {
    private final GuiScreen legacy;
    private boolean failed;

    public LegacyHostedScreen(LegacyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.legacy = (GuiScreen) menu.clientGui();
    }

    /** The container a screen keeps in its own field (not a GuiContainer's {@code inventorySlots}), or null. */
    public static legacyconnector.v1_7_10.net.minecraft.inventory.Container containerOf(GuiScreen screen) {
        for (Class<?> c = screen.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        || !legacyconnector.v1_7_10.net.minecraft.inventory.Container.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object value = f.get(screen);
                    if (value instanceof legacyconnector.v1_7_10.net.minecraft.inventory.Container container) return container;
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // the next field
                }
            }
        }
        return null;
    }

    @Override
    protected void init() {
        super.init();
        failed = !ScreenGuard.run(this, () -> legacy.setWorldAndResolution(Minecraft.getMinecraft(), width, height));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (failed) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        GuiScreen.mcc$beginFrame();
        RenderContext.beginGui(graphics);
        RenderContext.mcc$beginScreenDepth(); // 1.7.10's screen depth state: cleared depth, depth test on
        try {
            failed = !ScreenGuard.run(this, () -> legacy.drawScreen(mouseX, mouseY, partialTick));
        } finally {
            RenderContext.end();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // the legacy drawScreen draws the background, the slots and the labels
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (failed) return super.mouseClicked(x, y, button);
        failed = !ScreenGuard.run(this, () -> legacy.mcc$mouseClicked((int) x, (int) y, button));
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (failed) return super.mouseReleased(x, y, button);
        failed = !ScreenGuard.run(this, () -> legacy.mcc$mouseReleased((int) x, (int) y, button));
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (failed) return super.mouseDragged(x, y, button, dx, dy);
        failed = !ScreenGuard.run(this, () -> legacy.mcc$mouseDragged((int) x, (int) y, button));
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!failed) failed = !ScreenGuard.run(this, () -> legacy.mcc$mouseScrolled((int) x, (int) y, scrollY));
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        int legacyKey = LegacyKeys.toLegacy(key);
        if (legacyKey != 0 && !failed) failed = !ScreenGuard.run(this, () -> legacy.mcc$keyReleased(legacyKey));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (failed) return super.keyPressed(key, scanCode, modifiers);
        int legacyKey = LegacyKeys.toLegacy(key);
        if (legacyKey != 0) failed = !ScreenGuard.run(this, () -> legacy.mcc$keyTyped('\0', legacyKey));
        // the base GuiScreen.keyTyped closes on escape through displayGuiScreen(null), which would leave the container open on the server
        if (key == 256 && minecraft != null && minecraft.screen == this) onClose();
        return true;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!failed) failed = !ScreenGuard.run(this, () -> legacy.mcc$keyTyped(c, 0));
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!failed) failed = !ScreenGuard.run(this, legacy::updateScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return legacy.doesGuiPauseGame();
    }

    @Override
    public void removed() {
        if (!failed) failed = !ScreenGuard.runQuiet(this, legacy::onGuiClosed);
        super.removed();
    }
}
