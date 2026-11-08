package legacyconnector.vb1_7_3.net.minecraft.client.gui;

import legacyconnector.vb1_7_3.net.minecraft.client.font.TextRenderer;
import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;

/** Beta 1.7.3 {@code Gui}: textured rectangles, fills and text, drawn into the modern screen being rendered ({@link GuiDrawB173}). */
public class DrawContext {
    protected float zOffset;

    public DrawContext() {}

    protected void drawHorizontalLine(int x1, int x2, int y, int color) {
        if (x2 < x1) {
            int t = x1;
            x1 = x2;
            x2 = t;
        }
        fill(x1, y, x2 + 1, y + 1, color);
    }

    protected void drawVerticalLine(int x, int y1, int y2, int color) {
        if (y2 < y1) {
            int t = y1;
            y1 = y2;
            y2 = t;
        }
        fill(x, y1 + 1, x + 1, y2, color);
    }

    /** MCP {@code drawRect}. */
    protected void fill(int x1, int y1, int x2, int y2, int color) {
        GuiDrawB173.fill(x1, y1, x2, y2, color);
    }

    protected void fillGradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        GuiDrawB173.fillGradient(x1, y1, x2, y2, top, bottom);
    }

    public void drawCenteredTextWithShadow(TextRenderer renderer, String text, int x, int y, int color) {
        renderer.drawWithShadow(text, x - renderer.getWidth(text) / 2, y, color);
    }

    public void drawTextWithShadow(TextRenderer renderer, String text, int x, int y, int color) {
        renderer.drawWithShadow(text, x, y, color);
    }

    /** MCP {@code drawTexturedModalRect}: a part of the bound 256x256 texture. */
    public void drawTexture(int x, int y, int u, int v, int width, int height) {
        GuiDrawB173.blit(x, y, u, v, width, height);
    }
}
