package mccompat.v1_7_10.net.minecraft.client.renderer.entity;

import mccompat.v1_7_10.net.minecraft.client.gui.FontRenderer;
import mccompat.v1_7_10.net.minecraft.client.renderer.RenderBlocks;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.mccompat.v1_7_10.client.RenderContext;
import org.kylepitcock.mccompat.v1_7_10.client.TextureLocations;

public abstract class Render {
    protected RenderManager renderManager;
    protected RenderBlocks field_147909_c = new RenderBlocks();
    protected float shadowSize;
    protected float shadowOpaque = 1.0F;

    public abstract void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks);

    protected abstract ResourceLocation getEntityTexture(Entity entity);

    protected void bindEntityTexture(Entity entity) {
        bindTexture(getEntityTexture(entity));
    }

    protected void bindTexture(ResourceLocation location) {
        if (location != null) RenderContext.bindTexture(TextureLocations.modern(location.getResourceDomain(), location.getResourcePath()));
    }

    public void setRenderManager(RenderManager manager) {
        this.renderManager = manager;
    }

    public FontRenderer getFontRendererFromRenderManager() {
        return FontRenderer.INSTANCE;
    }
}
