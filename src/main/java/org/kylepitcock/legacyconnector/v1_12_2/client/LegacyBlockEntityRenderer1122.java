package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.mojang.blaze3d.vertex.PoseStack;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.OpenGlHelper;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.AABB;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Draws a hosted 1.12.2 tile entity with the TileEntitySpecialRenderer bound to its class: the legacy renderer runs inside a world
 * render context at the block's position (its x, y, z are 0), with the block's light as the lightmap coordinates vanilla set.
 */
public final class LegacyBlockEntityRenderer1122 implements BlockEntityRenderer<Legacy1122BlockEntity> {
    @Override
    public void render(Legacy1122BlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        TileEntity te = be.legacy();
        if (te == null) return;
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer<TileEntity> renderer = dispatcher.getRenderer(te);
        if (renderer == null) return;
        if (dispatcher.renderEngine == null) dispatcher.renderEngine = TextureManager.mcc$instance();
        if (dispatcher.world != te.getWorld()) dispatcher.setWorld(te.getWorld());
        RenderContext.beginWorld(pose, buffers, light, overlay);
        try {
            Tessellator.mcc$reset();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >>> 16);
            dispatcher.render(te, 0.0D, 0.0D, 0.0D, partialTick, -1, 1.0F);
        } finally {
            Tessellator.mcc$reset();
            RenderContext.end();
        }
    }

    /** Legacy renderers may draw well outside their block (vanilla checked TileEntity.getRenderBoundingBox, infinite for many). */
    @Override
    public boolean shouldRenderOffScreen(Legacy1122BlockEntity be) {
        TileEntity te = be.legacy();
        if (te == null) return false;
        TileEntitySpecialRenderer<TileEntity> renderer = TileEntityRendererDispatcher.instance.getRenderer(te);
        return renderer != null && renderer.isGlobalRenderer(te);
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    /**
     * 1.12.2's distance cull (TileEntityRendererDispatcher.render): drawn while {@code getDistanceSq(camera)} is below
     * {@code getMaxRenderDistanceSquared()}, 64 blocks unless the tile entity says otherwise.
     */
    @Override
    public boolean shouldRender(Legacy1122BlockEntity be, net.minecraft.world.phys.Vec3 camera) {
        TileEntity te = be.legacy();
        if (te == null) return false;
        if (org.kylepitcock.legacyconnector.core.VirtualLevels.isVirtual(be.getLevel())) return BlockEntityRenderer.super.shouldRender(be, camera);
        try {
            return te.getDistanceSq(camera.x, camera.y, camera.z) < te.getMaxRenderDistanceSquared();
        } catch (RuntimeException | LinkageError e) {
            return BlockEntityRenderer.super.shouldRender(be, camera);
        }
    }

    /**
     * The frustum cull box: the block and its neighbours (renderers draw a little outside their block) together with the tile
     * entity's own {@code getRenderBoundingBox()}, which vanilla 1.12.2 culled with (infinite for beacons and big multiblock renderers).
     */
    @Override
    public AABB getRenderBoundingBox(Legacy1122BlockEntity be) {
        var p = be.getBlockPos();
        AABB around = new AABB(p.getX() - 1, p.getY() - 1, p.getZ() - 1, p.getX() + 2, p.getY() + 2, p.getZ() + 2);
        TileEntity te = be.legacy();
        if (te == null || org.kylepitcock.legacyconnector.core.VirtualLevels.isVirtual(be.getLevel())) return around;
        try {
            var box = te.getRenderBoundingBox();
            if (box == null) return around;
            if (Double.isInfinite(box.minX) || Double.isInfinite(box.minY) || Double.isInfinite(box.minZ) || Double.isInfinite(box.maxX)
                    || Double.isInfinite(box.maxY) || Double.isInfinite(box.maxZ)) return AABB.INFINITE;
            return around.minmax(new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
        } catch (RuntimeException | LinkageError e) {
            return around;
        }
    }
}
