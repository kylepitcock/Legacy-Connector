package legacyconnector.v1_7_10.codechicken.lib.gui;

import java.awt.Dimension;
import java.awt.Point;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.gui.Gui;
import legacyconnector.v1_7_10.net.minecraft.client.gui.ScaledResolution;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.input.Mouse;

/** CodeChickenLib's GUI drawing helpers (used by NEI-style recipe handlers and GUIs), over the shim's {@link Gui}. */
public class GuiDraw {
    /** A custom tooltip line: a size and a draw callback. */
    public interface ITooltipLineHandler {
        Dimension getSize();

        void draw(int x, int y);
    }

    public static class GuiHook extends Gui {
        public void setZLevel(float z) {
            this.zLevel = z;
        }

        public float getZLevel() {
            return this.zLevel;
        }

        public void drawGradientRect(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
            super.drawGradientRect(x1, y1, x2, y2, colorTop, colorBottom);
        }
    }

    public static final GuiHook gui = new GuiHook();
    public static FontRenderer fontRenderer = FontRenderer.INSTANCE;
    public static TextureManager renderEngine = TextureManager.INSTANCE;

    private static final java.util.List<ITooltipLineHandler> TIP_LINES = new java.util.ArrayList<>();

    public static Dimension displaySize() {
        ScaledResolution r = new ScaledResolution(Minecraft.getMinecraft(), 0, 0);
        return new Dimension(r.getScaledWidth(), r.getScaledHeight());
    }

    public static Dimension displayRes() {
        return displaySize();
    }

    public static Point getMousePosition() {
        Dimension d = displaySize();
        int w = Math.max(1, Minecraft.mcc$get$displayWidth(Minecraft.getMinecraft()));
        int h = Math.max(1, Minecraft.mcc$get$displayHeight(Minecraft.getMinecraft()));
        return new Point(Mouse.getX() * d.width / w, d.height - Mouse.getY() * d.height / h - 1);
    }

    /** x, y, width, height (not corners, unlike {@link Gui#drawRect}). */
    public static void drawRect(int x, int y, int w, int h, int colour) {
        Gui.drawRect(x, y, x + w, y + h, colour);
    }

    public static void drawGradientRect(int x, int y, int w, int h, int colour1, int colour2) {
        gui.drawGradientRect(x, y, x + w, y + h, colour1, colour2);
    }

    public static void drawString(String text, int x, int y, int colour, boolean shadow) {
        fontRenderer.drawString(text, x, y, colour, shadow);
    }

    public static void drawString(String text, int x, int y, int colour) {
        drawString(text, x, y, colour, true);
    }

    public static void drawStringC(String text, int x, int y, int w, int h, int colour, boolean shadow) {
        drawString(text, x + (w - getStringWidth(text)) / 2, y + (h - 8) / 2, colour, shadow);
    }

    public static void drawStringC(String text, int x, int y, int w, int h, int colour) {
        drawStringC(text, x, y, w, h, colour, true);
    }

    public static void drawStringC(String text, int x, int y, int colour, boolean shadow) {
        drawString(text, x - getStringWidth(text) / 2, y, colour, shadow);
    }

    public static void drawStringC(String text, int x, int y, int colour) {
        drawStringC(text, x, y, colour, true);
    }

    public static void drawStringR(String text, int x, int y, int colour, boolean shadow) {
        drawString(text, x - getStringWidth(text), y, colour, shadow);
    }

    public static void drawStringR(String text, int x, int y, int colour) {
        drawStringR(text, x, y, colour, true);
    }

    public static int getStringWidth(String text) {
        return fontRenderer.getStringWidth(text);
    }

    public static void changeTexture(String texture) {
        renderEngine.bindTexture(new ResourceLocation(texture));
    }

    public static void changeTexture(ResourceLocation texture) {
        renderEngine.bindTexture(texture);
    }

    public static void drawTexturedModalRect(int x, int y, int tx, int ty, int w, int h) {
        gui.drawTexturedModalRect(x, y, tx, ty, w, h);
    }

    public static int getTipLineId(ITooltipLineHandler handler) {
        int i = TIP_LINES.indexOf(handler);
        if (i < 0) {
            TIP_LINES.add(handler);
            i = TIP_LINES.size() - 1;
        }
        return i;
    }

    public static ITooltipLineHandler getTipLine(int id) {
        return id >= 0 && id < TIP_LINES.size() ? TIP_LINES.get(id) : null;
    }
}
