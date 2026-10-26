package legacyconnector.v1_7_10.cpw.mods.fml.client.config;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** FML's GUI drawing helpers: nine-sliced boxes (buttons, sliders, check boxes of any size) and the config screens' symbols. */
public class GuiUtils {
    public static final String UNDO_CHAR = "↶";
    public static final String RESET_CHAR = "☄";
    public static final String VALID = "✔";
    public static final String INVALID = "✕";

    public static int[] colorCodes = new int[] {0, 170, 43520, 43690, 11141120, 11141290, 16755200, 11184810, 5592405, 5592575, 5635925, 5636095,
            16733525, 16733695, 16777045, 16777215, 0, 42, 10752, 10794, 2752512, 2752554, 2763264, 2763306, 1381653, 1381695, 1392405, 1392447,
            4134165, 4134207, 4144917, 4144959};

    /** The RGB of a formatting colour code; the lighter variant is the text colour, the other its shadow. */
    public static int getColorCode(char c, boolean isLighter) {
        return colorCodes[isLighter ? "0123456789abcdef".indexOf(c) : "0123456789abcdef".indexOf(c) + 16];
    }

    public static void drawContinuousTexturedBox(ResourceLocation res, int x, int y, int u, int v, int width, int height, int textureWidth,
            int textureHeight, int borderSize, float zLevel) {
        drawContinuousTexturedBox(res, x, y, u, v, width, height, textureWidth, textureHeight, borderSize, borderSize, borderSize, borderSize, zLevel);
    }

    public static void drawContinuousTexturedBox(int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight,
            int borderSize, float zLevel) {
        drawContinuousTexturedBox(x, y, u, v, width, height, textureWidth, textureHeight, borderSize, borderSize, borderSize, borderSize, zLevel);
    }

    public static void drawContinuousTexturedBox(ResourceLocation res, int x, int y, int u, int v, int width, int height, int textureWidth,
            int textureHeight, int topBorder, int bottomBorder, int leftBorder, int rightBorder, float zLevel) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(res);
        drawContinuousTexturedBox(x, y, u, v, width, height, textureWidth, textureHeight, topBorder, bottomBorder, leftBorder, rightBorder, zLevel);
    }

    /**
     * Draws a {@code width} x {@code height} box from the {@code textureWidth} x {@code textureHeight} region at (u, v) of the bound
     * 256x256 texture: the corners as they are, the borders and the middle repeated.
     */
    public static void drawContinuousTexturedBox(int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight,
            int topBorder, int bottomBorder, int leftBorder, int rightBorder, float zLevel) {
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        int fillerWidth = textureWidth - leftBorder - rightBorder;
        int fillerHeight = textureHeight - topBorder - bottomBorder;
        if (fillerWidth <= 0 || fillerHeight <= 0) return;
        int canvasWidth = width - leftBorder - rightBorder;
        int canvasHeight = height - topBorder - bottomBorder;
        int xPasses = canvasWidth / fillerWidth;
        int remainderWidth = canvasWidth % fillerWidth;
        int yPasses = canvasHeight / fillerHeight;
        int remainderHeight = canvasHeight % fillerHeight;

        drawTexturedModalRect(x, y, u, v, leftBorder, topBorder, zLevel);
        drawTexturedModalRect(x + leftBorder + canvasWidth, y, u + leftBorder + fillerWidth, v, rightBorder, topBorder, zLevel);
        drawTexturedModalRect(x, y + topBorder + canvasHeight, u, v + topBorder + fillerHeight, leftBorder, bottomBorder, zLevel);
        drawTexturedModalRect(x + leftBorder + canvasWidth, y + topBorder + canvasHeight, u + leftBorder + fillerWidth, v + topBorder + fillerHeight,
                rightBorder, bottomBorder, zLevel);

        for (int i = 0; i < xPasses + (remainderWidth > 0 ? 1 : 0); i++) {
            int w = i == xPasses ? remainderWidth : fillerWidth;
            drawTexturedModalRect(x + leftBorder + i * fillerWidth, y, u + leftBorder, v, w, topBorder, zLevel);
            drawTexturedModalRect(x + leftBorder + i * fillerWidth, y + topBorder + canvasHeight, u + leftBorder, v + topBorder + fillerHeight, w,
                    bottomBorder, zLevel);
            for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) {
                drawTexturedModalRect(x + leftBorder + i * fillerWidth, y + topBorder + j * fillerHeight, u + leftBorder, v + topBorder, w,
                        j == yPasses ? remainderHeight : fillerHeight, zLevel);
            }
        }
        for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) {
            int h = j == yPasses ? remainderHeight : fillerHeight;
            drawTexturedModalRect(x, y + topBorder + j * fillerHeight, u, v + topBorder, leftBorder, h, zLevel);
            drawTexturedModalRect(x + leftBorder + canvasWidth, y + topBorder + j * fillerHeight, u + leftBorder + fillerWidth, v + topBorder, rightBorder,
                    h, zLevel);
        }
    }

    /** A region of the bound 256x256 texture. */
    public static void drawTexturedModalRect(int x, int y, int u, int v, int width, int height, float zLevel) {
        var gui = RenderContext.gui();
        if (gui == null || width <= 0 || height <= 0 || RenderContext.texture() == null) return;
        gui.blit(RenderContext.texture(), x, y, (int) zLevel, (float) u, (float) v, width, height, 256, 256);
    }
}
