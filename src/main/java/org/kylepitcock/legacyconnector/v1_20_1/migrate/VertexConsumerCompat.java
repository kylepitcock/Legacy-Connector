package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
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
        consumer.addVertex(x, y, z, ARGB.colorFromFloat(alpha, red, green, blue),
                u, v, overlay, light, normalX, normalY, normalZ);
    }

    /** The 1.21.1 API submits a vertex when addVertex is called; there is no endVertex step. */
    public static void endVertex(VertexConsumer consumer) {
        // Deliberately empty: addVertex already started the next vertex in the builder.
    }
}
