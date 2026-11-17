package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Map;
import java.util.WeakHashMap;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The 1.20.1 {@code BufferBuilder}: a long-lived object you {@code begin(mode, format)}, fill and {@code end()}. 1.21.1 has no such object (a builder exists
 * from {@code Tesselator.begin} until it is built), so the loader renames the class in mod code to this one, which creates the real builder in
 * {@link #begin} and forwards every vertex call. {@code Tesselator.getBuilder()} and {@code Tesselator.end()} are redirected to {@link #of} and
 * {@link #end(Tesselator)}; {@code BufferBuilder.RenderedBuffer} became {@link MeshData}, so {@code BufferUploader.drawWithShader(builder.end())} links as it is.
 */
public final class CompatBufferBuilder implements VertexConsumer {
    private static final Map<Tesselator, CompatBufferBuilder> OF_TESSELATOR = new WeakHashMap<>();

    private final Tesselator tesselator;
    private ByteBufferBuilder own;
    private BufferBuilder real;

    /** {@code new BufferBuilder(int)}: a private builder with its own memory. */
    public CompatBufferBuilder(int capacity) {
        this.tesselator = null;
        this.own = new ByteBufferBuilder(Math.max(capacity, 256));
    }

    private CompatBufferBuilder(Tesselator tesselator) {
        this.tesselator = tesselator;
    }

    /** The mesh of a finished 1.21.1 draw, seen as a 1.20.1 builder that is "building" until {@link #end()} hands the mesh over (RenderType.end overrides). */
    private MeshData finished;

    private CompatBufferBuilder(ByteBufferBuilder memory) {
        this.tesselator = null;
        this.own = memory;
    }

    /** A builder over memory owned by someone else (a section buffer pack's {@code builder(RenderType)}). */
    public static CompatBufferBuilder over(ByteBufferBuilder memory) {
        return new CompatBufferBuilder(memory);
    }

    /** A builder that has already been filled and ended: {@link #end()} answers {@code mesh} once. */
    public static CompatBufferBuilder ofMesh(MeshData mesh) {
        CompatBufferBuilder b = new CompatBufferBuilder((Tesselator) null);
        b.finished = mesh;
        return b;
    }

    /** {@code Tesselator.getBuilder()}. */
    public static synchronized CompatBufferBuilder of(Tesselator tesselator) {
        return OF_TESSELATOR.computeIfAbsent(tesselator, CompatBufferBuilder::new);
    }

    /** {@code Tesselator.end()}: build and draw with the current shader. */
    public static void end(Tesselator tesselator) {
        MeshData mesh = of(tesselator).end();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    /** The 1.21.1 builder a mod's 1.20.1 {@code ParticleRenderType.begin(builder, textureManager)} started on the tesselator's builder (null when it started none). */
    public static BufferBuilder startedOf(Tesselator tesselator) {
        CompatBufferBuilder b;
        synchronized (CompatBufferBuilder.class) {
            b = OF_TESSELATOR.get(tesselator);
        }
        return b == null ? null : b.real;
    }

    /** Takes over a builder that 1.21.1 code started ({@code ParticleRenderType.begin(Tesselator, TextureManager)}): a mod's 1.20.1 code then fills and ends it. */
    public void adopt(BufferBuilder started) {
        this.real = started;
    }

    public void begin(VertexFormat.Mode mode, VertexFormat format) {
        if (real != null) throw new IllegalStateException("Already building!");
        real = tesselator != null ? tesselator.begin(mode, format) : new BufferBuilder(own, mode, format);
    }

    /** The tesselator this builder belongs to ({@code Tesselator.getBuilder()}), or null for a private builder. */
    public Tesselator tesselator() {
        return tesselator;
    }

    public boolean isBuilding() {
        return real != null || finished != null;
    }

    /** 1.20.1 {@code end()} returned the {@code RenderedBuffer}; null when no vertex was written (1.21.1's {@code build()} contract). */
    public MeshData end() {
        if (finished != null) {
            MeshData m = finished;
            finished = null;
            return m;
        }
        BufferBuilder b = real;
        if (b == null) throw new IllegalStateException("BufferBuilder not started");
        real = null;
        return b.build();
    }

    /** {@code BufferBuilder.endOrDiscardIfEmpty()}: the mesh, or null when nothing was written (1.21.1's {@code build()} answers null for an empty mesh, and frees it). */
    public MeshData endOrDiscardIfEmpty() {
        return real == null && finished == null ? null : end();
    }

    /** {@code BufferBuilder.building()} (the 1.20.1 spelling of {@link #isBuilding}). */
    public boolean building() {
        return isBuilding();
    }

    /** {@code vertex(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz)}: one full vertex (and the 1.20.1 {@code endVertex}, which 1.21.1 does not need). */
    public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        b().addVertex(x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
    }

    public void discard() {
        real = null;
        if (own != null) own.discard();
    }

    private BufferBuilder b() {
        if (real == null) throw new IllegalStateException("BufferBuilder not started");
        return real;
    }

    // ---- 1.21.1 VertexConsumer ------------------------------------------------------------------------------------------------
    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        return b().addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        return b().setColor(r, g, b, a);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return b().setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return b().setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return b().setUv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return b().setNormal(x, y, z);
    }

    // ---- 1.20.1 names ---------------------------------------------------------------------------------------------------------
    public VertexConsumer vertex(double x, double y, double z) {
        return b().addVertex((float) x, (float) y, (float) z);
    }

    public VertexConsumer vertex(Matrix4f pose, float x, float y, float z) {
        return b().addVertex(pose, x, y, z);
    }

    public VertexConsumer color(int r, int g, int b, int a) {
        return b().setColor(r, g, b, a);
    }

    public VertexConsumer color(float r, float g, float b, float a) {
        return b().setColor(r, g, b, a);
    }

    public VertexConsumer color(int argb) {
        return b().setColor(argb);
    }

    public VertexConsumer uv(float u, float v) {
        return b().setUv(u, v);
    }

    public VertexConsumer uv2(int u, int v) {
        return b().setUv2(u, v);
    }

    public VertexConsumer uv2(int light) {
        return b().setLight(light);
    }

    public VertexConsumer overlayCoords(int u, int v) {
        return b().setUv1(u, v);
    }

    public VertexConsumer overlayCoords(int overlay) {
        return b().setOverlay(overlay);
    }

    public VertexConsumer normal(float x, float y, float z) {
        return b().setNormal(x, y, z);
    }

    public VertexConsumer normal(Matrix3f m, float x, float y, float z) {
        Vector3f v = m.transform(x, y, z, new Vector3f());
        return b().setNormal(v.x(), v.y(), v.z());
    }

    public void endVertex() {}

    /** {@code BufferBuilder.clear()}: drops what was written so far (the 1.21.1 builder cannot be reset; the memory is reused by the next {@code begin}). */
    public void clear() {
        discard();
    }

    /**
     * {@code BufferBuilder.setQuadSorting(VertexSorting)}: 1.21.1 sorts a translucent mesh when it is built or uploaded, so the sorting is only remembered
     * (APPROXIMATION: the quads of a mod-built translucent batch keep their written order).
     */
    public void setQuadSorting(com.mojang.blaze3d.vertex.VertexSorting sorting) {}

    /** {@code DefaultedVertexConsumer.defaultColor(r, g, b, a)}: a colour for vertices that write none (APPROXIMATION: ignored; mods set colours explicitly). */
    public void defaultColor(int r, int g, int b, int a) {}

    public void unsetDefaultColor() {}
}
