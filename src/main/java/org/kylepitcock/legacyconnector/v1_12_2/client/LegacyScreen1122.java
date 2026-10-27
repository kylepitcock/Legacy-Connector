package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.ScreenGuard;

/** Hosts a 1.12.2 {@link GuiScreen} that has no container (config screens, manuals, client-only GUIs). */
public class LegacyScreen1122 extends Screen {
    private final GuiScreen legacy;
    private final KeyRouter1122 keys = new KeyRouter1122();
    private boolean failed;

    public LegacyScreen1122(GuiScreen legacy) {
        super(Component.empty());
        this.legacy = legacy;
    }

    public GuiScreen legacy() {
        return legacy;
    }

    private void guard(Runnable code) {
        if (!failed) failed = !ScreenGuard.run(this, code);
    }

    @Override
    protected void init() {
        failed = false;
        guard(() -> legacy.setWorldAndResolution(Minecraft.getMinecraft(), width, height));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (failed) return;
        RenderContext.beginGui(graphics);
        try {
            guard(() -> legacy.drawScreen(mouseX, mouseY, partialTick));
        } finally {
            RenderContext.end();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // legacy drawScreen draws its own background (drawDefaultBackground)
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        keys.flush(legacy);
        guard(() -> legacy.mcc$mouseClicked((int) x, (int) y, button));
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        guard(() -> legacy.mcc$mouseReleased((int) x, (int) y, button));
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        guard(() -> legacy.mcc$mouseDragged((int) x, (int) y));
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        guard(() -> legacy.mcc$mouseScrolled((int) x, (int) y, scrollY));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        guard(() -> keys.keyPressed(legacy, key, modifiers));
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        guard(() -> keys.keyReleased(legacy, key));
        return true;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        guard(() -> keys.charTyped(legacy, c));
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        guard(() -> {
            keys.flush(legacy);
            legacy.updateScreen();
        });
    }

    @Override
    public boolean isPauseScreen() {
        return legacy.doesGuiPauseGame();
    }

    @Override
    public void removed() {
        ScreenGuard.runQuiet(this, legacy::onGuiClosed);
    }
}
