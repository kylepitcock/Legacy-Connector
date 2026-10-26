package legacyconnector.v1_7_10.net.minecraft.client.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManagerReloadListener;
import legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Legacy font renderer. The game's own instance ({@link #INSTANCE}, {@code mc.fontRenderer}) is a view over the modern
 * {@link Font} and draws into the active GUI render context. Instances that mods construct themselves (custom fonts:
 * CoFH's, Immersive Engineering's nixie font, BuildCraft's package font) are 1.7.10's bitmap font renderer: glyphs of
 * the font texture and the unicode pages drawn through the GL emulation, with {@code renderDefaultChar},
 * {@code renderUnicodeChar}, {@code doDraw}, {@code setColor} and {@code bindTexture} as overridable hooks.
 */
public class FontRenderer implements IResourceManagerReloadListener {
    public static final FontRenderer INSTANCE = new FontRenderer();
    /** The characters of the 16x16 default font sheet, in sheet order (1.7.10's table). */
    private static final String CHARS = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000";
    private static final ResourceLocation[] unicodePageLocations = new ResourceLocation[256];

    public int FONT_HEIGHT = 9;
    public Random fontRandom = new Random();
    /** The 32 legacy chat colours (16 and their shadows); mods read them, e.g. to colour their own text. */
    public int[] colorCode = legacyColors(false);
    protected int[] charWidth = new int[256];
    protected byte[] glyphWidth = new byte[65536];
    protected ResourceLocation locationFontTexture;
    protected TextureManager renderEngine;
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
    /** True for a bitmap font renderer a mod constructed; false for the game's modern-font view. */
    private final boolean mcc$bitmap;
    private boolean mcc$fontLoaded;

    /** The game's font renderer: the modern font. */
    public FontRenderer() {
        this.mcc$bitmap = false;
    }

    public FontRenderer(GameSettings settings, ResourceLocation fontTexture, TextureManager textures, boolean unicode) {
        this.mcc$bitmap = true;
        this.locationFontTexture = fontTexture;
        this.renderEngine = textures != null ? textures : TextureManager.INSTANCE;
        this.unicodeFlag = unicode;
        this.colorCode = legacyColors(settings != null && settings.anaglyph);
        readGlyphSizes();
    }

    private static int[] legacyColors(boolean anaglyph) {
        int[] out = new int[32];
        for (int i = 0; i < 32; i++) {
            int j = (i >> 3 & 1) * 85;
            int r = (i >> 2 & 1) * 170 + j;
            int g = (i >> 1 & 1) * 170 + j;
            int b = (i & 1) * 170 + j;
            if (i == 6) r += 85;
            if (anaglyph) {
                int ar = (r * 30 + g * 59 + b * 11) / 100;
                int ag = (r * 30 + g * 70) / 100;
                int ab = (r * 30 + b * 70) / 100;
                r = ar;
                g = ag;
                b = ab;
            }
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

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        if (mcc$bitmap) readFontTexture();
    }

    /** The bitmap font's glyph widths, read lazily for fonts no mod registered as a reload listener. */
    private void mcc$ensureFont() {
        if (!mcc$fontLoaded && mcc$bitmap) readFontTexture();
    }

    private void readFontTexture() {
        mcc$fontLoaded = true;
        BufferedImage image;
        try (InputStream in = getResourceInputStream(locationFontTexture)) {
            image = ImageIO.read(in);
        } catch (IOException | RuntimeException e) {
            return;
        }
        if (image == null) return;
        int w = image.getWidth(), h = image.getHeight();
        int[] pixels = new int[w * h];
        image.getRGB(0, 0, w, h, pixels, 0, w);
        int cellH = h / 16, cellW = w / 16;
        float scale = 8.0F / cellW;
        for (int i = 0; i < 256; ++i) {
            int col = i % 16, row = i / 16;
            int x;
            for (x = cellW - 1; x >= 0; --x) {
                int px = col * cellW + x;
                boolean blank = true;
                for (int y = 0; y < cellH && blank; ++y) {
                    int idx = (row * cellH + y) * w;
                    if ((pixels[px + idx] >> 24 & 255) != 0) blank = false;
                }
                if (!blank) break;
            }
            ++x;
            charWidth[i] = (int) (0.5D + x * scale) + 1;
        }
        charWidth[32] = 4;
    }

    private void readGlyphSizes() {
        try (InputStream in = getResourceInputStream(new ResourceLocation("font/glyph_sizes.bin"))) {
            in.read(glyphWidth);
        } catch (IOException | RuntimeException e) {
            byte[] sizes = org.kylepitcock.legacyconnector.v1_7_10.client.LegacyFontPages.glyphSizes();
            if (sizes != null) System.arraycopy(sizes, 0, glyphWidth, 0, Math.min(sizes.length, glyphWidth.length));
        }
    }

    private float renderCharAtPos(int index, char c, boolean italic) {
        return c == ' ' ? 4.0F : (index >= 0 && CHARS.indexOf(c) != -1 && !unicodeFlag ? renderDefaultChar(index, italic) : renderUnicodeChar(c, italic));
    }

    /** One glyph of the default font sheet (16x16 cells of 8 pixels) at the pen position. */
    protected float renderDefaultChar(int index, boolean italic) {
        float u = index % 16 * 8;
        float v = index / 16 * 8;
        float slant = italic ? 1.0F : 0.0F;
        bindTexture(locationFontTexture);
        float w = charWidth[index] - 0.01F;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glTexCoord2f(u / 128.0F, v / 128.0F);
        GL11.glVertex3f(posX + slant, posY, 0.0F);
        GL11.glTexCoord2f(u / 128.0F, (v + 7.99F) / 128.0F);
        GL11.glVertex3f(posX - slant, posY + 7.99F, 0.0F);
        GL11.glTexCoord2f((u + w - 1.0F) / 128.0F, v / 128.0F);
        GL11.glVertex3f(posX + w - 1.0F + slant, posY, 0.0F);
        GL11.glTexCoord2f((u + w - 1.0F) / 128.0F, (v + 7.99F) / 128.0F);
        GL11.glVertex3f(posX + w - 1.0F - slant, posY + 7.99F, 0.0F);
        GL11.glEnd();
        return charWidth[index];
    }

    protected ResourceLocation getUnicodePageLocation(int page) {
        if (unicodePageLocations[page] == null) {
            unicodePageLocations[page] = new ResourceLocation(String.format("textures/font/unicode_page_%02x.png", page));
        }
        return unicodePageLocations[page];
    }

    private void loadGlyphTexture(int page) {
        bindTexture(getUnicodePageLocation(page));
    }

    /** One glyph of a unicode page (16x16 cells of 16 pixels, drawn at half size). */
    protected float renderUnicodeChar(char c, boolean italic) {
        if (glyphWidth[c] == 0) return 0.0F;
        loadGlyphTexture(c / 256);
        int start = glyphWidth[c] >>> 4;
        int end = glyphWidth[c] & 15;
        float fs = start;
        float fe = end + 1;
        float u = c % 16 * 16 + fs;
        float v = (c & 255) / 16 * 16;
        float w = fe - fs - 0.02F;
        float slant = italic ? 1.0F : 0.0F;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glTexCoord2f(u / 256.0F, v / 256.0F);
        GL11.glVertex3f(posX + slant, posY, 0.0F);
        GL11.glTexCoord2f(u / 256.0F, (v + 15.98F) / 256.0F);
        GL11.glVertex3f(posX - slant, posY + 7.99F, 0.0F);
        GL11.glTexCoord2f((u + w) / 256.0F, v / 256.0F);
        GL11.glVertex3f(posX + w / 2.0F + slant, posY, 0.0F);
        GL11.glTexCoord2f((u + w) / 256.0F, (v + 15.98F) / 256.0F);
        GL11.glVertex3f(posX + w / 2.0F - slant, posY + 7.99F, 0.0F);
        GL11.glEnd();
        return (fe - fs) / 2.0F + 1.0F;
    }

    public int drawStringWithShadow(String text, int x, int y, int color) {
        return drawString(text, x, y, color, true);
    }

    public int drawString(String text, int x, int y, int color) {
        return drawString(text, x, y, color, false);
    }

    public int drawString(String text, int x, int y, int color, boolean shadow) {
        if (text == null) return 0;
        if (!mcc$bitmap) {
            var gui = RenderContext.gui();
            // Legacy colors had no alpha; 0 alpha meant opaque.
            int argb = (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
            if (gui != null) return gui.drawString(font(), text, x, y, argb, shadow);
            var pose = RenderContext.pose();
            if (pose == null) return x + getStringWidth(text);
            // text in the world (Thaumcraft's aspect amounts over a jar, labels): drawn with the legacy matrix, as 1.7.10
            // did; without depth testing it shows through blocks
            return font().drawInBatch(text, x, y, argb, shadow, pose.last().pose(), RenderContext.buffers(),
                    RenderContext.depthTest() ? Font.DisplayMode.NORMAL : Font.DisplayMode.SEE_THROUGH, 0, RenderContext.light());
        }
        enableAlpha();
        resetStyles();
        int end;
        if (shadow) {
            end = renderString(text, x + 1, y + 1, color, true);
            end = Math.max(end, renderString(text, x, y, color, false));
        } else {
            end = renderString(text, x, y, color, false);
        }
        return end;
    }

    /** Right-to-left text in visual order (Arabic shaping, bidi reordering), as 1.7.10 did for bidi languages. */
    protected String bidiReorder(String text) {
        try {
            com.ibm.icu.text.Bidi bidi = new com.ibm.icu.text.Bidi(new com.ibm.icu.text.ArabicShaping(8).shape(text), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        } catch (com.ibm.icu.text.ArabicShapingException | RuntimeException | LinkageError e) {
            return text;
        }
    }

    protected void resetStyles() {
        randomStyle = false;
        boldStyle = false;
        italicStyle = false;
        underlineStyle = false;
        strikethroughStyle = false;
    }

    private void renderStringAtPos(String text, boolean shadow) {
        mcc$ensureFont();
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == 167 && i + 1 < text.length()) {
                int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
                if (code < 16) {
                    resetStyles();
                    if (code < 0 || code > 15) code = 15;
                    if (shadow) code += 16;
                    int rgb = colorCode[code];
                    textColor = rgb;
                    setColor((rgb >> 16) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F, alpha);
                } else if (code == 16) {
                    randomStyle = true;
                } else if (code == 17) {
                    boldStyle = true;
                } else if (code == 18) {
                    strikethroughStyle = true;
                } else if (code == 19) {
                    underlineStyle = true;
                } else if (code == 20) {
                    italicStyle = true;
                } else if (code == 21) {
                    resetStyles();
                    setColor(red, blue, green, alpha);
                }
                ++i;
                continue;
            }
            int index = CHARS.indexOf(c);
            if (randomStyle && index != -1) {
                int other;
                do {
                    other = fontRandom.nextInt(charWidth.length);
                } while (charWidth[index] != charWidth[other]);
                index = other;
            }
            float off = unicodeFlag ? 0.5F : 1.0F;
            boolean shift = (c == 0 || index == -1 || unicodeFlag) && shadow;
            if (shift) {
                posX -= off;
                posY -= off;
            }
            float advance = renderCharAtPos(index, c, italicStyle);
            if (shift) {
                posX += off;
                posY += off;
            }
            if (boldStyle) {
                posX += off;
                if (shift) {
                    posX -= off;
                    posY -= off;
                }
                renderCharAtPos(index, c, italicStyle);
                posX -= off;
                if (shift) {
                    posX += off;
                    posY += off;
                }
                ++advance;
            }
            doDraw(advance);
        }
    }

    /** Draws the strikethrough/underline of a glyph and advances the pen (Forge's hook). */
    protected void doDraw(float advance) {
        Tessellator t = Tessellator.instance;
        if (strikethroughStyle) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            t.startDrawingQuads();
            t.addVertex(posX, posY + FONT_HEIGHT / 2, 0.0D);
            t.addVertex(posX + advance, posY + FONT_HEIGHT / 2, 0.0D);
            t.addVertex(posX + advance, posY + FONT_HEIGHT / 2 - 1.0F, 0.0D);
            t.addVertex(posX, posY + FONT_HEIGHT / 2 - 1.0F, 0.0D);
            t.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        if (underlineStyle) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            t.startDrawingQuads();
            int lead = underlineStyle ? -1 : 0;
            t.addVertex(posX + lead, posY + FONT_HEIGHT, 0.0D);
            t.addVertex(posX + advance, posY + FONT_HEIGHT, 0.0D);
            t.addVertex(posX + advance, posY + FONT_HEIGHT - 1.0F, 0.0D);
            t.addVertex(posX + lead, posY + FONT_HEIGHT - 1.0F, 0.0D);
            t.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        posX += (int) advance;
    }

    private int renderStringAligned(String text, int x, int y, int width, int color, boolean shadow) {
        if (bidiFlag) x = x + width - getStringWidth(bidiReorder(text));
        return renderString(text, x, y, color, shadow);
    }

    /** Draws one run of text at (x, y) in the colour; returns the pen position after it. */
    protected int renderString(String text, int x, int y, int color, boolean shadow) {
        if (text == null) return 0;
        if (!mcc$bitmap) return drawString(text, x, y, color, shadow);
        if (bidiFlag) text = bidiReorder(text);
        if ((color & 0xFC000000) == 0) color |= 0xFF000000;
        if (shadow) color = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
        red = (color >> 16 & 255) / 255.0F;
        blue = (color >> 8 & 255) / 255.0F;
        green = (color & 255) / 255.0F;
        alpha = (color >> 24 & 255) / 255.0F;
        setColor(red, blue, green, alpha);
        posX = x;
        posY = y;
        renderStringAtPos(text, shadow);
        return (int) posX;
    }

    public int getStringWidth(String text) {
        if (text == null) return 0;
        if (!mcc$bitmap) return font().width(text);
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            int w = getCharWidth(c);
            if (w < 0 && i < text.length() - 1) {
                ++i;
                c = text.charAt(i);
                if (c != 'l' && c != 'L') {
                    if (c == 'r' || c == 'R') bold = false;
                } else {
                    bold = true;
                }
                w = 0;
            }
            width += w;
            if (bold && w > 0) ++width;
        }
        return width;
    }

    public int getCharWidth(char c) {
        if (!mcc$bitmap) return c == 167 ? -1 : font().width(String.valueOf(c));
        mcc$ensureFont();
        if (c == 167) return -1;
        if (c == 32) return 4;
        int index = CHARS.indexOf(c);
        if (c > 0 && index != -1 && !unicodeFlag) return charWidth[index];
        if (glyphWidth[c] != 0) {
            int start = glyphWidth[c] >>> 4;
            int end = glyphWidth[c] & 15;
            if (end > 7) {
                end = 15;
                start = 0;
            }
            ++end;
            return (end - start) / 2 + 1;
        }
        return 0;
    }

    public String trimStringToWidth(String text, int width) {
        return trimStringToWidth(text, width, false);
    }

    public String trimStringToWidth(String text, int width, boolean reverse) {
        if (!mcc$bitmap) {
            if (!reverse) return font().plainSubstrByWidth(text, width);
            String r = new StringBuilder(text).reverse().toString();
            return new StringBuilder(font().plainSubstrByWidth(r, width)).reverse().toString();
        }
        StringBuilder out = new StringBuilder();
        int used = 0;
        int start = reverse ? text.length() - 1 : 0;
        int step = reverse ? -1 : 1;
        boolean format = false, bold = false;
        for (int i = start; i >= 0 && i < text.length() && used < width; i += step) {
            char c = text.charAt(i);
            int w = getCharWidth(c);
            if (format) {
                format = false;
                if (c != 'l' && c != 'L') {
                    if (c == 'r' || c == 'R') bold = false;
                } else {
                    bold = true;
                }
            } else if (w < 0) {
                format = true;
            } else {
                used += w;
                if (bold) ++used;
            }
            if (used > width) break;
            if (reverse) out.insert(0, c);
            else out.append(c);
        }
        return out.toString();
    }

    private static String trimStringNewline(String text) {
        while (text != null && text.endsWith("\n")) text = text.substring(0, text.length() - 1);
        return text;
    }

    public void drawSplitString(String text, int x, int y, int width, int color) {
        if (!mcc$bitmap) {
            for (Object line : listFormattedStringToWidth(text, width)) {
                drawString((String) line, x, y, color);
                y += FONT_HEIGHT;
            }
            return;
        }
        resetStyles();
        textColor = color;
        text = trimStringNewline(text);
        for (Object line : listFormattedStringToWidth(text, width)) {
            renderStringAligned((String) line, x, y, width, textColor, false);
            y += FONT_HEIGHT;
        }
    }

    public int splitStringWidth(String text, int width) {
        return FONT_HEIGHT * listFormattedStringToWidth(text, width).size();
    }

    public List listFormattedStringToWidth(String text, int width) {
        if (!mcc$bitmap) {
            List<String> out = new ArrayList<>();
            for (var line : font().getSplitter().splitLines(text, width, net.minecraft.network.chat.Style.EMPTY)) out.add(line.getString());
            return out;
        }
        return Arrays.asList(wrapFormattedStringToWidth(text, width).split("\n"));
    }

    public String wrapFormattedStringToWidth(String text, int width) {
        if (!mcc$bitmap) return String.join("\n", listFormattedStringToWidth(text, width));
        int cut = sizeStringToWidth(text, width);
        if (text.length() <= cut) return text;
        String head = text.substring(0, cut);
        char c = text.charAt(cut);
        boolean space = c == ' ' || c == '\n';
        String rest = getFormatFromString(head) + text.substring(cut + (space ? 1 : 0));
        return head + "\n" + wrapFormattedStringToWidth(rest, width);
    }

    private int sizeStringToWidth(String text, int width) {
        int len = text.length();
        int used = 0, i = 0, lastSpace = -1;
        for (boolean bold = false; i < len; ++i) {
            char c = text.charAt(i);
            switch (c) {
                case '\n' -> --i;
                case 167 -> {
                    if (i < len - 1) {
                        ++i;
                        char f = text.charAt(i);
                        if (f != 'l' && f != 'L') {
                            if (f == 'r' || f == 'R' || isFormatColor(f)) bold = false;
                        } else {
                            bold = true;
                        }
                    }
                }
                default -> {
                    if (c == ' ') lastSpace = i;
                    used += getCharWidth(c);
                    if (bold) ++used;
                }
            }
            if (c == '\n') {
                ++i;
                lastSpace = i;
                break;
            }
            if (used > width) break;
        }
        return i != len && lastSpace != -1 && lastSpace < i ? lastSpace : i;
    }

    private static boolean isFormatColor(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    private static boolean isFormatSpecial(char c) {
        return c >= 'k' && c <= 'o' || c >= 'K' && c <= 'O' || c == 'r' || c == 'R';
    }

    /** The colour and style codes in effect at the end of the text (carried onto the next wrapped line). */
    public static String getFormatFromString(String text) {
        String format = "";
        int i = -1;
        int len = text.length();
        while ((i = text.indexOf(167, i + 1)) != -1) {
            if (i < len - 1) {
                char c = text.charAt(i + 1);
                if (isFormatColor(c)) format = "§" + c;
                else if (isFormatSpecial(c)) format = format + "§" + c;
            }
        }
        return format;
    }

    public boolean getUnicodeFlag() {
        return unicodeFlag;
    }

    public void setUnicodeFlag(boolean flag) {
        this.unicodeFlag = flag;
    }

    public boolean getBidiFlag() {
        return bidiFlag;
    }

    public void setBidiFlag(boolean flag) {
        this.bidiFlag = flag;
    }

    /** Forge hooks of the bitmap renderer. */
    protected void setColor(float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
    }

    protected void enableAlpha() {
        GL11.glEnable(GL11.GL_ALPHA_TEST);
    }

    protected void bindTexture(ResourceLocation location) {
        if (location == null) return;
        (renderEngine != null ? renderEngine : TextureManager.INSTANCE).bindTexture(location);
    }

    protected InputStream getResourceInputStream(ResourceLocation location) throws IOException {
        return legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
    }

    static Component literal(String s) {
        return Component.literal(s);
    }
}
