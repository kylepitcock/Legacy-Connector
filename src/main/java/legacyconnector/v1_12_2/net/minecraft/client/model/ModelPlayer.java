package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide;
import org.kylepitcock.legacyconnector.v1_12_2.client.EntityPose1122;

/** 1.12.2's player model: the biped plus the outer skin layer, cape and ears, and slim (Alex) arms. */
public class ModelPlayer extends ModelBiped {
    public ModelRenderer bipedLeftArmwear;
    public ModelRenderer bipedRightArmwear;
    public ModelRenderer bipedLeftLegwear;
    public ModelRenderer bipedRightLegwear;
    public ModelRenderer bipedBodyWear;
    private final ModelRenderer bipedCape;
    private final ModelRenderer bipedDeadmau5Head;
    private final boolean smallArms;

    public ModelPlayer(float modelSize, boolean smallArmsIn) {
        super(modelSize, 0.0F, 64, 64);
        smallArms = smallArmsIn;
        bipedDeadmau5Head = new ModelRenderer(this, 24, 0);
        bipedDeadmau5Head.addBox(-3.0F, -6.0F, -1.0F, 6, 6, 1, modelSize);
        bipedCape = new ModelRenderer(this, 0, 0);
        bipedCape.setTextureSize(64, 32);
        bipedCape.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1, modelSize);
        if (smallArmsIn) {
            bipedLeftArm = new ModelRenderer(this, 32, 48);
            bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
            bipedLeftArm.setRotationPoint(5.0F, 2.5F, 0.0F);
            bipedRightArm = new ModelRenderer(this, 40, 16);
            bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
            bipedRightArm.setRotationPoint(-5.0F, 2.5F, 0.0F);
            bipedLeftArmwear = new ModelRenderer(this, 48, 48);
            bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, modelSize + 0.25F);
            bipedLeftArmwear.setRotationPoint(5.0F, 2.5F, 0.0F);
            bipedRightArmwear = new ModelRenderer(this, 40, 32);
            bipedRightArmwear.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, modelSize + 0.25F);
            bipedRightArmwear.setRotationPoint(-5.0F, 2.5F, 10.0F);
        } else {
            bipedLeftArm = new ModelRenderer(this, 32, 48);
            bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
            bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
            bipedLeftArmwear = new ModelRenderer(this, 48, 48);
            bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
            bipedLeftArmwear.setRotationPoint(5.0F, 2.0F, 0.0F);
            bipedRightArmwear = new ModelRenderer(this, 40, 32);
            bipedRightArmwear.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
            bipedRightArmwear.setRotationPoint(-5.0F, 2.0F, 10.0F);
        }
        bipedLeftLeg = new ModelRenderer(this, 16, 48);
        bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
        bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
        bipedLeftLegwear = new ModelRenderer(this, 0, 48);
        bipedLeftLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
        bipedLeftLegwear.setRotationPoint(1.9F, 12.0F, 0.0F);
        bipedRightLegwear = new ModelRenderer(this, 0, 32);
        bipedRightLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
        bipedRightLegwear.setRotationPoint(-1.9F, 12.0F, 0.0F);
        bipedBodyWear = new ModelRenderer(this, 16, 32);
        bipedBodyWear.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelSize + 0.25F);
        bipedBodyWear.setRotationPoint(0.0F, 0.0F, 0.0F);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.pushMatrix();
        if (isChild) {
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
        } else if (EntityPose1122.sneaking(entityIn)) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }
        bipedLeftLegwear.render(scale);
        bipedRightLegwear.render(scale);
        bipedLeftArmwear.render(scale);
        bipedRightArmwear.render(scale);
        bipedBodyWear.render(scale);
        GlStateManager.popMatrix();
    }

    public void renderDeadmau5Head(float scale) {
        copyModelAngles(bipedHead, bipedDeadmau5Head);
        bipedDeadmau5Head.rotationPointX = 0.0F;
        bipedDeadmau5Head.rotationPointY = 0.0F;
        bipedDeadmau5Head.render(scale);
    }

    public void renderCape(float scale) {
        bipedCape.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        copyModelAngles(bipedLeftLeg, bipedLeftLegwear);
        copyModelAngles(bipedRightLeg, bipedRightLegwear);
        copyModelAngles(bipedLeftArm, bipedLeftArmwear);
        copyModelAngles(bipedRightArm, bipedRightArmwear);
        copyModelAngles(bipedBody, bipedBodyWear);
        bipedCape.rotationPointY = EntityPose1122.sneaking(entityIn) ? 2.0F : 0.0F;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        bipedLeftArmwear.showModel = visible;
        bipedRightArmwear.showModel = visible;
        bipedLeftLegwear.showModel = visible;
        bipedRightLegwear.showModel = visible;
        bipedBodyWear.showModel = visible;
        bipedCape.showModel = visible;
        bipedDeadmau5Head.showModel = visible;
    }

    @Override
    public void postRenderArm(float scale, EnumHandSide side) {
        ModelRenderer arm = getArmForSide(side);
        if (smallArms) {
            float f = 0.5F * (side == EnumHandSide.RIGHT ? 1 : -1);
            arm.rotationPointX += f;
            arm.postRender(scale);
            arm.rotationPointX -= f;
        } else {
            arm.postRender(scale);
        }
    }
}
