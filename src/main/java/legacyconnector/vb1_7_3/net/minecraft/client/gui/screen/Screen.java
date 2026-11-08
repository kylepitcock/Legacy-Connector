package legacyconnector.vb1_7_3.net.minecraft.client.gui.screen;

import java.util.ArrayList;
import java.util.List;

import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.client.font.TextRenderer;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.DrawContext;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.widget.ButtonWidget;
import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;
import org.kylepitcock.legacyconnector.vb1_7_3.MenusB173;

/**
 * Beta 1.7.3 {@code GuiScreen}. A modern screen wraps it ({@code LegacyB173Screen}): that screen sets the size ({@link #init(Minecraft, int, int)}),
 * calls {@link #render} inside its own rendering (drawing goes through {@link GuiDrawB173}), and passes mouse clicks and keys on with the
 * LWJGL 2 key codes. The modern screen draws the dimmed world behind it, so {@link #renderBackground()} draws nothing.
 */
public class Screen extends DrawContext {
    protected Minecraft minecraft;
    public int width;
    public int height;
    protected List buttons = new ArrayList();
    public boolean passEvents;
    protected TextRenderer textRenderer;
    private ButtonWidget selectedButton;

    public Screen() {}

    /** MCP {@code drawScreen}: the buttons (subclasses draw the rest first). */
    public void render(int mouseX, int mouseY, float delta) {
        for (Object b : buttons) ((ButtonWidget) b).render(minecraft, mouseX, mouseY);
    }

    /** MCP {@code keyTyped}: Escape closes the screen. */
    protected void keyPressed(char character, int keyCode) {
        if (keyCode == 1) minecraft.setScreen(null);
    }

    public static String getClipboard() {
        return MenusB173.clipboard();
    }

    /** MCP {@code mouseClicked}: a left click on a button selects it and calls {@link #buttonClicked}. */
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        for (Object o : new ArrayList<>(buttons)) {
            ButtonWidget b = (ButtonWidget) o;
            if (b.isMouseOver(minecraft, mouseX, mouseY)) {
                selectedButton = b;
                GuiDrawB173.click();
                buttonClicked(b);
            }
        }
    }

    /** MCP {@code mouseMovedOrUp}. */
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (selectedButton != null && button == 0) {
            selectedButton.mouseReleased(mouseX, mouseY);
            selectedButton = null;
        }
    }

    /** MCP {@code actionPerformed}. */
    protected void buttonClicked(ButtonWidget button) {}

    /** MCP {@code setWorldAndResolution}: the size, the font, no buttons, then {@link #init()}. */
    public void init(Minecraft minecraft, int width, int height) {
        this.minecraft = minecraft;
        this.textRenderer = minecraft.textRenderer;
        this.width = width;
        this.height = height;
        buttons.clear();
        init();
    }

    /** MCP {@code initGui}. */
    public void init() {}

    public void tickInput() {}

    public void onMouseEvent() {}

    public void onKeyboardEvent() {}

    /** MCP {@code updateScreen}. */
    public void tick() {}

    /** MCP {@code onGuiClosed}. */
    public void removed() {}

    /** MCP {@code drawDefaultBackground}: the modern screen already drew it. */
    public void renderBackground() {}

    public void renderBackground(int offset) {}

    public void renderBackgroundTexture(int offset) {}

    public boolean shouldPause() {
        return true;
    }

    /** MCP {@code deleteWorldOrConfirm}. */
    public void confirmed(boolean yes, int id) {}

    public void handleTab() {}

    // ------------------------------------------------------------------ for the modern screen (the legacy methods are protected)

    public void mcc$mouseClicked(int mouseX, int mouseY, int button) {
        mouseClicked(mouseX, mouseY, button);
    }

    public void mcc$mouseReleased(int mouseX, int mouseY, int button) {
        mouseReleased(mouseX, mouseY, button);
    }
    public void mcc$mouseDragged(int mouseX,int mouseY,int button) {}
    public void mcc$mouseScrolled(double amount) {}

    public void mcc$keyPressed(char character, int keyCode) {
        keyPressed(character, keyCode);
    }

    public List<?> mcc$buttons() {
        return buttons;
    }
}
