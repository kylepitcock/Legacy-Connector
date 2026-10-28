package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;

/** 1.12.2 iron golem model (128x128 texture), boxes as vanilla's: arms swing up to attack and hold out a flower. */
public class ModelIronGolem extends ModelBase {
    public ModelRenderer ironGolemHead;
    public ModelRenderer ironGolemBody;
    public ModelRenderer ironGolemRightArm;
    public ModelRenderer ironGolemLeftArm;
    public ModelRenderer ironGolemLeftLeg;
    public ModelRenderer ironGolemRightLeg;

    public ModelIronGolem() {
        this(0.0F);
    }

    public ModelIronGolem(float p_i1161_1_) {
        this(p_i1161_1_, -7.0F);
    }

    public ModelIronGolem(float p_i46362_1_, float p_i46362_2_) {
        ironGolemHead = new ModelRenderer(this).setTextureSize(128, 128);
        ironGolemHead.setRotationPoint(0.0F, p_i46362_2_, -2.0F);
        ironGolemHead.setTextureOffset(0, 0).addBox(-4.0F, -12.0F, -5.5F, 8, 10, 8, p_i46362_1_);
        ironGolemHead.setTextureOffset(24, 0).addBox(-1.0F, -5.0F, -7.5F, 2, 4, 2, p_i46362_1_);
        ironGolemBody = new ModelRenderer(this).setTextureSize(128, 128);
        ironGolemBody.setRotationPoint(0.0F, p_i46362_2_, 0.0F);
        ironGolemBody.setTextureOffset(0, 40).addBox(-9.0F, -2.0F, -6.0F, 18, 12, 11, p_i46362_1_);
        ironGolemBody.setTextureOffset(0, 70).addBox(-4.5F, 10.0F, -3.0F, 9, 5, 6, p_i46362_1_ + 0.5F);
        ironGolemRightArm = new ModelRenderer(this).setTextureSize(128, 128);
        ironGolemRightArm.setRotationPoint(0.0F, -7.0F, 0.0F);
        ironGolemRightArm.setTextureOffset(60, 21).addBox(-13.0F, -2.5F, -3.0F, 4, 30, 6, p_i46362_1_);
        ironGolemLeftArm = new ModelRenderer(this).setTextureSize(128, 128);
        ironGolemLeftArm.setRotationPoint(0.0F, -7.0F, 0.0F);
        ironGolemLeftArm.setTextureOffset(60, 58).addBox(9.0F, -2.5F, -3.0F, 4, 30, 6, p_i46362_1_);
        ironGolemLeftLeg = new ModelRenderer(this, 0, 22).setTextureSize(128, 128);
        ironGolemLeftLeg.setRotationPoint(-4.0F, 18.0F + p_i46362_2_, 0.0F);
        ironGolemLeftLeg.setTextureOffset(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6, 16, 5, p_i46362_1_);
        ironGolemRightLeg = new ModelRenderer(this, 0, 22).setTextureSize(128, 128);
        ironGolemRightLeg.mirror = true;
        ironGolemRightLeg.setTextureOffset(60, 0).setRotationPoint(5.0F, 18.0F + p_i46362_2_, 0.0F);
        ironGolemRightLeg.addBox(-3.5F, -3.0F, -3.0F, 6, 16, 5, p_i46362_1_);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
        ironGolemHead.render(scale);
        ironGolemBody.render(scale);
        ironGolemLeftLeg.render(scale);
        ironGolemRightLeg.render(scale);
        ironGolemRightArm.render(scale);
        ironGolemLeftArm.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        ironGolemHead.rotateAngleY = netHeadYaw * 0.017453292F;
        ironGolemHead.rotateAngleX = headPitch * 0.017453292F;
        ironGolemLeftLeg.rotateAngleX = -1.5F * triangleWave(limbSwing, 13.0F) * limbSwingAmount;
        ironGolemRightLeg.rotateAngleX = 1.5F * triangleWave(limbSwing, 13.0F) * limbSwingAmount;
        ironGolemLeftLeg.rotateAngleY = 0.0F;
        ironGolemRightLeg.rotateAngleY = 0.0F;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
        var golem = entitylivingbaseIn != null && entitylivingbaseIn.mcc$entity instanceof net.minecraft.world.entity.animal.IronGolem g ? g : null;
        int attack = golem == null ? 0 : golem.getAttackAnimationTick();
        if (attack > 0) {
            ironGolemRightArm.rotateAngleX = -2.0F + 1.5F * triangleWave((float) attack - partialTickTime, 10.0F);
            ironGolemLeftArm.rotateAngleX = -2.0F + 1.5F * triangleWave((float) attack - partialTickTime, 10.0F);
        } else {
            int rose = golem == null ? 0 : golem.getOfferFlowerTick();
            if (rose > 0) {
                ironGolemRightArm.rotateAngleX = -0.8F + 0.025F * triangleWave((float) rose, 70.0F);
                ironGolemLeftArm.rotateAngleX = 0.0F;
            } else {
                ironGolemRightArm.rotateAngleX = (-0.2F + 1.5F * triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
                ironGolemLeftArm.rotateAngleX = (-0.2F - 1.5F * triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
            }
        }
    }

    private float triangleWave(float p_78172_1_, float p_78172_2_) {
        return (Math.abs(p_78172_1_ % p_78172_2_ - p_78172_2_ * 0.5F) - p_78172_2_ * 0.25F) / (p_78172_2_ * 0.25F);
    }
}
