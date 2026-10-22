package legacyconnector.v1_7_10.net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** Legacy font renderer over the modern {@link Font}; draws into the active GUI render context. */
public class FontRenderer {
    public static final FontRenderer INSTANCE = new FontRenderer();
    public int FONT_HEIGHT = 9;
    /** The 32 legacy chat colours (16 and their shadows); mods read them, e.g. to colour their own text. */
    public int[] colorCode = legacyColors();

    private static int[] legacyColors() {
        int[] out = new int[32];
        for (int i = 0; i < 32; i++) {
            int j = (i >> 3 & 1) * 85;
            int r = (i >> 2 & 1) * 170 + j;
            int g = (i >> 1 & 1) * 170 + j;
            int b = (i & 1) * 170 + j;
            if (i == 6) r += 85;
            if (i >= 16) {
                r /= 4;
                g /= 4;
                b /= 4;
            }
            out[i] = (r & 255) << 16 | (g & 255) << 8 | b & 255;
        }
        return out;
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    public int drawStringWithShadow(String text, int x, int y, int color) {
        return drawString(text, x, y, color, true);
    }

    public int drawString(String text, int x, int y, int color) {
        return drawString(text, x, y, color, false);
    }

    public int drawString(String text, int x, int y, int color, boolean shadow) {
        if (text == null) return 0;
        var gui = RenderContext.gui();
        if (gui == null) return x + getStringWidth(text);
        // Legacy colors had no alpha; 0 alpha meant opaque.
        int argb = (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
        return gui.drawString(font(), text, x, y, argb, shadow);
    }

    public int getStringWidth(String text) {
        return text == null ? 0 : font().width(text);
    }

    public int getCharWidth(char c) {
        return font().width(String.valueOf(c));
    }

    public String trimStringToWidth(String text, int width) {
        return font().plainSubstrByWidth(text, width);
    }

    public String trimStringToWidth(String text, int width, boolean reverse) {
        if (!reverse) return trimStringToWidth(text, width);
        String r = new StringBuilder(text).reverse().toString();
        return new StringBuilder(trimStringToWidth(r, width)).reverse().toString();
    }

    public void drawSplitString(String text, int x, int y, int width, int color) {
        for (Object line : listFormattedStringToWidth(text, width)) {
            drawString((String) line, x, y, color);
            y += FONT_HEIGHT;
        }
    }

    public int splitStringWidth(String text, int width) {
        return FONT_HEIGHT * listFormattedStringToWidth(text, width).size();
    }

    public List listFormattedStringToWidth(String text, int width) {
        List<String> out = new ArrayList<>();
        for (var line : font().getSplitter().splitLines(text, width, net.minecraft.network.chat.Style.EMPTY)) out.add(line.getString());
        return out;
    }

    public String wrapFormattedStringToWidth(String text, int width) {
        return String.join("\n", listFormattedStringToWidth(text, width));
    }

    public boolean getUnicodeFlag() {
        return false;
    }

    public void setUnicodeFlag(boolean flag) {
    }

    public boolean getBidiFlag() {
        return false;
    }

    public void setBidiFlag(boolean flag) {
    }

    static Component literal(String s) {
        return Component.literal(s);
    }
}
