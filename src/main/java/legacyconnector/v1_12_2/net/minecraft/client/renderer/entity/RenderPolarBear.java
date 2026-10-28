package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelPolarBear;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityPolarBear;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 polar bear renderer: the bear model at 1.2 times its size. */
public class RenderPolarBear extends RenderLiving<EntityPolarBear> {
    private static final ResourceLocation POLAR_BEAR_TEXTURE = new ResourceLocation("textures/entity/bear/polarbear.png");

    public RenderPolarBear(RenderManager p_i47197_1_) {
        super(p_i47197_1_, new ModelPolarBear(), 0.7F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPolarBear entity) {
        return POLAR_BEAR_TEXTURE;
    }

    @Override
    protected void preRenderCallback(EntityPolarBear entitylivingbaseIn, float partialTickTime) {
        GlStateManager.scale(1.2F, 1.2F, 1.2F);
        super.preRenderCallback(entitylivingbaseIn, partialTickTime);
    }
}
