package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import net.minecraft.client.gui.Font;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2 font renderer drawn with the modern font, which understands the same {@code §} formatting codes. In a GUI it draws through the
 * active {@code GuiGraphics}; elsewhere (text over blocks and entities) with the legacy matrix of the render context. Widths and wrapping
 * use the modern font's metrics, which match the 1.12 glyph widths of the default font.
 */
public class FontRenderer implements legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManagerReloadListener {
    public static final FontRenderer mcc$INSTANCE = new FontRenderer();

    public int FONT_HEIGHT = 9;
    public Random fontRandom = new Random();
    protected int[] colorCode = legacyColors();
    protected int[] charWidth = new int[256];
    protected byte[] glyphWidth = new byte[65536];
    protected ResourceLocation locationFontTexture = new ResourceLocation("textures/font/ascii.png");
    protected float posX;
    protected float posY;
    protected boolean unicodeFlag;
    protected boolean bidiFlag;
    protected float red;
    protected float blue;
    protected float green;
    protected float alpha;
    protected int textColor;
    protected boolean randomStyle;
    protected boolean boldStyle;
    protected boolean italicStyle;
    protected boolean underlineStyle;
    protected boolean strikethroughStyle;

    /** Mods' own font renderers (custom font sheets) are built through the 1.12 constructor, which links here; they draw with the game font. */
    public FontRenderer() {
    }

