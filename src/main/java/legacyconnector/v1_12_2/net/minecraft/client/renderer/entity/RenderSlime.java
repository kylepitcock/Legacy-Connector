package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelSlime;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerSlimeGel;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntitySlime;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 slime renderer: the inner cube, the translucent gel layer, squashing as it hops and scaling with the slime's size. */
public class RenderSlime extends RenderLiving<EntitySlime> {
    private static final ResourceLocation SLIME_TEXTURES = new ResourceLocation("textures/entity/slime/slime.png");

    public RenderSlime(RenderManager p_i47193_1_) {
        super(p_i47193_1_, new ModelSlime(16), 0.25F);
        addLayer(new LayerSlimeGel(this));
    }

    @Override
    public void doRender(EntitySlime entity, double x, double y, double z, float entityYaw, float partialTicks) {
        shadowSize = 0.25F * (float) entity.getSlimeSize();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected void preRenderCallback(EntitySlime entitylivingbaseIn, float partialTickTime) {
        GlStateManager.scale(0.999F, 0.999F, 0.999F);
        float size = (float) entitylivingbaseIn.getSlimeSize();
        float prev = EntitySlime.mcc$get$prevSquishFactor(entitylivingbaseIn);
        float squish = (prev + (EntitySlime.mcc$get$squishFactor(entitylivingbaseIn) - prev) * partialTickTime) / (size * 0.5F + 1.0F);
        float f = 1.0F / (squish + 1.0F);
        GlStateManager.scale(f * size, 1.0F / f * size, f * size);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySlime entity) {
        return SLIME_TEXTURES;
    }
}
