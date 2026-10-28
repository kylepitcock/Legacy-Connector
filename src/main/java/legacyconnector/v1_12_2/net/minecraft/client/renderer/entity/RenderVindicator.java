package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityVindicator;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 vindicator renderer. */
public class RenderVindicator extends RenderLiving<EntityVindicator> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/illager/vindicator.png");

    public RenderVindicator(RenderManager renderManagerIn) {
        super(renderManagerIn, new legacyconnector.v1_12_2.net.minecraft.client.model.ModelIllager(0.0F, 0.0F, 64, 64), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityVindicator entity) {
        return TEXTURE;
    }

    @Override
    protected void preRenderCallback(EntityVindicator entitylivingbaseIn, float partialTickTime) {
        GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
