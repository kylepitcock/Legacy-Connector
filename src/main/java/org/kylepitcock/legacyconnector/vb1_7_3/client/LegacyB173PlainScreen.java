package org.kylepitcock.legacyconnector.vb1_7_3.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

/** A modern screen showing a b1.7.3 {@code GuiScreen} without container (a timer's settings, a naming screen): it draws, ticks and gets input. */
public class LegacyB173PlainScreen extends Screen {
    private final legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen gui;
    private final Screen returnScreen;
    private boolean failed;
    private int pendingKey = -1;

    public LegacyB173PlainScreen(legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen gui) {
        this(gui,null);
    }

    public LegacyB173PlainScreen(legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen gui,Screen returnScreen) {
        super(Component.empty());
        this.gui = gui;
        this.returnScreen=returnScreen;
    }

    public Screen returnScreen(){return returnScreen;}

    @Override public void onClose(){minecraft.setScreen(returnScreen);}

    public legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen legacy() {
        return gui;
    }

    private void fail(String what, Throwable e) {
        if (failed) return;
        failed = true;
        LoggerFactory.getLogger("LegacyConnector/b1.7.3").error("b1.7.3 screen {} failed in {}; closed", gui.getClass().getName(), what, e);
        onClose();
    }

    @Override
    protected void init() {
        try {
            gui.init(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance(), width, height);
        } catch (RuntimeException | LinkageError e) {
            fail("initGui", e);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (failed) return;
        ClientGuiB173.begin(g);
        try {
            gui.render(mouseX, mouseY, partialTick);
        } catch (RuntimeException | LinkageError e) {
            fail("drawScreen", e);
        } finally {
            ClientGuiB173.end();
        }
    }

    @Override
    public void tick() {
        if (failed) return;
        try {
            gui.tick();
        } catch (RuntimeException | LinkageError e) {
            fail("updateScreen", e);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        try {
            gui.mcc$mouseClicked((int) mouseX, (int) mouseY, button);
        } catch (RuntimeException | LinkageError e) {
            fail("mouseClicked", e);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        try {
            gui.mcc$mouseReleased((int) mouseX, (int) mouseY, button);
        } catch (RuntimeException | LinkageError e) {
            fail("mouseMovedOrUp", e);
        }
        return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){try{gui.mcc$mouseDragged((int)x,(int)y,button);}catch(RuntimeException|LinkageError e){fail("mouseDragged",e);}return true;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){try{gui.mcc$mouseScrolled(vertical);}catch(RuntimeException|LinkageError e){fail("mouseScrolled",e);}return true;}

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (KeysB173.types(key, modifiers)) {
            pendingKey = key;
            return true;
        }
        legacyKey(KeysB173.character(key), KeysB173.lwjgl(key));
        // a screen that swallowed Escape still closes
        if (key == GLFW.GLFW_KEY_ESCAPE && minecraft != null && minecraft.screen == this) onClose();
        return true;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        legacyKey(c, pendingKey < 0 ? 0 : KeysB173.lwjgl(pendingKey));
        pendingKey = -1;
        return true;
    }

    private void legacyKey(char c, int code) {
        if (failed) return;
        try {
            gui.mcc$keyPressed(c, code);
        } catch (RuntimeException | LinkageError e) {
            fail("keyTyped", e);
        }
    }

    @Override
    public void removed() {
        try {
            gui.removed();
        } catch (RuntimeException | LinkageError e) {
            // closing anyway
        }
    }

    @Override
    public boolean isPauseScreen() {
        try {
            return gui.shouldPause();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
