package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/**
 * 1.12.2 illager model (vindicators, evokers and mods' illager-like mobs): robed body, head with nose and hat, and either crossed arms
 * or free arms. The free arms are used while the mob is aggressive (raised to attack, as the attacking pose did); otherwise the arms
 * stay crossed.
 */
public class ModelIllager extends ModelBase {
    public ModelRenderer head;
    public ModelRenderer hat;
    public ModelRenderer body;
    public ModelRenderer arms;
    public ModelRenderer leg0;
    public ModelRenderer leg1;
    public ModelRenderer nose;
    public ModelRenderer rightArm;
    public ModelRenderer leftArm;

    public ModelIllager(float scaleFactor, float yOffset, int textureWidthIn, int textureHeightIn) {
        head = new ModelRenderer(this).setTextureSize(textureWidthIn, textureHeightIn);
        head.setRotationPoint(0.0F, yOffset, 0.0F);
        head.setTextureOffset(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, scaleFactor);
        hat = new ModelRenderer(this, 32, 0).setTextureSize(textureWidthIn, textureHeightIn);
        hat.addBox(-4.0F, -10.0F, -4.0F, 8, 12, 8, scaleFactor + 0.45F);
        head.addChild(hat);
        hat.showModel = false;
        nose = new ModelRenderer(this).setTextureSize(textureWidthIn, textureHeightIn);
        nose.setRotationPoint(0.0F, yOffset - 2.0F, 0.0F);
        nose.setTextureOffset(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2, 4, 2, scaleFactor);
        head.addChild(nose);
        body = new ModelRenderer(this).setTextureSize(textureWidthIn, textureHeightIn);
        body.setRotationPoint(0.0F, yOffset, 0.0F);
        body.setTextureOffset(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6, scaleFactor);
        body.setTextureOffset(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8, 18, 6, scaleFactor + 0.5F);
        arms = new ModelRenderer(this).setTextureSize(textureWidthIn, textureHeightIn);
        arms.setRotationPoint(0.0F, yOffset + 2.0F, 0.0F);
        arms.setTextureOffset(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4, 8, 4, scaleFactor);
        ModelRenderer left = new ModelRenderer(this, 44, 22).setTextureSize(textureWidthIn, textureHeightIn);
        left.mirror = true;
        left.addBox(4.0F, -2.0F, -2.0F, 4, 8, 4, scaleFactor);
        arms.addChild(left);
        arms.setTextureOffset(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8, 4, 4, scaleFactor);
        leg0 = new ModelRenderer(this, 0, 22).setTextureSize(textureWidthIn, textureHeightIn);
        leg0.setRotationPoint(-2.0F, 12.0F + yOffset, 0.0F);
        leg0.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, scaleFactor);
        leg1 = new ModelRenderer(this, 0, 22).setTextureSize(textureWidthIn, textureHeightIn);
        leg1.mirror = true;
        leg1.setRotationPoint(2.0F, 12.0F + yOffset, 0.0F);
        leg1.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, scaleFactor);
        rightArm = new ModelRenderer(this, 40, 46).setTextureSize(textureWidthIn, textureHeightIn);
        rightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, scaleFactor);
        rightArm.setRotationPoint(-5.0F, 2.0F + yOffset, 0.0F);
        leftArm = new ModelRenderer(this, 40, 46).setTextureSize(textureWidthIn, textureHeightIn);
        leftArm.mirror = true;
        leftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, scaleFactor);
        leftArm.setRotationPoint(5.0F, 2.0F + yOffset, 0.0F);
    }

    private static boolean freeArms(Entity entity) {
        return entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.Mob mob && mob.isAggressive();
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
        head.render(scale);
        body.render(scale);
        leg0.render(scale);
        leg1.render(scale);
        if (freeArms(entityIn)) {
            rightArm.render(scale);
            leftArm.render(scale);
        } else {
            arms.render(scale);
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        head.rotateAngleY = netHeadYaw * 0.017453292F;
        head.rotateAngleX = headPitch * 0.017453292F;
        arms.rotationPointY = 3.0F;
        arms.rotationPointZ = -1.0F;
        arms.rotateAngleX = -0.75F;
        leg0.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount * 0.5F;
        leg1.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount * 0.5F;
        leg0.rotateAngleY = 0.0F;
        leg1.rotateAngleY = 0.0F;
        if (freeArms(entityIn)) {
            // the attacking pose: arms raised toward the target, swinging with the attack
            float f = MathHelper.sin(swingProgress * (float) Math.PI);
            float f1 = MathHelper.sin((1.0F - (1.0F - swingProgress) * (1.0F - swingProgress)) * (float) Math.PI);
            rightArm.rotateAngleZ = 0.0F;
            leftArm.rotateAngleZ = 0.0F;
            rightArm.rotateAngleY = 0.15707964F;
            leftArm.rotateAngleY = -0.15707964F;
            rightArm.rotateAngleX = -1.8849558F + MathHelper.cos(ageInTicks * 0.09F) * 0.15F;
            leftArm.rotateAngleX = -0.0F + MathHelper.cos(ageInTicks * 0.19F) * 0.5F;
            rightArm.rotateAngleX += f * 2.2F - f1 * 0.4F;
            leftArm.rotateAngleX += f * 1.2F - f1 * 0.4F;
            rightArm.rotateAngleZ += MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
            leftArm.rotateAngleZ -= MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
            rightArm.rotateAngleX += MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
            leftArm.rotateAngleX -= MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
        }
    }

    public ModelRenderer getArm(EnumHandSide side) {
        return side == EnumHandSide.LEFT ? leftArm : rightArm;
    }
}
