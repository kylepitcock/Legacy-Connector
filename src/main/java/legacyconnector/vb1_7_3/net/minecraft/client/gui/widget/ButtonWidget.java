package legacyconnector.vb1_7_3.net.minecraft.client.gui.widget;

import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.DrawContext;
import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;

/** Beta 1.7.3 {@code GuiButton}: drawn from {@code /gui/gui.png} as before (the layer builds that sheet from the modern button sprites). */
public class ButtonWidget extends DrawContext {
    public int width;
    public int height;
    public int x;
    public int y;
    public String text;
    public int id;
    public boolean active = true;
    public boolean visible = true;

    public ButtonWidget(int id, int x, int y, String text) {
        this(id, x, y, 200, 20, text);
    }

    public ButtonWidget(int id, int x, int y, int width, int height, String text) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
    }

    /** MCP {@code getHoverState}: 0 disabled, 1 normal, 2 hovered. */
    protected int getYImage(boolean hovered) {
        if (!active) return 0;
        return hovered ? 2 : 1;
    }

    /** MCP {@code drawButton}. */
    public void render(Minecraft minecraft, int mouseX, int mouseY) {
        if (!visible) return;
        GuiDrawB173.bind(GuiDrawB173.textureId("/gui/gui.png"));
        GuiDrawB173.color(1.0F, 1.0F, 1.0F, 1.0F);
        boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        int v = 46 + getYImage(hovered) * 20;
        drawTexture(x, y, 0, v, width / 2, height);
        drawTexture(x + width / 2, y, 200 - width / 2, v, width / 2, height);
        renderBackground(minecraft, mouseX, mouseY);
        int color = !active ? 0xFFA0A0A0 : hovered ? 0xFFFFFFA0 : 0xFFE0E0E0;
        if (minecraft != null && minecraft.textRenderer != null) {
            drawCenteredTextWithShadow(minecraft.textRenderer, text, x + width / 2, y + (height - 8) / 2, color);
        }
    }

    /** MCP {@code mouseDragged}: sliders draw their knob here. */
    protected void renderBackground(Minecraft minecraft, int mouseX, int mouseY) {}

    public void mouseReleased(int mouseX, int mouseY) {}

    /** MCP {@code mousePressed}. */
    public boolean isMouseOver(Minecraft minecraft, int mouseX, int mouseY) {
        return active && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
}
