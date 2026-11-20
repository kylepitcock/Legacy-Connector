package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Retains the legacy Forge 1.20.1 vertex-emission shapes used by AlienEvo's
 * flat loot-effect renderer on the 1.21.1 fluent VertexConsumer API.
 */
public final class VertexConsumerCompat {
    private VertexConsumerCompat() {}

    /** Legacy double-precision vertex position followed by the old chained color/UV calls. */
    public static VertexConsumer vertex(VertexConsumer consumer, double x, double y, double z) {
        return consumer.addVertex((float) x, (float) y, (float) z);
    }

    /**
     * Legacy full vertex call. The old separate RGBA floats are packed into
     * the 1.21.1 ARGB color parameter before the new full addVertex call.
     */
    public static void addVertex(VertexConsumer consumer,
            float x, float y, float z,
            float red, float green, float blue, float alpha,
            float u, float v, int overlay, int light,
            float normalX, float normalY, float normalZ) {
        int color = (channel(alpha) << 24) | (channel(red) << 16) | (channel(green) << 8) | channel(blue);
        consumer.addVertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
    }

    /** The 1.21.1 API has no endVertex method; preserve only legacy wrappers that still buffer a vertex. */
    public static void endVertex(VertexConsumer consumer) {
        if (consumer instanceof LegacyEndVertexConsumer legacy) legacy.legacyEndVertex();
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, (int) (value * 255.0F)));
    }
}
