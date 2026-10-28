package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/** 1.12.2 wolf model (boxes as vanilla's): sits, wags, tilts its head when interested and shakes itself dry. */
public class ModelWolf extends ModelBase {
    public ModelRenderer wolfHeadMain;
    public ModelRenderer wolfBody;
    public ModelRenderer wolfLeg1;
    public ModelRenderer wolfLeg2;
    public ModelRenderer wolfLeg3;
    public ModelRenderer wolfLeg4;
    ModelRenderer wolfTail;
    ModelRenderer wolfMane;

    public ModelWolf() {
        wolfHeadMain = new ModelRenderer(this, 0, 0);
        wolfHeadMain.addBox(-2.0F, -3.0F, -2.0F, 6, 6, 4, 0.0F);
        wolfHeadMain.setRotationPoint(-1.0F, 13.5F, -7.0F);
        wolfBody = new ModelRenderer(this, 18, 14);
        wolfBody.addBox(-3.0F, -2.0F, -3.0F, 6, 9, 6, 0.0F);
        wolfBody.setRotationPoint(0.0F, 14.0F, 2.0F);
        wolfMane = new ModelRenderer(this, 21, 0);
        wolfMane.addBox(-3.0F, -3.0F, -3.0F, 8, 6, 7, 0.0F);
        wolfMane.setRotationPoint(-1.0F, 14.0F, 2.0F);
        wolfLeg1 = new ModelRenderer(this, 0, 18);
        wolfLeg1.addBox(0.0F, 0.0F, -1.0F, 2, 8, 2, 0.0F);
        wolfLeg1.setRotationPoint(-2.5F, 16.0F, 7.0F);
        wolfLeg2 = new ModelRenderer(this, 0, 18);
        wolfLeg2.addBox(0.0F, 0.0F, -1.0F, 2, 8, 2, 0.0F);
        wolfLeg2.setRotationPoint(0.5F, 16.0F, 7.0F);
        wolfLeg3 = new ModelRenderer(this, 0, 18);
        wolfLeg3.addBox(0.0F, 0.0F, -1.0F, 2, 8, 2, 0.0F);
        wolfLeg3.setRotationPoint(-2.5F, 16.0F, -4.0F);
        wolfLeg4 = new ModelRenderer(this, 0, 18);
        wolfLeg4.addBox(0.0F, 0.0F, -1.0F, 2, 8, 2, 0.0F);
        wolfLeg4.setRotationPoint(0.5F, 16.0F, -4.0F);
        wolfTail = new ModelRenderer(this, 9, 18);
        wolfTail.addBox(0.0F, 0.0F, -1.0F, 2, 8, 2, 0.0F);
        wolfTail.setRotationPoint(-1.0F, 12.0F, 8.0F);
        wolfHeadMain.setTextureOffset(16, 14).addBox(-2.0F, -5.0F, 0.0F, 2, 2, 1, 0.0F);
        wolfHeadMain.setTextureOffset(16, 14).addBox(2.0F, -5.0F, 0.0F, 2, 2, 1, 0.0F);
        wolfHeadMain.setTextureOffset(0, 10).addBox(-0.5F, 0.0F, -5.0F, 3, 3, 4, 0.0F);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
        if (isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 5.0F * scale, 2.0F * scale);
            wolfHeadMain.renderWithRotation(scale);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
        } else {
            wolfHeadMain.renderWithRotation(scale);
        }
        wolfBody.render(scale);
        wolfLeg1.render(scale);
        wolfLeg2.render(scale);
        wolfLeg3.render(scale);
        wolfLeg4.render(scale);
        wolfTail.renderWithRotation(scale);
        wolfMane.render(scale);
        if (isChild) GlStateManager.popMatrix();
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
        var wolf = entitylivingbaseIn != null && entitylivingbaseIn.mcc$entity instanceof net.minecraft.world.entity.animal.Wolf w ? w : null;
        boolean angry = wolf != null && wolf.isAngry();
        wolfTail.rotateAngleY = angry ? 0.0F : MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        if (wolf != null && wolf.isInSittingPose()) {
            wolfMane.setRotationPoint(-1.0F, 16.0F, -3.0F);
            wolfMane.rotateAngleX = ((float) Math.PI * 2F / 5F);
            wolfMane.rotateAngleY = 0.0F;
            wolfBody.setRotationPoint(0.0F, 18.0F, 0.0F);
            wolfBody.rotateAngleX = ((float) Math.PI / 4F);
            wolfTail.setRotationPoint(-1.0F, 21.0F, 6.0F);
            wolfLeg1.setRotationPoint(-2.5F, 22.0F, 2.0F);
            wolfLeg1.rotateAngleX = ((float) Math.PI * 3F / 2F);
            wolfLeg2.setRotationPoint(0.5F, 22.0F, 2.0F);
            wolfLeg2.rotateAngleX = ((float) Math.PI * 3F / 2F);
            wolfLeg3.rotateAngleX = 5.811947F;
            wolfLeg3.setRotationPoint(-2.49F, 17.0F, -4.0F);
            wolfLeg4.rotateAngleX = 5.811947F;
            wolfLeg4.setRotationPoint(0.51F, 17.0F, -4.0F);
        } else {
            wolfBody.setRotationPoint(0.0F, 14.0F, 2.0F);
            wolfBody.rotateAngleX = ((float) Math.PI / 2F);
            wolfMane.setRotationPoint(-1.0F, 14.0F, -3.0F);
            wolfMane.rotateAngleX = wolfBody.rotateAngleX;
            wolfTail.setRotationPoint(-1.0F, 12.0F, 8.0F);
            wolfLeg1.setRotationPoint(-2.5F, 16.0F, 7.0F);
            wolfLeg2.setRotationPoint(0.5F, 16.0F, 7.0F);
            wolfLeg3.setRotationPoint(-2.5F, 16.0F, -4.0F);
            wolfLeg4.setRotationPoint(0.5F, 16.0F, -4.0F);
            wolfLeg1.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            wolfLeg2.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            wolfLeg3.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            wolfLeg4.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        }
        if (wolf != null) {
            wolfHeadMain.rotateAngleZ = wolf.getHeadRollAngle(partialTickTime) + wolf.getBodyRollAngle(partialTickTime, 0.0F);
            wolfMane.rotateAngleZ = wolf.getBodyRollAngle(partialTickTime, -0.08F);
            wolfBody.rotateAngleZ = wolf.getBodyRollAngle(partialTickTime, -0.16F);
            wolfTail.rotateAngleZ = wolf.getBodyRollAngle(partialTickTime, -0.2F);
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        wolfHeadMain.rotateAngleX = headPitch * 0.017453292F;
        wolfHeadMain.rotateAngleY = netHeadYaw * 0.017453292F;
        wolfTail.rotateAngleX = ageInTicks;
    }
}
