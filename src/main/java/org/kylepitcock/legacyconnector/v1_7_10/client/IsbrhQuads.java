package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/** Converts captured legacy quads into baked quads (positions made relative to the block, atlas UVs kept). */
final class IsbrhQuads {
    private IsbrhQuads() {}

    private static final ThreadLocal<RenderContext.Capture> LAST = new ThreadLocal<>();

    /** The description of the capture this thread baked last (read right after the bake, for the one-time log line). */
    static String describeLast() {
        RenderContext.Capture c = LAST.get();
        return c == null ? "no capture" : describe(c);
    }

    static List<BakedQuad> bake(RenderContext.Capture capture, int bx, int by, int bz, TextureAtlasSprite sprite) {
        LAST.set(capture);
        List<BakedQuad> out = new ArrayList<>(capture.quads.size());
        for (int q = 0; q < capture.quads.size(); q++) {
            double[][] c = capture.quads.get(q);
            float[] n = capture.normals.get(q);
            int[] data = new int[32];
            // A colour the renderer set (Thaumcraft's aspect tint on infused stone, a colorMultiplier) was the final colour in
            // 1.7.10. Uncoloured quads stay white and get modern shading and AO. A single colour on a face lying along an axis
            // had 1.7.10's face shade in it (0.5 down, 1 up, 0.8 north/south, 0.6 east/west): that is divided out and the
            // modern pipeline shades it again, with AO, as 1.7.10 did with smooth lighting on. Any other single colour keeps
            // its shade and gets AO; corners of different colours were lit by the renderer itself and are kept as drawn.
            boolean coloured = false;
            for (double[] v : c) coloured |= v.length > 7 && v[7] != 0.0D;
            Direction dir = Direction.getNearest(n[0], n[1], n[2]);
            boolean uniform = coloured && (long) c[0][5] == (long) c[1][5] && (long) c[0][5] == (long) c[2][5] && (long) c[0][5] == (long) c[3][5];
            float unshade = 1.0F;
            if (uniform && Math.abs(n[0] * dir.getStepX() + n[1] * dir.getStepY() + n[2] * dir.getStepZ()) > 0.999F) {
                float shade = legacyShade(dir);
                int argb = (int) (long) c[0][5];
                float max = Math.max((argb >> 16 & 255), Math.max((argb >> 8 & 255), (argb & 255))) / 255.0F;
                if (max <= shade + 0.01F) unshade = 1.0F / shade;
            }
            for (int i = 0; i < 4; i++) {
                double[] v = c[i];
                int o = i * 8;
                data[o] = Float.floatToRawIntBits((float) (v[0] - bx));
                data[o + 1] = Float.floatToRawIntBits((float) (v[1] - by));
                data[o + 2] = Float.floatToRawIntBits((float) (v[2] - bz));
                data[o + 3] = coloured ? abgr(unshade == 1.0F ? (long) v[5] : scale((int) (long) v[5], unshade)) : 0xFFFFFFFF;
                data[o + 4] = Float.floatToRawIntBits((float) v[3]);
                data[o + 5] = Float.floatToRawIntBits((float) v[4]);
                data[o + 6] = 0;
                data[o + 7] = pack(n);
            }
            if (!coloured) out.add(new BakedQuad(data, -1, dir, sprite, true));
            else out.add(new BakedQuad(data, -1, dir, sprite, unshade != 1.0F, uniform));
        }
        return out;
    }

    /** 1.7.10's (and the modern) directional shade of a face. */
    private static float legacyShade(Direction d) {
        return switch (d) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    /** The colour's RGB multiplied by {@code f} (alpha kept, channels capped). */
    private static long scale(int argb, float f) {
        int r = Math.min(255, Math.round((argb >> 16 & 255) * f));
        int g = Math.min(255, Math.round((argb >> 8 & 255) * f));
        int b = Math.min(255, Math.round((argb & 255) * f));
        return argb & 0xFF000000L | r << 16 | g << 8 | b;
    }

    /** Captured ARGB to the vertex format's ABGR. */
    private static int abgr(long argb) {
        int c = (int) argb;
        return c & 0xFF00FF00 | (c & 0xFF) << 16 | (c >> 16) & 0xFF;
    }

    private static int pack(float[] n) {
        return (Mth.clamp((int) (n[0] * 127.0F), -127, 127) & 255) | (Mth.clamp((int) (n[1] * 127.0F), -127, 127) & 255) << 8
                | (Mth.clamp((int) (n[2] * 127.0F), -127, 127) & 255) << 16;
    }

    /** One line for the log: where the first captured quad's UVs land in the block atlas (a diagnostic for blocks that draw black or nothing). */
    static String describe(RenderContext.Capture capture) {
        if (capture.quads.isEmpty()) return "no quads";
        double[][] c = capture.quads.get(0);
        double u = 0, v = 0;
        StringBuilder sb = new StringBuilder("first quad uv");
        for (int i = 0; i < 4; i++) {
            u += c[i][3] / 4;
            v += c[i][4] / 4;
            sb.append(String.format(" (%.4f,%.4f)", c[i][3], c[i][4]));
        }
        try {
            var atlas = net.minecraft.client.Minecraft.getInstance().getModelManager().getAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            String found = null;
            java.lang.reflect.Field f = net.minecraft.client.renderer.texture.TextureAtlas.class.getDeclaredField("texturesByName");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<net.minecraft.resources.ResourceLocation, TextureAtlasSprite> sprites = (java.util.Map<net.minecraft.resources.ResourceLocation, TextureAtlasSprite>) f.get(atlas);
            for (var e : sprites.entrySet()) {
                TextureAtlasSprite s = e.getValue();
                if (u >= s.getU0() && u <= s.getU1() && v >= s.getV0() && v <= s.getV1()) {
                    found = e.getKey().toString();
                    break;
                }
            }
            sb.append(" -> ").append(found == null ? "OUTSIDE every sprite of the block atlas" : "sprite " + found);
        } catch (ReflectiveOperationException | RuntimeException e) {
            sb.append(" (atlas lookup failed: ").append(e).append(")");
        }
        return sb.toString();
    }
}
