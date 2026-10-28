package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelOcelot;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityOcelot;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 ocelot renderer (tame cats are modern cats, drawn by the game). */
public class RenderOcelot extends RenderLiving<EntityOcelot> {
    private static final ResourceLocation OCELOT_TEXTURES = new ResourceLocation("textures/entity/cat/ocelot.png");

    public RenderOcelot(RenderManager p_i47199_1_) {
        super(p_i47199_1_, new ModelOcelot(), 0.4F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityOcelot entity) {
        return OCELOT_TEXTURES;
    }

    @Override
    protected void preRenderCallback(EntityOcelot entitylivingbaseIn, float partialTickTime) {
        super.preRenderCallback(entitylivingbaseIn, partialTickTime);
        // tame cats were drawn smaller
        if (entitylivingbaseIn.mcc$entity instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame()) GlStateManager.scale(0.8F, 0.8F, 0.8F);
    }
}
