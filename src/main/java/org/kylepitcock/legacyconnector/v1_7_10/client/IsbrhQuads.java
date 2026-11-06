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
        int count = capture.size();
        List<BakedQuad> out = new ArrayList<>(count);
        for (int q = 0; q < count; q++) {
            if (degenerate(capture, q)) continue;
            float n0 = capture.normal(q, 0), n1 = capture.normal(q, 1), n2 = capture.normal(q, 2);
            int[] data = new int[32];
            // A colour the renderer set (Thaumcraft's aspect tint on infused stone, a colorMultiplier) was the final colour in
            // 1.7.10. Uncoloured quads stay white and get modern shading and AO. A single colour on a face lying along an axis
            // had 1.7.10's face shade in it (0.5 down, 1 up, 0.8 north/south, 0.6 east/west): that is divided out and the
            // modern pipeline shades it again, with AO, as 1.7.10 did with smooth lighting on. Any other single colour keeps
            // its shade and gets AO; corners of different colours were lit by the renderer itself and are kept as drawn.
            boolean coloured = false;
            for (int i = 0; i < 4; i++) coloured |= capture.ownColor(q, i);
            Direction dir = Direction.getNearest(n0, n1, n2);
            long c0 = (long) capture.get(q, 0, 5);
            boolean uniform = coloured && c0 == (long) capture.get(q, 1, 5) && c0 == (long) capture.get(q, 2, 5) && c0 == (long) capture.get(q, 3, 5);
            float unshade = 1.0F;
            if (uniform && Math.abs(n0 * dir.getStepX() + n1 * dir.getStepY() + n2 * dir.getStepZ()) > 0.999F) {
                float shade = legacyShade(dir);
                int argb = (int) c0;
                float max = Math.max((argb >> 16 & 255), Math.max((argb >> 8 & 255), (argb & 255))) / 255.0F;
                if (max <= shade + 0.01F) unshade = 1.0F / shade;
            }
            int packedNormal = pack(n0, n1, n2);
            for (int i = 0; i < 4; i++) {
                int o = i * 8;
                long argb = (long) capture.get(q, i, 5);
                data[o] = Float.floatToRawIntBits((float) (capture.get(q, i, 0) - bx));
                data[o + 1] = Float.floatToRawIntBits((float) (capture.get(q, i, 1) - by));
                data[o + 2] = Float.floatToRawIntBits((float) (capture.get(q, i, 2) - bz));
                data[o + 3] = coloured ? abgr(unshade == 1.0F ? argb : scale((int) argb, unshade)) : 0xFFFFFFFF;
                data[o + 4] = Float.floatToRawIntBits((float) capture.get(q, i, 3));
                data[o + 5] = Float.floatToRawIntBits((float) capture.get(q, i, 4));
                data[o + 6] = emissive(capture, q, i);
                data[o + 7] = packedNormal;
            }
            if (!coloured) out.add(new BakedQuad(data, -1, dir, sprite, true));
            else out.add(new BakedQuad(data, -1, dir, sprite, unshade != 1.0F, uniform));
        }
        return out;
    }

    /**
     * A quad without area (all corners on one point or one line). DragonAPI's block renderers end every
     * {@code renderWorldBlock} with four {@code addVertex(0, 0, 0)} (ChromatiCraft's CrystallineStoneRenderer, RuneRenderer and
     * the rest), so 1.7.10's chunk pass was never empty: baked, that is a zero-size quad at the world origin with whatever UV the
     * thread's Tessellator held last, in the cut-out and the translucent layer of every such block. It draws nothing, but it gave
     * every one of those sections translucent geometry thousands of blocks away (Sodium sorts it every frame) and named a random
     * sprite in the render log. Such quads are dropped.
     */
    static boolean degenerate(RenderContext.Capture capture, int q) {
        double ax = capture.get(q, 2, 0) - capture.get(q, 0, 0), ay = capture.get(q, 2, 1) - capture.get(q, 0, 1), az = capture.get(q, 2, 2) - capture.get(q, 0, 2);
        double bx = capture.get(q, 3, 0) - capture.get(q, 1, 0), by = capture.get(q, 3, 1) - capture.get(q, 1, 1), bz = capture.get(q, 3, 2) - capture.get(q, 1, 2);
        double cx = ay * bz - az * by, cy = az * bx - ax * bz, cz = ax * by - ay * bx;
        return cx * cx + cy * cy + cz * cz < 1.0E-12D;
    }

    /**
     * The baked light of a corner: the brightness the renderer set itself ({@code Tessellator.setBrightness}) when it is full block
     * light (or at least block light 10, a dimmer glow), as glowing overlays are drawn in 1.7.10 ({@code setBrightness(240)}: ChromatiCraft's pylon structure runes, glowing ore
     * layers), else 0. The modern pipeline lights a corner with the larger of the baked and the world light, so those parts glow
     * in the dark as they did instead of taking the world's light; the world brightness legacy RenderBlocks sets for ordinary faces is
     * left to the modern lighting (and its ambient occlusion).
     */
    private static int emissive(RenderContext.Capture capture, int q, int i) {
        if (capture.get(q, i, 8) == 0.0D) return 0;
        int light = (int) capture.get(q, i, 6);
        if ((light & 0xFFFF) >= 0xF0) return light;
        // a glow short of full (Thaumcraft's ore shards: setBrightness(max(world, 160))): its block light only, never the sky's (the time of day is the world's)
        return (light & 0xFFFF) >= 0xA0 ? light & 0xFFFF : 0;
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

    private static int pack(float x, float y, float z) {
        return (Mth.clamp((int) (x * 127.0F), -127, 127) & 255) | (Mth.clamp((int) (y * 127.0F), -127, 127) & 255) << 8
                | (Mth.clamp((int) (z * 127.0F), -127, 127) & 255) << 16;
    }

    /** One line for the log: where the first captured quad's UVs land in the block atlas (a diagnostic for blocks that draw black or nothing). */
    static String describe(RenderContext.Capture capture) {
        int first = 0;
        while (first < capture.size() && degenerate(capture, first)) first++;
        if (first >= capture.size()) return capture.isEmpty() ? "no quads" : "only zero-size quads";
        double u = 0, v = 0;
        StringBuilder sb = new StringBuilder("first quad uv");
        for (int i = 0; i < 4; i++) {
            u += capture.get(first, i, 3) / 4;
            v += capture.get(first, i, 4) / 4;
            sb.append(String.format(" (%.4f,%.4f)", capture.get(first, i, 3), capture.get(first, i, 4)));
        }
        try {
            var atlas = net.minecraft.client.Minecraft.getInstance().getModelManager().getAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            String found = null, onEdge = null;
            java.lang.reflect.Field f = net.minecraft.client.renderer.texture.TextureAtlas.class.getDeclaredField("texturesByName");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<net.minecraft.resources.ResourceLocation, TextureAtlasSprite> sprites = (java.util.Map<net.minecraft.resources.ResourceLocation, TextureAtlasSprite>) f.get(atlas);
            for (var e : sprites.entrySet()) {
                TextureAtlasSprite s = e.getValue();
                // strictly inside: a quad sampling a texel row or column shared by two sprites (a single-point quad on a sprite's edge,
                // as Reika's pipes draw the middle of the glass icon) would otherwise be named after whichever neighbour comes first
                if (u > s.getU0() && u < s.getU1() && v > s.getV0() && v < s.getV1()) {
                    found = e.getKey().toString();
                    break;
                }
                if (onEdge == null && u >= s.getU0() && u <= s.getU1() && v >= s.getV0() && v <= s.getV1()) onEdge = e.getKey().toString();
            }
            if (found == null) found = onEdge == null ? null : onEdge + " (on its edge)";
            sb.append(" -> ").append(found == null ? "OUTSIDE every sprite of the block atlas" : "sprite " + found);
        } catch (ReflectiveOperationException | RuntimeException e) {
            sb.append(" (atlas lookup failed: ").append(e).append(")");
        }
        return sb.toString();
    }
}