    /**
     * 1.12's constructor: a font sheet, the texture manager and the unicode flag. Mods' renderers built this way (a custom sheet or a
     * subclass that restyles text) draw with the game font; the sheet and flag are kept for code that reads them.
     */
    public FontRenderer(legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings gameSettingsIn, ResourceLocation location,
            legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager textureManagerIn, boolean unicode) {
        if (location != null) this.locationFontTexture = location;
        this.unicodeFlag = unicode;
    }

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
        return net.minecraft.client.Minecraft.getInstance().font;
    }

    @Override
    public void onResourceManagerReload(legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager manager) {
    }

    public int drawStringWithShadow(String text, float x, float y, int color) {
        return drawString(text, x, y, color, true);
    }

    public int drawString(String text, int x, int y, int color) {
        return drawString(text, (float) x, (float) y, color, false);
    }

    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        if (text == null) return 0;
        if (bidiFlag) text = bidiReorder(text);
        // legacy colours had no alpha; 0 alpha meant opaque
        int argb = (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
        var gui = RenderContext.gui();
        if (gui != null) return gui.drawString(font(), text, x, y, argb, dropShadow);
        var pose = RenderContext.pose();
        if (pose == null) return (int) x + getStringWidth(text);
        // text in the world (labels over blocks): drawn with the legacy matrix; without depth testing it shows through blocks
        return font().drawInBatch(text, x, y, argb, dropShadow, pose.last().pose(), RenderContext.buffers(),
                RenderContext.depthTest() ? Font.DisplayMode.NORMAL : Font.DisplayMode.SEE_THROUGH, 0, RenderContext.light());
    }

    /** Draws one run of text and returns the pen position after it. */
    protected int renderString(String text, float x, float y, int color, boolean dropShadow) {
        int end = drawString(text, x, y, color, dropShadow);
        posX = end;
        posY = y;
        return end;
    }

    protected void renderStringAtPos(String text, boolean shadow) {
        posX = renderString(text, posX, posY, textColor, shadow);
    }

    protected float renderDefaultChar(int ch, boolean italic) {
        return renderChar((char) ch);
    }

    protected float renderUnicodeChar(char ch, boolean italic) {
        return renderChar(ch);
    }

    /** Draws one character at the pen position and returns its advance (1.12: spaces, then the ASCII sheet or a unicode page). */
    protected float renderChar(char ch, boolean italic) {
        if (ch == 160 || ch == ' ') return 4.0F;
        return renderChar(ch);
    }

    /** Forge: after a character, the underline and strikethrough of its advance, then the pen moves on. */
    protected void doDraw(float f) {
        int color = textColor != 0 ? textColor | 0xFF000000 : 0xFFFFFFFF;
        if (strikethroughStyle) Gui.drawRect((int) posX, (int) (posY + FONT_HEIGHT / 2.0F), (int) (posX + f), (int) (posY + FONT_HEIGHT / 2.0F) + 1, color);
        if (underlineStyle) Gui.drawRect((int) posX - 1, (int) (posY + FONT_HEIGHT), (int) (posX + f), (int) (posY + FONT_HEIGHT) + 1, color);
        posX += (float) (int) f;
    }

    private float renderChar(char ch) {
        String s = String.valueOf(ch);
        int color = textColor != 0 ? textColor : ((int) (alpha * 255) & 255) << 24 | ((int) (red * 255) & 255) << 16 | ((int) (blue * 255) & 255) << 8 | (int) (green * 255) & 255;
        drawString(s, posX, posY, color, false);
        return getCharWidth(ch);
    }

    protected void resetStyles() {
        randomStyle = false;
        boldStyle = false;
        italicStyle = false;
        underlineStyle = false;
        strikethroughStyle = false;
    }

    protected void setColor(float r, float g, float b, float a) {
        this.red = r;
        this.blue = g;
        this.green = b;
        this.alpha = a;
    }

    protected void bindTexture(ResourceLocation location) {
        RenderContext.bindTexture(location.mcc$modern());
    }

    private String bidiReorder(String text) {
        try {
            java.text.Bidi bidi = new java.text.Bidi(text, java.text.Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
            if (!bidi.isMixed() && bidi.isLeftToRight()) return text;
            StringBuilder sb = new StringBuilder();
            for (int run = bidi.getRunCount() - 1; run >= 0; run--) {
                String part = text.substring(bidi.getRunStart(run), bidi.getRunLimit(run));
                sb.append((bidi.getRunLevel(run) & 1) == 1 ? new StringBuilder(part).reverse() : part);
            }
            return sb.toString();
        } catch (RuntimeException e) {
            return text;
        }
    }

    public int getStringWidth(String text) {
        if (text == null) return 0;
        if (font() == null) {
            int width = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == 167 && i + 1 < text.length()) {
                    i++; // a formatting code has no width
                    continue;
                }
                width += mcc$defaultWidth(c);
            }
            return width;
        }
        return font().width(text);
    }

    public int getCharWidth(char character) {
        if (character == 167) return -1;
        if (character == ' ') return 4;
        if (font() == null) return mcc$defaultWidth(character);
        return font().width(String.valueOf(character));
    }

    /**
     * The default font's advance of a character, for text measured before the modern client has its font (mods build their font
     * renderers and wrap text during init; Immersive Engineering's manual font): vanilla's ASCII widths, 6 for anything else.
     */
    private static int mcc$defaultWidth(char c) {
        if (c == ' ') return 4;
        if ("!',.:;i|".indexOf(c) >= 0) return 2;
        if ("`l".indexOf(c) >= 0) return 3;
        if ("It[]".indexOf(c) >= 0) return 4;
        if ("\"()*<>fk{}".indexOf(c) >= 0) return 5;
        return 6;
    }

    public String trimStringToWidth(String text, int width) {
        return trimStringToWidth(text, width, false);
    }

    public String trimStringToWidth(String text, int width, boolean reverse) {
        if (text == null) return "";
        return reverse ? font().plainSubstrByWidth(text, width, true) : font().plainSubstrByWidth(text, width);
    }

    public void drawSplitString(String str, int x, int y, int wrapWidth, int textColor) {
        resetStyles();
        this.textColor = textColor;
        for (String line : listFormattedStringToWidth(str, wrapWidth)) {
            drawString(line, (float) x, (float) y, textColor, false);
            y += FONT_HEIGHT;
        }
    }

    public int getWordWrappedHeight(String str, int maxLength) {
        return FONT_HEIGHT * listFormattedStringToWidth(str, maxLength).size();
    }

    public void setUnicodeFlag(boolean unicodeFlag) {
        this.unicodeFlag = unicodeFlag;
    }

    public boolean getUnicodeFlag() {
        return unicodeFlag;
    }

    public void setBidiFlag(boolean bidiFlag) {
        this.bidiFlag = bidiFlag;
    }

    public boolean getBidiFlag() {
        return bidiFlag;
    }

    /** Splits text into lines no wider than {@code wrapWidth}, carrying the formatting of each line over to the next (1.12 behaviour). */
    public List<String> listFormattedStringToWidth(String str, int wrapWidth) {
        List<String> out = new ArrayList<>();
        if (str == null) return out;
        wrapFormatted(str, wrapWidth, out);
        return out;
    }

    private void wrapFormatted(String str, int wrapWidth, List<String> out) {
        while (true) {
            int i = sizeStringToWidth(str, wrapWidth);
            if (str.length() <= i) {
                out.add(str);
                return;
            }
            String head = str.substring(0, i);
            char c = str.charAt(i);
            boolean space = c == ' ' || c == '\n';
            String tail = getFormatFromString(head) + str.substring(i + (space ? 1 : 0));
            out.add(head);
            if (tail.length() >= str.length() && i == 0) {
                out.add(tail);
                return; // nothing fits: avoid looping forever on a width smaller than one character
            }
            str = tail;
        }
    }

    /** How many characters of {@code str} fit into {@code wrapWidth}, preferring to break at a space or newline. */
    protected int sizeStringToWidth(String str, int wrapWidth) {
        int len = str.length();
        int width = 0;
        int i = 0;
        int lastSpace = -1;
        boolean bold = false;
        for (; i < len; ++i) {
            char c = str.charAt(i);
            switch (c) {
                case '\n' -> {
                    --i;
                }
                case ' ' -> {
                    lastSpace = i;
                    width += getCharWidth(c);
                    if (bold) ++width;
                }
                case 167 -> {
                    if (i < len - 1) {
                        ++i;
                        char f = str.charAt(i);
                        if (f == 'l' || f == 'L') bold = true;
                        else if (f == 'r' || f == 'R' || isFormatColor(f)) bold = false;
                    }
                }
                default -> {
                    width += getCharWidth(c);
                    if (bold) ++width;
                }
            }
            if (c == '\n') {
                ++i;
                lastSpace = i;
                break;
            }
            if (width > wrapWidth) break;
        }
        return i != len && lastSpace != -1 && lastSpace < i ? lastSpace : i;
    }

    private static boolean isFormatColor(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    private static boolean isFormatSpecial(char c) {
        return c >= 'k' && c <= 'o' || c >= 'K' && c <= 'O' || c == 'r' || c == 'R';
    }

    /** The colour and style codes in effect at the end of {@code text}. */
    public static String getFormatFromString(String text) {
        StringBuilder sb = new StringBuilder();
        int i = -1;
        int len = text.length();
        while ((i = text.indexOf(167, i + 1)) != -1) {
            if (i < len - 1) {
                char c = text.charAt(i + 1);
                if (isFormatColor(c)) sb.setLength(0);
                if (isFormatColor(c) || isFormatSpecial(c)) sb.append('§').append(c);
            }
        }
        return sb.toString();
    }

    /** The RGB of a colour code character ({@code 0}-{@code f}). */
    public int getColorCode(char character) {
        int i = "0123456789abcdef".indexOf(Character.toLowerCase(character));
        return i >= 0 && i < 16 ? colorCode[i] : -1;
    }
}
