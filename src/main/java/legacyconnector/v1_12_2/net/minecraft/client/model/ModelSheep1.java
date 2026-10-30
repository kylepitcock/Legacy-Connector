package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;

/** 1.12.2 sheep wool (the fleece layer): inflated head, body and upper legs drawn over {@link ModelSheep2}. */
public class ModelSheep1 extends ModelQuadruped {
    private float headRotationAngleX;

    public ModelSheep1() {
        super(12, 0.0F);
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-3.0F, -4.0F, -4.0F, 6, 6, 6, 0.6F);
        head.setRotationPoint(0.0F, 6.0F, -8.0F);
        body = new ModelRenderer(this, 28, 8);
        body.addBox(-4.0F, -10.0F, -7.0F, 8, 16, 6, 1.75F);
        body.setRotationPoint(0.0F, 5.0F, 2.0F);
        leg1 = woolLeg(-3.0F, 7.0F);
        leg2 = woolLeg(3.0F, 7.0F);
        leg3 = woolLeg(-3.0F, -5.0F);
        leg4 = woolLeg(3.0F, -5.0F);
    }

    private ModelRenderer woolLeg(float x, float z) {
        ModelRenderer leg = new ModelRenderer(this, 0, 16);
        leg.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, 0.5F);
        leg.setRotationPoint(x, 12.0F, z);
        return leg;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
        super.setLivingAnimations(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTickTime);
        head.rotationPointY = 6.0F + ModelSheep2.mcc$headRotationPointY(entitylivingbaseIn, partialTickTime) * 9.0F;
        headRotationAngleX = ModelSheep2.mcc$headRotationAngleX(entitylivingbaseIn, partialTickTime);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        head.rotateAngleX = headRotationAngleX;
    }
}
