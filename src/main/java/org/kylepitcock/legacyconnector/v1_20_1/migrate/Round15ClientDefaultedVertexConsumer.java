package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Stand-in for 1.20.1's {@code DefaultedVertexConsumer} (an abstract {@code VertexConsumer} that kept an optional default colour; removed in 1.21.1, where
 * {@code VertexConsumer} is built from {@code addVertex/setColor/setUv/setUv1/setUv2/setNormal}). A mod subclass keeps overriding the old methods
 * ({@code vertex(DDD)}, {@code color(IIII)}, {@code uv}, {@code overlayCoords}, {@code uv2}, {@code normal}, {@code endVertex}); the 1.21.1 methods forward to
 * them. A vertex that the old code never ends explicitly ends when the next one starts (1.20.1 callers always sent {@code endVertex()}; the loader turns that
 * call into {@link #legacyEndVertex()} through {@link VertexCompat#endVertex}).
 * (mc-classes.tsv: {@code com/mojang/blaze3d/vertex/DefaultedVertexConsumer}.)
 */
public abstract class Round15ClientDefaultedVertexConsumer implements VertexConsumer, LegacyEndVertexConsumer {
    protected boolean defaultColorSet;
    protected int defaultR = 255;
    protected int defaultG = 255;
    protected int defaultB = 255;
    protected int defaultA = 255;
    private boolean pendingVertex;

    public void defaultColor(int r, int g, int b, int a) {
        this.defaultR = r;
        this.defaultG = g;
        this.defaultB = b;
        this.defaultA = a;
        this.defaultColorSet = true;
    }

    public void unsetDefaultColor() {
        this.defaultColorSet = false;
    }

    // ---- the 1.20.1 members a mod implements --------------------------------------------------------------------------------------

    public abstract VertexConsumer vertex(double x, double y, double z);

    public abstract VertexConsumer color(int r, int g, int b, int a);

    public abstract VertexConsumer uv(float u, float v);

    public abstract VertexConsumer overlayCoords(int u, int v);

    public abstract VertexConsumer uv2(int u, int v);

    public abstract VertexConsumer normal(float x, float y, float z);

    public abstract void endVertex();

    // ---- the 1.21.1 members the game calls ----------------------------------------------------------------------------------------

    // ---- 1.20.1 default methods a mod reaches with super.xxx(...) -----------------------------------------------------------------

    /** {@code VertexConsumer.vertex(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz)}. */
    public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        VertexCompat.vertex(this, x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz);
    }

    public void putBulkData(com.mojang.blaze3d.vertex.PoseStack.Pose pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float r, float g, float b, int light, int overlay) {
        putBulkData(pose, quad, r, g, b, 1.0F, light, overlay);
    }

    public void putBulkData(com.mojang.blaze3d.vertex.PoseStack.Pose pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float[] brightness, float r, float g, float b,
                            int[] lights, int overlay, boolean readAlpha) {
        putBulkData(pose, quad, brightness, r, g, b, 1.0F, lights, overlay, readAlpha);
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        legacyEndVertex();
        pendingVertex = true;
        return vertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        return color(r, g, b, a);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return uv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return overlayCoords(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return uv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return normal(x, y, z);
    }

    @Override
    public void legacyEndVertex() {
        if (pendingVertex) {
            pendingVertex = false;
            endVertex();
        }
    }
}
