package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;

/**
 * Client-side helpers of gap round 9 (1.20.1 mods on 1.21.1). Reached from {@code mc-calls.tsv}, {@code mc-fields.tsv} and {@code mc-bridges.tsv}.
 */
public final class Round9ClientCompat {
    private Round9ClientCompat() {}

    private static RenderStateShard.ShaderStateShard positionColorTex;

    /**
     * {@code RenderStateShard.POSITION_COLOR_TEX_SHADER}: 1.21.1 dropped the {@code position_color_tex} shader. The layer already lays the
     * {@code POSITION_COLOR_TEX} vertex format out as {@code POSITION_TEX_COLOR}, so the shader of that layout stands in (APPROXIMATION: same fields, other order).
     */
    public static synchronized RenderStateShard.ShaderStateShard positionColorTexShader() {
        if (positionColorTex == null) positionColorTex = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);
        return positionColorTex;
    }

    /**
     * {@code Toast.TEXTURE} ({@code textures/gui/toasts.png}): 1.21.1 draws toasts from the {@code toast/*} sprites of the GUI atlas and no longer ships the
     * sheet. APPROXIMATION: the old location is answered, a mod that blits it gets the missing texture, nothing fails to link or throws.
     */
    public static net.minecraft.resources.ResourceLocation toastTexture() {
        return net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/gui/toasts.png");
    }

    /** {@code ChunkBufferBuilderPack.builder(RenderType)}: the section pack hands out byte buffers now; the builder writes into the layer's own buffer. */
    public static CompatBufferBuilder packBuilder(SectionBufferBuilderPack pack, RenderType type) {
        return CompatBufferBuilder.over(pack.buffer(type));
    }

    /** The 1.21.1 {@code RenderType.draw(MeshData)} seen by a mod's 1.20.1 {@code end(BufferBuilder, VertexSorting)} override: the builder that holds the mesh. */
    public static CompatBufferBuilder builderOf(MeshData mesh) {
        return CompatBufferBuilder.ofMesh(mesh);
    }

    /** The sorting argument of the same call: 1.21.1 sorts when the mesh is built, so the sorting is only a hint here (distance to the origin). */
    public static VertexSorting sortingOf(MeshData mesh) {
        return VertexSorting.DISTANCE_TO_ORIGIN;
    }

    /**
     * {@code super.end(builder, sorting)} of a mod's {@code RenderType} subclass: the vanilla draw of the finished builder (set up the state, draw the mesh,
     * clear the state). It does not go through {@code draw(MeshData)}: the bridge of the mod's override lives there and would call the override again.
     */
    public static void end(RenderType type, CompatBufferBuilder builder, VertexSorting sorting) {
        if (!builder.isBuilding()) return;
        MeshData mesh = builder.end();
        if (mesh == null) return;
        type.setupRenderState();
        BufferUploader.drawWithShader(mesh);
        type.clearRenderState();
    }
}
