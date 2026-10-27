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

    @Override
    public AABB getRenderBoundingBox(Legacy1122BlockEntity be) {
        var p = be.getBlockPos();
        return new AABB(p.getX() - 1, p.getY() - 1, p.getZ() - 1, p.getX() + 2, p.getY() + 2, p.getZ() + 2);
    }
}
