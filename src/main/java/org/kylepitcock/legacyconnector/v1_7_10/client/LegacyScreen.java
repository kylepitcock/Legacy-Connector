package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Hosts a legacy {@link GuiScreen} that has no container (config screens, books, client-only GUIs). */
public class LegacyScreen extends Screen {
    private final GuiScreen legacy;
    private boolean failed;

    public LegacyScreen(GuiScreen legacy) {
        super(Component.empty());
        this.legacy = legacy;
    }

    public GuiScreen legacy() {
        return legacy;
    }

    @Override
    protected void init() {
        failed = !ScreenGuard.run(this, () -> legacy.setWorldAndResolution(Minecraft.getMinecraft(), width, height));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (failed) return;
        GuiScreen.mcc$beginFrame();
        RenderContext.beginGui(graphics);
        // 1.7.10 drew screens with depth testing off; mods turn it on where they need it (the Thaumonomicon draws its map
        // with GL_GEQUAL before enabling it, and lost the map and frame to the depth the modern defaults left)
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST); // the state cache can be out of step with GL
        RenderContext.mcc$setLegacyDepthTest(false);
        // the held item's depth is still in the buffer and kept the darkening backdrop off it
        com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, net.minecraft.client.Minecraft.ON_OSX);
        try {
            failed = !ScreenGuard.run(this, () -> legacy.drawScreen(mouseX, mouseY, partialTick));
        } finally {
            RenderContext.end();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // legacy drawScreen draws its own background (drawDefaultBackground)
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        legacy.mcc$mouseClicked((int) x, (int) y, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        legacy.mcc$mouseReleased((int) x, (int) y, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        legacy.mcc$mouseDragged((int) x, (int) y, button);
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
        int legacyKey = LegacyKeys.toLegacy(key);
        if (legacyKey != 0) legacy.mcc$keyTyped('\0', legacyKey);
        return true;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        legacy.mcc$keyTyped(c, 0);
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        legacy.updateScreen();
    }

    @Override
    public boolean isPauseScreen() {
        return legacy.doesGuiPauseGame();
    }

    @Override
    public void removed() {
        legacy.onGuiClosed();
    }
}
