package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** 1.12.2 enderman model: the tall biped (64x32 texture), arms held forward when carrying a block, the head lifted when angry. */
public class ModelEnderman extends ModelBiped {
    public boolean isCarrying;
    public boolean isAttacking;

    public ModelEnderman(float scale) {
        super(0.0F, -14.0F, 64, 32);
        this.bipedHeadwear = new ModelRenderer(this, 0, 16);
        this.bipedHeadwear.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, scale - 0.5F);
        this.bipedHeadwear.setRotationPoint(0.0F, -14.0F, 0.0F);
        this.bipedBody = new ModelRenderer(this, 32, 16);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, scale);
        this.bipedBody.setRotationPoint(0.0F, -14.0F, 0.0F);
        this.bipedRightArm = new ModelRenderer(this, 56, 0);
        this.bipedRightArm.addBox(-1.0F, -2.0F, -1.0F, 2, 30, 2, scale);
        this.bipedRightArm.setRotationPoint(-3.0F, -12.0F, 0.0F);
        this.bipedLeftArm = new ModelRenderer(this, 56, 0);
        this.bipedLeftArm.mirror = true;
        this.bipedLeftArm.addBox(-1.0F, -2.0F, -1.0F, 2, 30, 2, scale);
        this.bipedLeftArm.setRotationPoint(5.0F, -12.0F, 0.0F);
        this.bipedRightLeg = new ModelRenderer(this, 56, 0);
        this.bipedRightLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 30, 2, scale);
        this.bipedRightLeg.setRotationPoint(-2.0F, -2.0F, 0.0F);
        this.bipedLeftLeg = new ModelRenderer(this, 56, 0);
        this.bipedLeftLeg.mirror = true;
        this.bipedLeftLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 30, 2, scale);
        this.bipedLeftLeg.setRotationPoint(2.0F, -2.0F, 0.0F);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        this.bipedHead.showModel = true;
        this.bipedBody.rotateAngleX = 0.0F;
        this.bipedBody.rotationPointY = -14.0F;
        this.bipedBody.rotationPointZ = -0.0F;
        this.bipedRightLeg.rotateAngleX -= 0.0F;
        this.bipedLeftLeg.rotateAngleX -= 0.0F;
        this.bipedRightArm.rotateAngleX = (float) ((double) this.bipedRightArm.rotateAngleX * 0.5D);
        this.bipedLeftArm.rotateAngleX = (float) ((double) this.bipedLeftArm.rotateAngleX * 0.5D);
        this.bipedRightLeg.rotateAngleX = (float) ((double) this.bipedRightLeg.rotateAngleX * 0.5D);
        this.bipedLeftLeg.rotateAngleX = (float) ((double) this.bipedLeftLeg.rotateAngleX * 0.5D);
        float limit = 0.4F;
        this.bipedRightArm.rotateAngleX = clamp(this.bipedRightArm.rotateAngleX, -limit, limit);
        this.bipedLeftArm.rotateAngleX = clamp(this.bipedLeftArm.rotateAngleX, -limit, limit);
        this.bipedRightLeg.rotateAngleX = clamp(this.bipedRightLeg.rotateAngleX, -limit, limit);
        this.bipedLeftLeg.rotateAngleX = clamp(this.bipedLeftLeg.rotateAngleX, -limit, limit);
        if (this.isCarrying) {
            this.bipedRightArm.rotateAngleX = -0.5F;
            this.bipedLeftArm.rotateAngleX = -0.5F;
            this.bipedRightArm.rotateAngleZ = 0.05F;
            this.bipedLeftArm.rotateAngleZ = -0.05F;
        }
        this.bipedRightArm.rotationPointZ = 0.0F;
        this.bipedLeftArm.rotationPointZ = 0.0F;
        this.bipedRightLeg.rotationPointZ = 0.0F;
        this.bipedLeftLeg.rotationPointZ = 0.0F;
        this.bipedRightLeg.rotationPointY = -5.0F;
        this.bipedLeftLeg.rotationPointY = -5.0F;
        this.bipedHead.rotationPointZ = -0.0F;
        this.bipedHead.rotationPointY = -13.0F;
        this.bipedHeadwear.rotationPointX = this.bipedHead.rotationPointX;
        this.bipedHeadwear.rotationPointY = this.bipedHead.rotationPointY;
        this.bipedHeadwear.rotationPointZ = this.bipedHead.rotationPointZ;
        this.bipedHeadwear.rotateAngleX = this.bipedHead.rotateAngleX;
        this.bipedHeadwear.rotateAngleY = this.bipedHead.rotateAngleY;
        this.bipedHeadwear.rotateAngleZ = this.bipedHead.rotateAngleZ;
        if (this.isAttacking) {
            this.bipedHead.rotationPointY -= 5.0F;
            this.bipedHeadwear.rotationPointY -= 5.0F;
        }
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }
}
