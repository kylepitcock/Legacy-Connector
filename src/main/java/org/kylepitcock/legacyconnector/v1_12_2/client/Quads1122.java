package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import net.minecraft.core.Direction;

/**
 * Converts baked quads between the legacy layouts (any 1.12.2 vertex format; Forge baked in {@link DefaultVertexFormats#ITEM}) and the
 * modern block format (position, colour, uv, lightmap, normal: 8 ints per vertex). Bytes inside the int array are little-endian, as
 * Forge's pack/unpack treat them.
 */
public final class Quads1122 {
    private static final java.util.Map<net.minecraft.client.renderer.block.model.BakedQuad, BakedQuad> LEGACY =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private Quads1122() {}

    public static Direction direction(EnumFacing facing) {
        return facing == null ? null : Direction.from3DDataValue(facing.getIndex());
    }

    public static EnumFacing facing(Direction direction) {
        return direction == null ? null : EnumFacing.byIndex(direction.get3DDataValue());
    }

    private static int getByte(int[] data, int p) {
        int i = p >> 2;
        return i < data.length ? (data[i] >>> ((p & 3) * 8)) & 0xFF : 0;
    }

    private static int getInt(int[] data, int p) {
        if ((p & 3) == 0) return (p >> 2) < data.length ? data[p >> 2] : 0;
        return getByte(data, p) | getByte(data, p + 1) << 8 | getByte(data, p + 2) << 16 | getByte(data, p + 3) << 24;
    }

    private static int getShort(int[] data, int p) {
        return getByte(data, p) | getByte(data, p + 1) << 8;
    }

    private static float component(int[] data, int at, VertexFormatElement e, int i) {
        if (e == null || i >= e.getElementCount()) return 0;
        int p = at + i * e.getType().getSize();
        return switch (e.getType()) {
            case FLOAT -> Float.intBitsToFloat(getInt(data, p));
            case UBYTE -> getByte(data, p);
            case BYTE -> (byte) getByte(data, p);
            case USHORT -> getShort(data, p);
            case SHORT -> (short) getShort(data, p);
            case UINT, INT -> getInt(data, p);
        };
    }

    /** The modern quad of a legacy one (positions, colour, uvs, lightmap and normal carried over; sprite: its modern sprite). */
    public static net.minecraft.client.renderer.block.model.BakedQuad toModern(BakedQuad quad) {
        if (quad.mcc$modern != null) return quad.mcc$modern;
        int[] data = quad.getVertexData();
        VertexFormat f = quad.getFormat() == null ? DefaultVertexFormats.ITEM : quad.getFormat();
        int stride = f.getSize();
        int pos = f.mcc$offsetOf(VertexFormatElement.EnumUsage.POSITION, 0);
        int color = f.mcc$offsetOf(VertexFormatElement.EnumUsage.COLOR, 0);
        int uv = f.mcc$offsetOf(VertexFormatElement.EnumUsage.UV, 0);
        int lm = f.mcc$offsetOf(VertexFormatElement.EnumUsage.UV, 1);
        int normal = f.mcc$offsetOf(VertexFormatElement.EnumUsage.NORMAL, 0);
        VertexFormatElement posE = f.mcc$elementOf(VertexFormatElement.EnumUsage.POSITION, 0);
        VertexFormatElement colorE = f.mcc$elementOf(VertexFormatElement.EnumUsage.COLOR, 0);
        VertexFormatElement uvE = f.mcc$elementOf(VertexFormatElement.EnumUsage.UV, 0);
        VertexFormatElement normalE = f.mcc$elementOf(VertexFormatElement.EnumUsage.NORMAL, 0);
        int[] out = new int[32];
        int packedNormal = faceNormal(quad.getFace());
        for (int v = 0; v < 4; v++) {
            int base = v * stride;
            int o = v * 8;
            out[o] = Float.floatToRawIntBits(component(data, base + pos, posE, 0));
            out[o + 1] = Float.floatToRawIntBits(component(data, base + pos, posE, 1));
            out[o + 2] = Float.floatToRawIntBits(component(data, base + pos, posE, 2));
            if (color >= 0 && colorE.getType() == VertexFormatElement.EnumType.UBYTE) out[o + 3] = getInt(data, base + color);
            else if (color >= 0) {
                int r = (int) (component(data, base + color, colorE, 0) * 255), g = (int) (component(data, base + color, colorE, 1) * 255);
                int b = (int) (component(data, base + color, colorE, 2) * 255), a = colorE.getElementCount() > 3 ? (int) (component(data, base + color, colorE, 3) * 255) : 255;
                out[o + 3] = (a & 255) << 24 | (b & 255) << 16 | (g & 255) << 8 | (r & 255);
            } else out[o + 3] = -1;
            out[o + 4] = Float.floatToRawIntBits(component(data, base + uv, uvE, 0));
            out[o + 5] = Float.floatToRawIntBits(component(data, base + uv, uvE, 1));
            out[o + 6] = lm >= 0 ? getInt(data, base + lm) : 0;
            if (normal >= 0 && normalE.getType() == VertexFormatElement.EnumType.BYTE) {
                out[o + 7] = getByte(data, base + normal) | getByte(data, base + normal + 1) << 8 | getByte(data, base + normal + 2) << 16;
            } else out[o + 7] = packedNormal;
        }
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = quad.getSprite() == null ? null : quad.getSprite().mcc$modern();
        if (sprite == null) sprite = missingSprite();
        var modern = new net.minecraft.client.renderer.block.model.BakedQuad(out, quad.getTintIndex(), direction(quad.getFace() == null ? EnumFacing.UP : quad.getFace()),
                sprite, quad.shouldApplyDiffuseLighting());
        quad.mcc$modern = modern;
        return modern;
    }

    private static int faceNormal(EnumFacing face) {
        if (face == null) return 127 << 8;
        var d = face.getDirectionVec();
        return ((byte) (d.getX() * 127) & 255) | ((byte) (d.getY() * 127) & 255) << 8 | ((byte) (d.getZ() * 127) & 255) << 16;
    }

    public static net.minecraft.client.renderer.texture.TextureAtlasSprite missingSprite() {
        var atlas = Sprites1122.blockAtlas();
        return atlas == null ? null : atlas.getSprite(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
    }

    /** The legacy quad (item format) of a modern one; cached per modern quad. */
    public static BakedQuad toLegacy(net.minecraft.client.renderer.block.model.BakedQuad quad) {
        BakedQuad cached = LEGACY.get(quad);
        if (cached != null) return cached;
        int[] in = quad.getVertices();
        int[] out = new int[28];
        for (int v = 0; v < 4; v++) {
            int i = v * 8, o = v * 7;
            if (i + 7 >= in.length) break;
            System.arraycopy(in, i, out, o, 6);
            out[o + 6] = in[i + 7];
        }
        BakedQuad legacy = new BakedQuad(out, quad.getTintIndex(), facing(quad.getDirection()), legacySprite(quad.getSprite()), quad.isShade(), DefaultVertexFormats.ITEM);
        legacy.mcc$modern = quad;
        LEGACY.put(quad, legacy);
        return legacy;
    }

    /** The legacy sprite showing a modern sprite (named by its modern id). */
    public static TextureAtlasSprite legacySprite(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        if (sprite == null) return TextureMap.mcc$blocks().getMissingSprite();
        return TextureMap.mcc$blocks().mcc$spriteOf(sprite);
    }
}
