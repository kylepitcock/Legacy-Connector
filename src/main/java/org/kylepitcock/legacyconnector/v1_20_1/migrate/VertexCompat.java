package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The 1.20.1 {@link VertexConsumer} calls ({@code vertex(..).color(..).uv(..).endVertex()}) on a 1.21.1 consumer ({@code addVertex}/{@code setColor}/...,
 * the vertex is finished by the next {@code addVertex} or by building). The loader redirects every call on a {@code VertexConsumer} here, receiver first.
 */
public final class VertexCompat {
    private VertexCompat() {}

    public static VertexConsumer vertex(VertexConsumer c, double x, double y, double z) {
        return c.addVertex((float) x, (float) y, (float) z);
    }

    public static VertexConsumer vertex(VertexConsumer c, Matrix4f pose, float x, float y, float z) {
        return c.addVertex(pose, x, y, z);
    }

    public static VertexConsumer color(VertexConsumer c, int r, int g, int b, int a) {
        return c.setColor(r, g, b, a);
    }

    public static VertexConsumer color(VertexConsumer c, float r, float g, float b, float a) {
        return c.setColor(r, g, b, a);
    }

    public static VertexConsumer color(VertexConsumer c, int argb) {
        return c.setColor(argb);
    }

    public static VertexConsumer uv(VertexConsumer c, float u, float v) {
        return c.setUv(u, v);
    }

    public static VertexConsumer uv2(VertexConsumer c, int u, int v) {
        return c.setUv2(u, v);
    }

    public static VertexConsumer uv2(VertexConsumer c, int packedLight) {
        return c.setLight(packedLight);
    }

    public static VertexConsumer overlayCoords(VertexConsumer c, int u, int v) {
        return c.setUv1(u, v);
    }

    public static VertexConsumer overlayCoords(VertexConsumer c, int packedOverlay) {
        return c.setOverlay(packedOverlay);
    }

    public static VertexConsumer normal(VertexConsumer c, float x, float y, float z) {
        return c.setNormal(x, y, z);
    }

    public static VertexConsumer normal(VertexConsumer c, Matrix3f normalMatrix, float x, float y, float z) {
        Vector3f v = normalMatrix.transform(x, y, z, new Vector3f());
        return c.setNormal(v.x(), v.y(), v.z());
    }

    /** A vertex is complete once the next one starts: nothing to do, except for the quad baker that emits a quad after its fourth. */
    public static void endVertex(VertexConsumer c) {
        if (c instanceof LegacyQuadBakingVertexConsumer q) q.endLegacyVertex();
    }

    /** {@code VertexConsumer.vertex(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz)} (a whole vertex in one call): {@code addVertex} with a packed colour. */
    public static void vertex(VertexConsumer c, float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        c.addVertex(x, y, z, net.minecraft.util.FastColor.ARGB32.colorFromFloat(a, r, g, b), u, v, overlay, light, nx, ny, nz);
    }
}
