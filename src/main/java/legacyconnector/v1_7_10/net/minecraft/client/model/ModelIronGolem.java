package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityIronGolem;

/**
 * Vanilla's iron golem model. It was missing from the shim, so a mod class extending it (Twilight Forest's tower golem model) extended a
 * synthesized class that is not a {@link ModelBase}, and the mod's client proxy failed verification.
 */
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

    public ModelIronGolem(float grow) {
        this(grow, -7.0F);
    }

    public ModelIronGolem(float grow, float yOffset) {
        textureWidth = 128;
        textureHeight = 128;
        ironGolemHead = new ModelRenderer(this).setTextureSize(128, 128);
        ironGolemHead.setRotationPoint(0.0F, 0.0F + yOffset, -2.0F);
        ironGolemHead.setTextureOffset(0, 0).addBox(-4.0F, -12.0F, -5.5F, 8, 10, 8, grow);
        ironGolemHead.setTextureOffset(24, 0).addBox(-1.0F, -5.0F, -7.5F, 2, 4, 2, grow);
        ironGolemBody = new ModelRenderer(this).setTextureSize(128, 128);
        ironGolemBody.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
        ironGolemBody.setTextureOffset(0, 40).addBox(-9.0F, -2.0F, -6.0F, 18, 12, 11, grow);
        ironGolemBody.setTextureOffset(0, 70).addBox(-4.5F, 10.0F, -3.0F, 9, 5, 6, grow + 0.5F);
        ironGolemRightArm = new ModelRenderer(this).setTextureSize(128, 128);
        ironGolemRightArm.setRotationPoint(0.0F, -7.0F, 0.0F);
        ironGolemRightArm.setTextureOffset(60, 21).addBox(-13.0F, -2.5F, -3.0F, 4, 30, 6, grow);
        ironGolemLeftArm = new ModelRenderer(this).setTextureSize(128, 128);
        ironGolemLeftArm.setRotationPoint(0.0F, -7.0F, 0.0F);
        ironGolemLeftArm.setTextureOffset(60, 58).addBox(9.0F, -2.5F, -3.0F, 4, 30, 6, grow);
        ironGolemLeftLeg = new ModelRenderer(this, 0, 22).setTextureSize(128, 128);
        ironGolemLeftLeg.setRotationPoint(-4.0F, 18.0F + yOffset, 0.0F);
        ironGolemLeftLeg.setTextureOffset(60, 0).addBox(-3.5F, -3.0F, -3.0F, 6, 16, 5, grow);
        ironGolemRightLeg = new ModelRenderer(this, 0, 22).setTextureSize(128, 128);
        ironGolemRightLeg.mirror = true;
        ironGolemRightLeg.setTextureOffset(60, 0).setRotationPoint(5.0F, 18.0F + yOffset, 0.0F);
        ironGolemRightLeg.addBox(-3.5F, -3.0F, -3.0F, 6, 16, 5, grow);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, entity);
        ironGolemHead.render(scale);
        ironGolemBody.render(scale);
        ironGolemLeftLeg.render(scale);
        ironGolemRightLeg.render(scale);
        ironGolemRightArm.render(scale);
        ironGolemLeftArm.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity) {
        ironGolemHead.rotateAngleY = headYaw / (180F / (float) Math.PI);
        ironGolemHead.rotateAngleX = headPitch / (180F / (float) Math.PI);
        ironGolemLeftLeg.rotateAngleX = -1.5F * triangle(limbSwing, 13.0F) * limbSwingAmount;
        ironGolemRightLeg.rotateAngleX = 1.5F * triangle(limbSwing, 13.0F) * limbSwingAmount;
        ironGolemLeftLeg.rotateAngleY = 0.0F;
        ironGolemRightLeg.rotateAngleY = 0.0F;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partial) {
        int rose = entity instanceof EntityIronGolem golem ? golem.getHoldRoseTick() : 0;
        if (rose > 0) {
            ironGolemRightArm.rotateAngleX = -0.8F + 0.025F * triangle(rose, 70.0F);
            ironGolemLeftArm.rotateAngleX = 0.0F;
        } else {
            ironGolemRightArm.rotateAngleX = (-0.2F + 1.5F * triangle(limbSwing, 13.0F)) * limbSwingAmount;
            ironGolemLeftArm.rotateAngleX = (-0.2F - 1.5F * triangle(limbSwing, 13.0F)) * limbSwingAmount;
        }
    }

    private static float triangle(float a, float b) {
        return (Math.abs(a % b - b * 0.5F) - b * 0.25F) / (b * 0.25F);
    }
}
