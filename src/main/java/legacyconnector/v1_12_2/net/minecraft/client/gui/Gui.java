package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2 drawing helpers, drawn through the active {@code GuiGraphics} of the hosting modern screen. Textured rectangles use the texture
 * last bound through the render context ({@code TextureManager.bindTexture}).
 */
public class Gui {
    public static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
    public static final ResourceLocation STAT_ICONS = new ResourceLocation("textures/gui/container/stats_icons.png");
    public static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    protected float zLevel;

    public Gui() {
    }

    protected void drawHorizontalLine(int startX, int endX, int y, int color) {
        if (endX < startX) {
            int t = startX;
            startX = endX;
            endX = t;
        }
        drawRect(startX, y, endX + 1, y + 1, color);
    }

    protected void drawVerticalLine(int x, int startY, int endY, int color) {
        if (endY < startY) {
            int t = startY;
            startY = endY;
            endY = t;
        }
        drawRect(x, startY + 1, x + 1, endY, color);
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        var gui = RenderContext.gui();
        if (gui != null) gui.fill(left, top, right, bottom, color);
    }

    protected void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        var gui = RenderContext.gui();
        if (gui != null) gui.fillGradient(left, top, right, bottom, startColor, endColor);
    }

    public void drawCenteredString(FontRenderer font, String text, int x, int y, int color) {
        if (font == null) font = FontRenderer.mcc$INSTANCE;
        font.drawStringWithShadow(text, (float) (x - font.getStringWidth(text) / 2), (float) y, color);
    }

    public void drawString(FontRenderer font, String text, int x, int y, int color) {
        if (font == null) font = FontRenderer.mcc$INSTANCE;
        font.drawStringWithShadow(text, (float) x, (float) y, color);
    }

    /** Blits a region of the bound texture, taken as 256x256 (the classic behaviour). */
    public void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height) {
        var gui = RenderContext.gui();
        if (gui != null) gui.blit(RenderContext.texture(), x, y, textureX, textureY, width, height);
    }

    public void drawTexturedModalRect(float xCoord, float yCoord, int minU, int minV, int maxU, int maxV) {
        var gui = RenderContext.gui();
        if (gui == null) return;
        var pose = gui.pose();
        pose.pushPose();
        pose.translate(xCoord - (int) xCoord, yCoord - (int) yCoord, 0.0F);
        gui.blit(RenderContext.texture(), (int) xCoord, (int) yCoord, minU, minV, maxU, maxV);
        pose.popPose();
    }

    /**
     * Draws a sprite of the bound atlas (normally the blocks atlas) at its full UV range, scaled to the size given. A sprite the modern
     * blocks atlas has is drawn from there; any other is drawn with its own UVs from the bound texture.
     */
    public void drawTexturedModalRect(int xCoord, int yCoord, legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite textureSprite, int widthIn, int heightIn) {
        var gui = RenderContext.gui();
        if (gui == null || textureSprite == null) return;
        var modern = textureSprite.mcc$modern();
        if (modern != null) {
            gui.blit(xCoord, yCoord, (int) zLevel, widthIn, heightIn, modern);
        } else {
            mcc$blitUV(xCoord, yCoord, widthIn, heightIn, textureSprite.getMinU(), textureSprite.getMinV(), textureSprite.getMaxU(), textureSprite.getMaxV());
        }
    }

    /** Blits the bound texture's UV rectangle (0..1 coordinates) to a screen rectangle. */
    public static void mcc$blitUV(int x, int y, int width, int height, float u0, float v0, float u1, float v1) {
        var gui = RenderContext.gui();
        if (gui == null) return;
        final int res = 65536;
        gui.blit(RenderContext.texture(), x, y, width, height, u0 * res, v0 * res, Math.round((u1 - u0) * res), Math.round((v1 - v0) * res), res, res);
    }

    public static void drawModalRectWithCustomSizedTexture(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        var gui = RenderContext.gui();
        if (gui != null) gui.blit(RenderContext.texture(), x, y, u, v, width, height, (int) textureWidth, (int) textureHeight);
    }

    public static void drawScaledCustomSizeModalRect(int x, int y, float u, float v, int uWidth, int vHeight, int width, int height, float tileWidth, float tileHeight) {
        var gui = RenderContext.gui();
        if (gui != null) gui.blit(RenderContext.texture(), x, y, width, height, u, v, uWidth, vHeight, (int) tileWidth, (int) tileHeight);
    }
}
