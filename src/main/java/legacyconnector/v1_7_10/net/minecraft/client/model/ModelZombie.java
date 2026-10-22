package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;

/** Biped holding its arms straight out (zombies). */
public class ModelZombie extends ModelBiped {
    public ModelZombie() {
        this(0.0F, false);
    }

    protected ModelZombie(float grow, float yOffset, int textureWidth, int textureHeight) {
        super(grow, yOffset, textureWidth, textureHeight);
    }

    public ModelZombie(float grow, boolean tall) {
        super(grow, 0.0F, 64, tall ? 32 : 64);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, entity);
        final float pi = (float) Math.PI;
        float a = MathHelper.sin(onGround * pi);
        float b = MathHelper.sin((1.0F - (1.0F - onGround) * (1.0F - onGround)) * pi);
        bipedRightArm.rotateAngleZ = 0.0F;
        bipedLeftArm.rotateAngleZ = 0.0F;
        bipedRightArm.rotateAngleY = -(0.1F - a * 0.6F);
        bipedLeftArm.rotateAngleY = 0.1F - a * 0.6F;
        bipedRightArm.rotateAngleX = -pi / 2.0F;
        bipedLeftArm.rotateAngleX = -pi / 2.0F;
        bipedRightArm.rotateAngleX -= a * 1.2F - b * 0.4F;
        bipedLeftArm.rotateAngleX -= a * 1.2F - b * 0.4F;
        bipedRightArm.rotateAngleZ += MathHelper.cos(age * 0.09F) * 0.05F + 0.05F;
        bipedLeftArm.rotateAngleZ -= MathHelper.cos(age * 0.09F) * 0.05F + 0.05F;
        bipedRightArm.rotateAngleX += MathHelper.sin(age * 0.067F) * 0.05F;
        bipedLeftArm.rotateAngleX -= MathHelper.sin(age * 0.067F) * 0.05F;
    }
}
