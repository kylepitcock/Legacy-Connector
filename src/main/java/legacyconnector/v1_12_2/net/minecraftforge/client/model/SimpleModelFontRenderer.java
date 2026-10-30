package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import com.google.common.collect.ImmutableList;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;
import legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

/**
 * Forge: a font renderer that writes text into baked quads instead of drawing it (text on item and block models). Characters of the
 * ASCII sheet become quads textured from {@link #setSprite the font sprite} (the sheet stitched into the block atlas), placed by the
 * model matrix; unicode characters are left to the subclass ({@link #renderUnicodeChar}).
 */
public abstract class SimpleModelFontRenderer extends FontRenderer {
    private static final String ASCII = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞş"
            + "Ŵŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`"
            + "abcdefghijklmnopqrstuvwxyz{|}~\u0000ÇüéâäàåçêëèïîìÄÅ"
            + "ÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑ"
            + "ªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗"
            + "╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤"
            + "╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτ"
            + "ΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²"
            + "■\u0000";

    private float r, g, b, a;
    private final Matrix4f matrix;
    private ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
    private final VertexFormat format;
    private final Vector3f normal = new Vector3f(0, 0, 1);
    private final EnumFacing orientation;
    private boolean fillBlanks;
    private TextureAtlasSprite sprite;

    public SimpleModelFontRenderer(GameSettings settings, ResourceLocation font, TextureManager manager, boolean isUnicode, Matrix4f matrix, VertexFormat format) {
        super(settings, font, manager, isUnicode);
        this.matrix = new Matrix4f(matrix);
        Matrix3f nm = new Matrix3f();
        this.matrix.getRotationScale(nm);
        nm.invert();
        nm.transpose();
        this.format = format;
        nm.transform(normal);
        normal.normalize();
        this.orientation = EnumFacing.getFacingFromVector(normal.x, normal.y, normal.z);
        mcc$fillWidths();
    }

    /** 1.12 read each ASCII glyph's width from the sheet; the game font's widths are the same for that sheet. */
    private void mcc$fillWidths() {
        for (int i = 0; i < 256 && i < ASCII.length(); i++) {
            char c = ASCII.charAt(i);
            charWidth[i] = c == 0 ? 0 : c == ' ' ? 4 : Math.max(0, super.getCharWidth(c));
        }
    }

