package org.kylepitcock.legacyconnector.vb1_7_3.client;

import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Menu;
import org.kylepitcock.legacyconnector.vb1_7_3.MenusB173;
import org.slf4j.LoggerFactory;

/**
 * The modern screen of a {@link LegacyB173Menu}: the mod's {@code GuiContainer} draws background, labels and buttons (its {@code drawScreen}),
 * the modern screen the slots, items, carried stack and tooltips, and handles slot clicks. Without a legacy GUI (a ModLoaderMp container,
 * or a GUI that failed) a plain panel with the slots is drawn.
 */
public class LegacyB173Screen extends AbstractContainerScreen<LegacyB173Menu> {
    private final HandledScreen gui;
    private boolean failed;
    private int pendingKey = -1;

    public LegacyB173Screen(LegacyB173Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.gui = menu.gui() instanceof HandledScreen hs ? hs : null;
        this.imageWidth = menu.backgroundWidth();
        this.imageHeight = menu.backgroundHeight();
    }

    private boolean live() {
        return gui != null && !failed;
    }

    private void fail(String what, Throwable e) {
        if (failed) return;
        failed = true;
        LoggerFactory.getLogger("LegacyConnector/b1.7.3").error("b1.7.3 GUI {} failed in {}; drawn as a plain panel", gui.getClass().getName(), what, e);
    }

    @Override
    protected void init() {
        super.init();
        MenusB173.trace("screen for menu {} shown ({}x{} at {},{})", menu.containerId, imageWidth, imageHeight, leftPos, topPos);
        if (!live()) return;
        try {
            gui.init(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance(), width, height);
        } catch (RuntimeException | LinkageError e) {
            fail("initGui", e);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        if (live()) {
            ClientGuiB173.begin(g);
            try {
                gui.render(mouseX, mouseY, partialTick);
            } catch (RuntimeException | LinkageError e) {
                fail("drawScreen", e);
            } finally {
                ClientGuiB173.end();
            }
        }
        if (!live()) plainPanel(g);
    }

    /** A vanilla-like panel with a frame for every slot. */
    private void plainPanel(GuiGraphics g) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        g.renderOutline(x, y, imageWidth, imageHeight, 0xFF555555);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF8B8B8B);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF555555);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void containerTick() {
        if (!live()) return;
        try {
            gui.tick();
        } catch (RuntimeException | LinkageError e) {
            fail("updateScreen", e);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (live()) {
            try {
                gui.mcc$mouseClicked((int) mouseX, (int) mouseY, button);
            } catch (RuntimeException | LinkageError e) {
                fail("mouseClicked", e);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (live()) {
            try {
                gui.mcc$mouseReleased((int) mouseX, (int) mouseY, button);
            } catch (RuntimeException | LinkageError e) {
                fail("mouseMovedOrUp", e);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (live()) {
            if (KeysB173.types(key, modifiers)) {
                pendingKey = key;
            } else {
                legacyKey(KeysB173.character(key,modifiers), KeysB173.lwjgl(key));
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (live()) {
            legacyKey(c, pendingKey < 0 ? 0 : KeysB173.lwjgl(pendingKey));
            pendingKey = -1;
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    private void legacyKey(char c, int code) {
        try {
            gui.mcc$keyPressed(c, code);
        } catch (RuntimeException | LinkageError e) {
            fail("keyTyped", e);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (!live()) return;
        try {
            gui.removed();
        } catch (RuntimeException | LinkageError e) {
            fail("onGuiClosed", e);
        }
    }
}
