package org.kylepitcock.legacyconnector.vb1_7_3.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;
import org.kylepitcock.legacyconnector.vb1_7_3.EntityRenderingB173;
import org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173;
import org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

/**
 * Draws a legacy tile entity with the {@code TileEntitySpecialRenderer} its mod registered ({@code ModLoader.RegisterTileEntity(class, id,
 * renderer)}): the original {@code render(tile, x, y, z, partial)} runs against the block's origin and its model parts, texture binds
 * and transforms are replayed natively, as for an original entity renderer.
 */
public final class LegacyB173BlockEntityRendererB173 implements BlockEntityRenderer<LegacyB173BlockEntity> {
    private static final java.util.Set<Class<?>> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public LegacyB173BlockEntityRendererB173(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(LegacyB173BlockEntity host, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var tile = host.legacy();
        if (tile == null || host.getLevel() == null) return;
        if (!(ContentB173.tileEntityRenderer(tile.getClass()) instanceof legacyconnector.vb1_7_3.net.minecraft.client.render.block.entity.BlockEntityRenderer renderer)) return;
        var mod = ContentB173.ownerOf(tile.getClass());
        RenderContext.beginWorld(pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.pushPose();
        try (var readOnly = EntityViewsB173.readOnly()) {
            synchronized (renderer) {
                renderer.setWorld(tile.world);
                try (var scope = RendererContextB173.open(path -> EntityRenderingB173.texture(mod, path), (part, texture) -> {
                    NativeRendererExtrasB173.applyStyle(RendererContextB173.style());
                    int color = RendererContextB173.argb();
                    RenderContext.color(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, ((color >>> 24) & 255) / 255F);
                    RenderContext.bindTexture(texture);
                    ModelPartsB173.draw(part);
                }, NativeRendererExtrasB173.create(host.getLevel(), host.getBlockPos().hashCode(), pose, buffers))) {
                    renderer.render(tile, 0, 0, 0, partial);
                }
            }
        } catch (RuntimeException | LinkageError error) {
            if (REPORTED.add(renderer.getClass()))
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Render").error("Original Beta tile entity renderer failed for {}", tile.getClass().getName(), error);
        } finally {
            pose.popPose();
            RenderContext.end();
        }
    }
}
