package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/** 1.12.2's skeleton model: thin limbs, the bow drawn while attacking, arms forward when attacking without one. */
public class ModelSkeleton extends ModelBiped {
    public ModelSkeleton() {
        this(0.0F, false);
    }

    public ModelSkeleton(float modelSize, boolean fullLimbs) {
        super(modelSize, 0.0F, 64, 32);
        if (!fullLimbs) {
            bipedRightArm = new ModelRenderer(this, 40, 16);
            bipedRightArm.addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2, modelSize);
            bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
            bipedLeftArm = new ModelRenderer(this, 40, 16);
            bipedLeftArm.mirror = true;
            bipedLeftArm.addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2, modelSize);
            bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
            bipedRightLeg = new ModelRenderer(this, 0, 16);
            bipedRightLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2, modelSize);
            bipedRightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);
            bipedLeftLeg = new ModelRenderer(this, 0, 16);
            bipedLeftLeg.mirror = true;
            bipedLeftLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2, modelSize);
            bipedLeftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
        }
    }

    private static boolean attacking(Entity e) {
        return e != null && e.mcc$entity instanceof net.minecraft.world.entity.Mob mob && mob.isAggressive();
    }

    private static boolean holdsBow(Entity e) {
        return e != null && e.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l && l.getMainHandItem().is(net.minecraft.world.item.Items.BOW);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTickTime) {
        rightArmPose = ArmPose.EMPTY;
        leftArmPose = ArmPose.EMPTY;
        if (holdsBow(entity) && attacking(entity)) {
            boolean right = !(entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) || l.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
            if (right) rightArmPose = ArmPose.BOW_AND_ARROW;
            else leftArmPose = ArmPose.BOW_AND_ARROW;
        }
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTickTime);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        if (attacking(entityIn) && !holdsBow(entityIn)) {
            float f = MathHelper.sin(swingProgress * (float) Math.PI);
            float f1 = MathHelper.sin((1.0F - (1.0F - swingProgress) * (1.0F - swingProgress)) * (float) Math.PI);
            bipedRightArm.rotateAngleZ = 0.0F;
            bipedLeftArm.rotateAngleZ = 0.0F;
            bipedRightArm.rotateAngleY = -(0.1F - f * 0.6F);
            bipedLeftArm.rotateAngleY = 0.1F - f * 0.6F;
            bipedRightArm.rotateAngleX = -((float) Math.PI / 2F);
            bipedLeftArm.rotateAngleX = -((float) Math.PI / 2F);
            bipedRightArm.rotateAngleX -= f * 1.2F - f1 * 0.4F;
            bipedLeftArm.rotateAngleX -= f * 1.2F - f1 * 0.4F;
            bipedRightArm.rotateAngleZ += MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
            bipedLeftArm.rotateAngleZ -= MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
            bipedRightArm.rotateAngleX += MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
            bipedLeftArm.rotateAngleX -= MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
        }
    }

    @Override
    public void postRenderArm(float scale, EnumHandSide side) {
        float f = side == EnumHandSide.RIGHT ? 1.0F : -1.0F;
        ModelRenderer arm = getArmForSide(side);
        arm.rotationPointX += f;
        arm.postRender(scale);
        arm.rotationPointX -= f;
    }
}
