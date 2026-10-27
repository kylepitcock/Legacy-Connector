package legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 tile entity renderer. Bound with ClientRegistry.bindTileEntitySpecialRenderer, it draws its tile entities from the modern
 * block entity renderer of the hosting block entity, with the matrix stack already at the block (x, y, z are 0).
 */
public abstract class TileEntitySpecialRenderer<T extends TileEntity> {
    protected static final ResourceLocation[] DESTROY_STAGES = new ResourceLocation[] {new ResourceLocation("textures/blocks/destroy_stage_0.png"),
            new ResourceLocation("textures/blocks/destroy_stage_1.png"), new ResourceLocation("textures/blocks/destroy_stage_2.png"),
            new ResourceLocation("textures/blocks/destroy_stage_3.png"), new ResourceLocation("textures/blocks/destroy_stage_4.png"),
            new ResourceLocation("textures/blocks/destroy_stage_5.png"), new ResourceLocation("textures/blocks/destroy_stage_6.png"),
            new ResourceLocation("textures/blocks/destroy_stage_7.png"), new ResourceLocation("textures/blocks/destroy_stage_8.png"),
            new ResourceLocation("textures/blocks/destroy_stage_9.png")};
    protected TileEntityRendererDispatcher rendererDispatcher;

    public void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
    }

    public void renderTileEntityFast(T te, double x, double y, double z, float partialTicks, int destroyStage, float partial, BufferBuilder buffer) {
    }

    protected void bindTexture(ResourceLocation location) {
        TextureManager textureManager = rendererDispatcher == null || rendererDispatcher.renderEngine == null ? TextureManager.mcc$instance() : rendererDispatcher.renderEngine;
        if (textureManager != null) textureManager.bindTexture(location);
    }

    protected World getWorld() {
        return rendererDispatcher == null ? null : rendererDispatcher.world;
    }

    public void setRendererDispatcher(TileEntityRendererDispatcher rendererDispatcherIn) {
        rendererDispatcher = rendererDispatcherIn;
    }

    public boolean isGlobalRenderer(T te) {
        return false;
    }

    /** Forge: renderers that only add quads to a shared buffer (drawn in one batch). */
    public boolean hasFastRenderer() {
        return false;
    }
}
