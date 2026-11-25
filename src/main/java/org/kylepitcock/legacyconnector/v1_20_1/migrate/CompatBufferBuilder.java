package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import org.kylepitcock.legacyconnector.core.flags.Flags;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.RenderType;
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

    /** The memory this builder writes into (what {@code RenderBuffers.fixedBuffers} entries hand to the buffer source); null for a tesselator's builder. */
    public ByteBufferBuilder memory() {
        return own;
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

    /**
     * The 1.21.1 builder a mod's {@code ParticleRenderType.begin(builder, textures)} started, handed to the particle engine: the engine builds and draws it
     * itself, so this wrapper lets go of it (a mod's 1.20.1 {@code end} is never called by 1.21.1; keeping it would make the next frame's begin fail with
     * "Already building!").
     */
    public static BufferBuilder handOver(Tesselator tesselator) {
        CompatBufferBuilder b;
        synchronized (CompatBufferBuilder.class) {
            b = OF_TESSELATOR.get(tesselator);
        }
        if (b == null) return null;
        BufferBuilder started = b.real;
        b.real = null;
        return started;
    }

    /** Takes over a builder that 1.21.1 code started ({@code ParticleRenderType.begin(Tesselator, TextureManager)}): a mod's 1.20.1 code then fills and ends it. */
    public void adopt(BufferBuilder started) {
        this.real = started;
    }

    private static final boolean DIAG = Flags.BUFFER_DIAG.on();
    private Throwable beganAt;

    /** {@code BufferBuilder.format} (a public-in-practice field of 1.20.1 that renderers read): the format of the last {@code begin}. */
    public VertexFormat format;
    /** {@code BufferBuilder.defaultColorSet}: 1.20.1 flag for {@code defaultColor}: vertices that write no colour get {@link #defaultR} to {@link #defaultA} at {@link #endVertex}. */
    public boolean defaultColorSet;
    private int defaultR = 255, defaultG = 255, defaultB = 255, defaultA = 255;

    // ---- raw element writers of 1.20.1 (putByte/putShort/putFloat/nextElement) ----------------------------------------------------
    // 1.21.1's builder takes typed vertex calls only, so the raw bytes of one vertex are staged here and decoded into those calls when the last element
    // of the vertex is passed (nextElement) or a whole buffer is handed over (putBulkData(ByteBuffer)). Elements other than position, colour, UV0, overlay,
    // light and normal are dropped (APPROXIMATION).

    /** The opaque sort state of 1.20.1's {@code getSortState()}: this builder keeps none (sorting happens on the mesh in 1.21.1). */
    public static final class SortState {}

    /** {@code getSortState()}: an empty state (quad sorting happens on the finished mesh, see {@link #drawWithRenderType}). */
    public SortState getSortState() {
        return new SortState();
    }

    /** {@code restoreSortState(state)}: nothing to restore. */
    public void restoreSortState(SortState state) {}

    private byte[] raw;
    private int rawElement;
    private int vertexCount;

    private byte[] raw() {
        int size = format.getVertexSize();
        if (raw == null || raw.length != size) raw = new byte[size];
        return raw;
    }

    /** {@code currentElement()}: the element the raw writers address. */
    public com.mojang.blaze3d.vertex.VertexFormatElement currentElement() {
        return format.getElements().get(rawElement);
    }

    /** {@code nextElement()}: moves to the next element of the vertex; after the last one the staged vertex is written. */
    public void nextElement() {
        rawElement++;
        if (rawElement >= format.getElements().size()) {
            rawElement = 0;
            flushRaw(raw());
        }
    }

    /** {@code isCurrentBatchEmpty()}: no vertex was written since {@code begin}. */
    public boolean isCurrentBatchEmpty() {
        return vertexCount == 0;
    }

    private int rawBase(int index) {
        return format.getOffset(currentElement()) + index;
    }

    private java.nio.ByteBuffer rawView() {
        return java.nio.ByteBuffer.wrap(raw()).order(java.nio.ByteOrder.nativeOrder());
    }

    public void putByte(int index, byte value) {
        raw()[rawBase(index)] = value;
    }

    public void putShort(int index, short value) {
        rawView().putShort(rawBase(index), value);
    }

    public void putFloat(int index, float value) {
        rawView().putFloat(rawBase(index), value);
    }

    /** {@code uvShort(u, v, index)}: two shorts (overlay or light coordinates) at {@code index} bytes into the current element. */
    public VertexConsumer uvShort(short u, short v, int index) {
        putShort(index, u);
        putShort(index + 2, v);
        return this;
    }

    /** {@code putBulkData(ByteBuffer)}: whole vertices in this builder's format. */
    public void putBulkData(java.nio.ByteBuffer buffer) {
        int size = format.getVertexSize();
        java.nio.ByteBuffer src = buffer.duplicate().order(java.nio.ByteOrder.nativeOrder());
        byte[] one = new byte[size];
        while (src.remaining() >= size) {
            src.get(one);
            flushRaw(one);
        }
    }

    /** {@code putBulkData(pose, quad, brightness, r, g, b, lights, overlay, readAlpha)} of 1.20.1: 1.21.1 also takes the alpha (opaque here). */
    public void putBulkData(com.mojang.blaze3d.vertex.PoseStack.Pose pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float[] brightness,
            float r, float g, float b, int[] lights, int overlay, boolean readAlpha) {
        b().putBulkData(pose, quad, brightness, r, g, b, 1.0F, lights, overlay, readAlpha);
    }

    private void flushRaw(byte[] vertex) {
        java.nio.ByteBuffer in = java.nio.ByteBuffer.wrap(vertex).order(java.nio.ByteOrder.nativeOrder());
        java.util.List<com.mojang.blaze3d.vertex.VertexFormatElement> elements = format.getElements();
        int pos = -1;
        for (int i = 0; i < elements.size(); i++) if (elements.get(i) == com.mojang.blaze3d.vertex.VertexFormatElement.POSITION) pos = format.getOffset(elements.get(i));
        if (pos < 0) return;
        BufferBuilder out = b();
        out.addVertex(in.getFloat(pos), in.getFloat(pos + 4), in.getFloat(pos + 8));
        vertexCount++;
        for (com.mojang.blaze3d.vertex.VertexFormatElement e : elements) {
            int o = format.getOffset(e);
            if (e == com.mojang.blaze3d.vertex.VertexFormatElement.COLOR) {
                out.setColor(vertex[o] & 0xFF, vertex[o + 1] & 0xFF, vertex[o + 2] & 0xFF, vertex[o + 3] & 0xFF);
            } else if (e == com.mojang.blaze3d.vertex.VertexFormatElement.UV0) {
                out.setUv(in.getFloat(o), in.getFloat(o + 4));
            } else if (e == com.mojang.blaze3d.vertex.VertexFormatElement.UV1) {
                out.setUv1(in.getShort(o), in.getShort(o + 2));
            } else if (e == com.mojang.blaze3d.vertex.VertexFormatElement.UV2) {
                out.setUv2(in.getShort(o), in.getShort(o + 2));
            } else if (e == com.mojang.blaze3d.vertex.VertexFormatElement.NORMAL) {
                out.setNormal(vertex[o] / 127.0F, vertex[o + 1] / 127.0F, vertex[o + 2] / 127.0F);
            }
        }
        java.util.Arrays.fill(vertex, (byte) 0);
    }

    /** {@code putBulkData(pose, quad, r, g, b, light, overlay)} of 1.20.1: 1.21.1 also takes the alpha (opaque here). */
    public void putBulkData(com.mojang.blaze3d.vertex.PoseStack.Pose pose, net.minecraft.client.renderer.block.model.BakedQuad quad, float r, float g, float b, int light, int overlay) {
        putBulkData(pose, quad, r, g, b, 1.0F, light, overlay);
    }

    /**
     * 1.20.1 RenderType.end(BufferBuilder, VertexSorting), adapted to 1.21.1.
     *
     * The modern RenderType owns render-state setup/cleanup and draws MeshData;
     * translucent layers still need their quad indices sorted using the same
     * allocator that produced the mesh. LateEffectBuffers creates its builders
     * with the private-capacity constructor, so that allocator is available here.
     */
    public static void drawWithRenderType(RenderType renderType, CompatBufferBuilder builder, VertexSorting sorting) {
        if (renderType == null || builder == null || sorting == null) {
            throw new IllegalArgumentException("RenderType, builder and vertex sorting must be non-null");
        }

        drawWithRenderType(builder, renderType.sortOnUpload(), sorting, renderType::draw);
    }

    /**
     * Shared mesh-building path used by the RenderType bridge and CPU-only tests.
     * Keeping the draw operation as a callback avoids initializing Minecraft's
     * client RenderType registry from a standalone JUnit test.
     */
    static void drawWithRenderType(CompatBufferBuilder builder, boolean sortOnUpload,
            VertexSorting sorting, java.util.function.Consumer<MeshData> draw) {
        if (builder == null || sorting == null || draw == null) {
            throw new IllegalArgumentException("Builder, vertex sorting and draw callback must be non-null");
        }
        if (sortOnUpload && builder.own == null) {
            throw new IllegalStateException("Cannot sort a RenderType mesh without its original ByteBufferBuilder");
        }

        MeshData mesh = builder.end();
        if (mesh == null) return;
        if (sortOnUpload) mesh.sortQuads(builder.own, sorting);
        // RenderType.draw performs the 1.21.1 setup/draw/clear sequence and
        // transfers MeshData ownership to the renderer, which closes it.
        draw.accept(mesh);
    }

    public void begin(VertexFormat.Mode mode, VertexFormat format) {
        this.format = format;
        this.rawElement = 0;
        this.vertexCount = 0;
        if (real != null) throw new IllegalStateException("Already building!", beganAt);
        if (DIAG) beganAt = new Throwable("previous begin");
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
        vertexCount++;
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

    /**
     * The 1.20.1 end of a vertex. 1.21.1 finishes a vertex with the next {@code addVertex}, and throws "Missing elements in vertex" for a colour that was
     * never written, so the default colour of {@link #defaultColor} is written here: {@code setColor} is ignored when the vertex already has a colour (or
     * the format has none), which is exactly "a colour for vertices that write none".
     */
    public void endVertex() {
        if (defaultColorSet && real != null) real.setColor(defaultR, defaultG, defaultB, defaultA);
    }

    /** {@code BufferBuilder.clear()}: drops what was written so far (the 1.21.1 builder cannot be reset; the memory is reused by the next {@code begin}). */
    public void clear() {
        discard();
    }

    /**
     * {@code BufferBuilder.setQuadSorting(VertexSorting)}: 1.21.1 sorts a translucent mesh when it is built or uploaded, so the sorting is only remembered
     * (APPROXIMATION: the quads of a mod-built translucent batch keep their written order).
     */
    public void setQuadSorting(com.mojang.blaze3d.vertex.VertexSorting sorting) {}

    /** {@code DefaultedVertexConsumer.defaultColor(r, g, b, a)}: a colour for the vertices that write none (applied by {@link #endVertex}). */
    public void defaultColor(int r, int g, int b, int a) {
        defaultR = r;
        defaultG = g;
        defaultB = b;
        defaultA = a;
        defaultColorSet = true;
    }

    public void unsetDefaultColor() {
        defaultColorSet = false;
    }
}