    public void setSprite(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    public void setFillBlanks(boolean fillBlanks) {
        this.fillBlanks = fillBlanks;
    }

    @Override
    public int drawStringWithShadow(String text, float x, float y, int color) {
        return drawString(text, x, y, color, false);
    }

    @Override
    public int drawString(String text, int x, int y, int color) {
        return drawString(text, (float) x, (float) y, color, false);
    }

    /** Lays the text out character by character (formatting codes recolour and restyle it) and returns the pen position after it. */
    @Override
    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        if (text == null) return 0;
        resetStyles();
        if ((color & 0xFC000000) == 0) color |= 0xFF000000;
        setColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >> 24 & 255) / 255.0F);
        posX = x;
        posY = y;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == 167 && i + 1 < text.length()) {
                int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
                if (code < 16) {
                    resetStyles();
                    if (code < 0) code = 15;
                    int rgb = colorCode[code];
                    setColor((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F, a);
                } else if (code == 17) boldStyle = true;
                else if (code == 18) strikethroughStyle = true;
                else if (code == 19) underlineStyle = true;
                else if (code == 20) italicStyle = true;
                else if (code == 21) {
                    resetStyles();
                    setColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >> 24 & 255) / 255.0F);
                }
                ++i;
                continue;
            }
            int index = ASCII.indexOf(c);
            float advance;
            if (c == ' ' || c == 160) advance = 4.0F;
            else if (index >= 0 && c != 0 && !unicodeFlag) advance = renderDefaultChar(index, italicStyle);
            else advance = renderUnicodeChar(c, italicStyle);
            if (boldStyle) ++advance;
            doDraw(advance);
        }
        return (int) posX;
    }

    @Override
    public int getStringWidth(String text) {
        if (text == null) return 0;
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == 167 && i + 1 < text.length()) {
                char f = Character.toLowerCase(text.charAt(++i));
                if (f == 'l') bold = true;
                else if (f == 'r' || "0123456789abcdef".indexOf(f) >= 0) bold = false;
                continue;
            }
            int w = getCharWidth(c);
            if (w > 0) width += w + (bold ? 1 : 0);
        }
        return width;
    }

    @Override
    public int getCharWidth(char character) {
        if (character == 167) return -1;
        if (character == ' ' || character == 160) return 4;
        int index = ASCII.indexOf(character);
        if (index >= 0 && character != 0 && !unicodeFlag) return charWidth[index];
        return super.getCharWidth(character);
    }

    /** One glyph of the ASCII sheet as a quad at the pen position; returns its advance. */
    @Override
    protected float renderDefaultChar(int pos, boolean italic) {
        if (sprite == null || pos < 0 || pos >= 256) return pos >= 0 && pos < 256 ? charWidth[pos] : 0;
        float x = (pos % 16) / 16.0F;
        float y = (pos / 16) / 16.0F;
        float sh = italic ? 1.0F : 0.0F;
        float w = charWidth[pos] - 1.01F;
        float h = FONT_HEIGHT - 1.01F;
        float wt = w / 128.0F;
        float ht = h / 128.0F;
        UnpackedBakedQuad.Builder quadBuilder = new UnpackedBakedQuad.Builder(format);
        quadBuilder.setTexture(sprite);
        quadBuilder.setQuadOrientation(orientation);
        addVertex(quadBuilder, posX + sh, posY, x, y);
        addVertex(quadBuilder, posX - sh, posY + h, x, y + ht);
        addVertex(quadBuilder, posX + w + sh, posY + h, x + wt, y + ht);
        addVertex(quadBuilder, posX + w - sh, posY, x + wt, y);
        builder.add(quadBuilder.build());
        if (fillBlanks) {
            float cuv = 15.0F / 16.0F;
            quadBuilder = new UnpackedBakedQuad.Builder(format);
            quadBuilder.setTexture(sprite);
            quadBuilder.setQuadOrientation(orientation);
            addVertex(quadBuilder, posX + w + sh, posY, cuv, cuv);
            addVertex(quadBuilder, posX + w - sh, posY + h, cuv, cuv);
            addVertex(quadBuilder, posX + charWidth[pos] + sh, posY + h, cuv, cuv);
            addVertex(quadBuilder, posX + charWidth[pos] - sh, posY, cuv, cuv);
            builder.add(quadBuilder.build());
            quadBuilder = new UnpackedBakedQuad.Builder(format);
            quadBuilder.setTexture(sprite);
            quadBuilder.setQuadOrientation(orientation);
            addVertex(quadBuilder, posX + sh, posY + h, cuv, cuv);
            addVertex(quadBuilder, posX - sh, posY + FONT_HEIGHT, cuv, cuv);
            addVertex(quadBuilder, posX + charWidth[pos] + sh, posY + FONT_HEIGHT, cuv, cuv);
            addVertex(quadBuilder, posX + charWidth[pos] - sh, posY + h, cuv, cuv);
            builder.add(quadBuilder.build());
        }
        return charWidth[pos];
    }

    private final Vector4f vec = new Vector4f();

    private void addVertex(UnpackedBakedQuad.Builder quadBuilder, float x, float y, float u, float v) {
        vec.x = x;
        vec.y = y;
        vec.z = 0;
        vec.w = 1;
        matrix.transform(vec);
        for (int e = 0; e < format.getElementCount(); e++) {
            VertexFormatElement element = format.getElement(e);
            switch (element.getUsage()) {
                case POSITION -> quadBuilder.put(e, vec.x, vec.y, vec.z, vec.w);
                case UV -> {
                    if (element.getIndex() == 0) quadBuilder.put(e, sprite.getInterpolatedU(u * 16), sprite.getInterpolatedV(v * 16), 0, 1);
                    else quadBuilder.put(e);
                }
                case COLOR -> quadBuilder.put(e, r, g, b, a);
                case NORMAL -> quadBuilder.put(e, normal.x, normal.y, normal.z, 1);
                default -> quadBuilder.put(e);
            }
        }
    }

    /** The quads written since the last call. */
    public ImmutableList<BakedQuad> build() {
        ImmutableList<BakedQuad> ret = builder.build();
        builder = ImmutableList.builder();
        return ret;
    }

    @Override
    protected void doDraw(float shift) {
        posX += (int) shift;
    }

    @Override
    protected abstract float renderUnicodeChar(char c, boolean italic);

    @Override
    protected void bindTexture(ResourceLocation location) {
    }

    @Override
    protected void setColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
}
