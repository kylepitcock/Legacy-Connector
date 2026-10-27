package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/** 1.12.2's zombie model: the biped with arms held forward (raised further while attacking). */
public class ModelZombie extends ModelBiped {
    public ModelZombie() {
        this(0.0F, false);
    }

    public ModelZombie(float modelSize, boolean halfHeightTexture) {
        super(modelSize, 0.0F, 64, halfHeightTexture ? 32 : 64);
    }

    protected ModelZombie(float modelSize, float yOffset, int textureWidthIn, int textureHeightIn) {
        super(modelSize, yOffset, textureWidthIn, textureHeightIn);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        boolean raised = entityIn != null && entityIn.mcc$entity instanceof net.minecraft.world.entity.Mob mob && mob.isAggressive();
        float f = MathHelper.sin(swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin((1.0F - (1.0F - swingProgress) * (1.0F - swingProgress)) * (float) Math.PI);
        bipedRightArm.rotateAngleZ = 0.0F;
        bipedLeftArm.rotateAngleZ = 0.0F;
        bipedRightArm.rotateAngleY = -(0.1F - f * 0.6F);
        bipedLeftArm.rotateAngleY = 0.1F - f * 0.6F;
        float f2 = -(float) Math.PI / (raised ? 1.5F : 2.25F);
        bipedRightArm.rotateAngleX = f2;
        bipedLeftArm.rotateAngleX = f2;
        bipedRightArm.rotateAngleX += f * 1.2F - f1 * 0.4F;
        bipedLeftArm.rotateAngleX += f * 1.2F - f1 * 0.4F;
        bipedRightArm.rotateAngleZ += MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        bipedLeftArm.rotateAngleZ -= MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        bipedRightArm.rotateAngleX += MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
        bipedLeftArm.rotateAngleX -= MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
    }
}
