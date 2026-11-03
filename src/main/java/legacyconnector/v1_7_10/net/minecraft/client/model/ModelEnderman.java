package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

/** The enderman: a biped with long thin limbs, whose arms lift when it carries a block and whose head opens when it is angry. */
public class ModelEnderman extends ModelBiped {
    public boolean isCarrying;
    public boolean isAttacking;

    public ModelEnderman() {
        super(0.0F, -14.0F, 64, 32);
        float yOffset = -14.0F;
        bipedHeadwear = new ModelRenderer(this, 0, 16);
        bipedHeadwear.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, -0.5F);
        bipedHeadwear.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
        bipedBody = new ModelRenderer(this, 32, 16);
        bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, 0.0F);
        bipedBody.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
        bipedRightArm = new ModelRenderer(this, 56, 0);
        bipedRightArm.addBox(-1.0F, -2.0F, -1.0F, 2, 30, 2, 0.0F);
        bipedRightArm.setRotationPoint(-3.0F, 2.0F + yOffset, 0.0F);
        bipedLeftArm = new ModelRenderer(this, 56, 0);
        bipedLeftArm.mirror = true;
        bipedLeftArm.addBox(-1.0F, -2.0F, -1.0F, 2, 30, 2, 0.0F);
        bipedLeftArm.setRotationPoint(5.0F, 2.0F + yOffset, 0.0F);
        bipedRightLeg = new ModelRenderer(this, 56, 0);
        bipedRightLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 30, 2, 0.0F);
        bipedRightLeg.setRotationPoint(-2.0F, 12.0F + yOffset, 0.0F);
        bipedLeftLeg = new ModelRenderer(this, 56, 0);
        bipedLeftLeg.mirror = true;
        bipedLeftLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 30, 2, 0.0F);
        bipedLeftLeg.setRotationPoint(2.0F, 12.0F + yOffset, 0.0F);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, entity);
        bipedHead.showModel = true;
        float yOffset = -14.0F;
        bipedBody.rotateAngleX = 0.0F;
        bipedBody.rotationPointY = yOffset;
        bipedBody.rotationPointZ = -0.0F;
        bipedRightLeg.rotateAngleX -= 0.0F;
        bipedLeftLeg.rotateAngleX -= 0.0F;
        bipedRightArm.rotateAngleX = (float) ((double) bipedRightArm.rotateAngleX * 0.5D);
        bipedLeftArm.rotateAngleX = (float) ((double) bipedLeftArm.rotateAngleX * 0.5D);
        bipedRightLeg.rotateAngleX = (float) ((double) bipedRightLeg.rotateAngleX * 0.5D);
        bipedLeftLeg.rotateAngleX = (float) ((double) bipedLeftLeg.rotateAngleX * 0.5D);
        float max = 0.4F;
        bipedRightArm.rotateAngleX = Math.max(-max, Math.min(max, bipedRightArm.rotateAngleX));
        bipedLeftArm.rotateAngleX = Math.max(-max, Math.min(max, bipedLeftArm.rotateAngleX));
        bipedRightLeg.rotateAngleX = Math.max(-max, Math.min(max, bipedRightLeg.rotateAngleX));
        bipedLeftLeg.rotateAngleX = Math.max(-max, Math.min(max, bipedLeftLeg.rotateAngleX));
        if (isCarrying) {
            bipedRightArm.rotateAngleX = -0.5F;
            bipedLeftArm.rotateAngleX = -0.5F;
            bipedRightArm.rotateAngleZ = 0.05F;
            bipedLeftArm.rotateAngleZ = -0.05F;
        }
        bipedRightArm.rotationPointZ = 0.0F;
        bipedLeftArm.rotationPointZ = 0.0F;
        bipedRightLeg.rotationPointZ = 0.0F;
        bipedLeftLeg.rotationPointZ = 0.0F;
        bipedRightLeg.rotationPointY = 9.0F + yOffset;
        bipedLeftLeg.rotationPointY = 9.0F + yOffset;
        bipedHead.rotationPointZ = -0.0F;
        bipedHead.rotationPointY = yOffset + 1.0F;
        bipedHeadwear.rotationPointX = bipedHead.rotationPointX;
        bipedHeadwear.rotationPointY = bipedHead.rotationPointY;
        bipedHeadwear.rotationPointZ = bipedHead.rotationPointZ;
        bipedHeadwear.rotateAngleX = bipedHead.rotateAngleX;
        bipedHeadwear.rotateAngleY = bipedHead.rotateAngleY;
        bipedHeadwear.rotateAngleZ = bipedHead.rotateAngleZ;
        if (isAttacking) {
            float shake = 1.0F;
            bipedHead.rotationPointY -= shake * 5.0F;
        }
    }
}
