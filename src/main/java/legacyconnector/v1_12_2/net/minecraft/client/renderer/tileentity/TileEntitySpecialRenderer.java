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

    public legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer getFontRenderer() {
        return legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer.mcc$INSTANCE;
    }

    /** Turns the lightmap off (full-bright drawing: screens, glowing parts) or back on, as 1.12 toggled its texture unit. */
    protected void setLightmapDisabled(boolean disabled) {
        legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.setActiveTexture(legacyconnector.v1_12_2.net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit);
        if (disabled) legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.disableTexture2D();
        else legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.setActiveTexture(legacyconnector.v1_12_2.net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit);
    }

    /** A name plate over the tile entity (1.5 blocks up, centred), facing the camera, within maxDistance of it. */
    protected void drawNameplate(T te, String str, double x, double y, double z, int maxDistance) {
        var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        var cam = camera.getPosition();
        if (te.getDistanceSq(cam.x, cam.y, cam.z) > (double) (maxDistance * maxDistance)) return;
        legacyconnector.v1_12_2.net.minecraft.client.renderer.EntityRenderer.drawNameplate(getFontRenderer(), str, (float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F, 0,
                camera.getYRot(), camera.getXRot(), false, false);
    }

    /** Forge: renderers that only add quads to a shared buffer (drawn in one batch). */
    public boolean hasFastRenderer() {
        return false;
    }
}
