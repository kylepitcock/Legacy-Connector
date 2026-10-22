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

    static List<BakedQuad> bake(RenderContext.Capture capture, int bx, int by, int bz, TextureAtlasSprite sprite) {
        List<BakedQuad> out = new ArrayList<>(capture.quads.size());
        for (int q = 0; q < capture.quads.size(); q++) {
            double[][] c = capture.quads.get(q);
            float[] n = capture.normals.get(q);
            int[] data = new int[32];
            for (int i = 0; i < 4; i++) {
                double[] v = c[i];
                int o = i * 8;
                data[o] = Float.floatToRawIntBits((float) (v[0] - bx));
                data[o + 1] = Float.floatToRawIntBits((float) (v[1] - by));
                data[o + 2] = Float.floatToRawIntBits((float) (v[2] - bz));
                data[o + 3] = 0xFFFFFFFF;
                data[o + 4] = Float.floatToRawIntBits((float) v[3]);
                data[o + 5] = Float.floatToRawIntBits((float) v[4]);
                data[o + 6] = 0;
                data[o + 7] = pack(n);
            }
            Direction dir = Direction.getNearest(n[0], n[1], n[2]);
            out.add(new BakedQuad(data, -1, dir, sprite, true));
        }
        return out;
    }

    private static int pack(float[] n) {
        return (Mth.clamp((int) (n[0] * 127.0F), -127, 127) & 255) | (Mth.clamp((int) (n[1] * 127.0F), -127, 127) & 255) << 8
                | (Mth.clamp((int) (n[2] * 127.0F), -127, 127) & 255) << 16;
    }
}
