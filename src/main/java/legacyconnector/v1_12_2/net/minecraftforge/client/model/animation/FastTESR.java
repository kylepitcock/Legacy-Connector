package legacyconnector.v1_12_2.net.minecraftforge.client.model.animation;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderHelper;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;

/**
 * Forge 1.12.2 fast tile entity renderer: only adds block-format quads (block atlas) to a buffer. The dispatcher draws it through
 * {@link #renderTileEntityFast}; {@link #render} does the same for mods that call it directly.
 */
public abstract class FastTESR<T extends TileEntity> extends TileEntitySpecialRenderer<T> {
    @Override
    public final void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float partial) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? 7425 : 7424);
        buffer.begin(7, DefaultVertexFormats.BLOCK);
        renderTileEntityFast(te, x, y, z, partialTicks, destroyStage, partial, buffer);
        buffer.setTranslation(0.0D, 0.0D, 0.0D);
        tessellator.draw();
        RenderHelper.enableStandardItemLighting();
    }

    @Override
    public final boolean hasFastRenderer() {
        return true;
    }

    @Override
    public abstract void renderTileEntityFast(T te, double x, double y, double z, float partialTicks, int destroyStage, float partial, BufferBuilder buffer);
}
