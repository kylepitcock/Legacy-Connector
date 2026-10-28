package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelIronGolem;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityIronGolem;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 iron golem renderer: the golem model, rocking side to side as it walks. */
public class RenderIronGolem extends RenderLiving<EntityIronGolem> {
    private static final ResourceLocation IRON_GOLEM_TEXTURES = new ResourceLocation("textures/entity/iron_golem.png");

    public RenderIronGolem(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelIronGolem(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityIronGolem entity) {
        return IRON_GOLEM_TEXTURES;
    }

    @Override
    protected void applyRotations(EntityIronGolem entityLiving, float p_77043_2_, float rotationYaw, float partialTicks) {
        super.applyRotations(entityLiving, p_77043_2_, rotationYaw, partialTicks);
        float amount = legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.mcc$get$limbSwingAmount(entityLiving);
        if ((double) amount >= 0.01D) {
            float swing = legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.mcc$get$limbSwing(entityLiving) - amount * (1.0F - partialTicks) + 6.0F;
            float f2 = (Math.abs(swing % 13.0F - 6.5F) - 3.25F) / 3.25F;
            GlStateManager.rotate(6.5F * f2, 0.0F, 0.0F, 1.0F);
        }
    }
}
