package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityWitch;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 witch renderer. */
public class RenderWitch extends RenderLiving<EntityWitch> {
    private static final ResourceLocation WITCH_TEXTURES = new ResourceLocation("textures/entity/witch.png");

    public RenderWitch(RenderManager renderManagerIn) {
        super(renderManagerIn, new legacyconnector.v1_12_2.net.minecraft.client.model.ModelWitch(0.0F), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityWitch entity) {
        return WITCH_TEXTURES;
    }

    @Override
    protected void preRenderCallback(EntityWitch entitylivingbaseIn, float partialTickTime) {
        GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
