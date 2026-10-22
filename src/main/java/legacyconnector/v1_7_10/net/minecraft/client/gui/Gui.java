package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** 1.7.10 drawing helpers, drawn through the active {@code GuiGraphics}. */
public class Gui {
    public static final ResourceLocation optionsBackground = new ResourceLocation("textures/gui/options_background.png");
    public static final ResourceLocation statIcons = new ResourceLocation("textures/gui/container/stats_icons.png");
    public static final ResourceLocation icons = new ResourceLocation("textures/gui/icons.png");
    protected float zLevel;

    protected void drawHorizontalLine(int x1, int x2, int y, int color) {
        if (x2 < x1) {
            int t = x1;
            x1 = x2;
            x2 = t;
        }
        drawRect(x1, y, x2 + 1, y + 1, color);
    }

    protected void drawVerticalLine(int x, int y1, int y2, int color) {
        if (y2 < y1) {
            int t = y1;
            y1 = y2;
            y2 = t;
        }
        drawRect(x, y1 + 1, x + 1, y2, color);
    }

    public static void drawRect(int x1, int y1, int x2, int y2, int color) {
        var gui = RenderContext.gui();
        if (gui != null) gui.fill(x1, y1, x2, y2, color);
    }

    protected void drawGradientRect(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
        var gui = RenderContext.gui();
        if (gui != null) gui.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
    }

    public void drawCenteredString(FontRenderer font, String text, int x, int y, int color) {
        font.drawStringWithShadow(text, x - font.getStringWidth(text) / 2, y, color);
    }

    public void drawString(FontRenderer font, String text, int x, int y, int color) {
        font.drawStringWithShadow(text, x, y, color);
    }

    /** Blits a 256x256-texture region from the bound texture (the classic 1.7.10 behaviour). */
    public void drawTexturedModalRect(int x, int y, int u, int v, int width, int height) {
        var gui = RenderContext.gui();
        if (gui != null) gui.blit(RenderContext.texture(), x, y, u, v, width, height);
    }

    public void drawTexturedModelRectFromIcon(int x, int y, IIcon icon, int width, int height) {
        var gui = RenderContext.gui();
        if (gui == null || icon == null) return;
        var sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                .apply(net.minecraft.resources.ResourceLocation.parse(icon.getIconName()));
        gui.blit(x, y, 0, width, height, sprite);
    }

    public static void drawModalRectWithCustomSizedTexture(int x, int y, float u, float v, int width, int height, float texW, float texH) {
        var gui = RenderContext.gui();
        if (gui != null) gui.blit(RenderContext.texture(), x, y, u, v, width, height, (int) texW, (int) texH);
    }

    public static void drawScaledCustomSizeModalRect(int x, int y, float u, float v, int uW, int vH, int width, int height, float texW, float texH) {
        var gui = RenderContext.gui();
        if (gui != null) gui.blit(RenderContext.texture(), x, y, width, height, u, v, uW, vH, (int) texW, (int) texH);
    }
}
