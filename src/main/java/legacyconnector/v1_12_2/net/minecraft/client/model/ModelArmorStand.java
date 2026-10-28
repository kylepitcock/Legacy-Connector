package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide;

/** 1.12.2 armour stand model (64x64 texture), boxes as vanilla's: the stand's poses come from the entity's rotations. */
public class ModelArmorStand extends ModelBiped {
    public ModelRenderer standRightSide;
    public ModelRenderer standLeftSide;
    public ModelRenderer standWaist;
    public ModelRenderer standBase;

    public ModelArmorStand() {
        this(0.0F);
    }

    public ModelArmorStand(float modelSize) {
        super(modelSize, 0.0F, 64, 64);
        bipedHead = new ModelRenderer(this, 0, 0);
        bipedHead.addBox(-1.0F, -7.0F, -1.0F, 2, 7, 2, modelSize);
        bipedHead.setRotationPoint(0.0F, 0.0F, 0.0F);
        bipedBody = new ModelRenderer(this, 0, 26);
        bipedBody.addBox(-6.0F, 0.0F, -1.5F, 12, 3, 3, modelSize);
        bipedBody.setRotationPoint(0.0F, 0.0F, 0.0F);
        bipedRightArm = new ModelRenderer(this, 24, 0);
        bipedRightArm.addBox(-2.0F, -2.0F, -1.0F, 2, 12, 2, modelSize);
        bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
        bipedLeftArm = new ModelRenderer(this, 32, 16);
        bipedLeftArm.mirror = true;
        bipedLeftArm.addBox(0.0F, -2.0F, -1.0F, 2, 12, 2, modelSize);
        bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        bipedRightLeg = new ModelRenderer(this, 8, 0);
        bipedRightLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 11, 2, modelSize);
        bipedRightLeg.setRotationPoint(-1.9F, 12.0F, 0.0F);
        bipedLeftLeg = new ModelRenderer(this, 40, 16);
        bipedLeftLeg.mirror = true;
        bipedLeftLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 11, 2, modelSize);
        bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
        standRightSide = new ModelRenderer(this, 16, 0);
        standRightSide.addBox(-3.0F, 3.0F, -1.0F, 2, 7, 2, modelSize);
        standRightSide.setRotationPoint(0.0F, 0.0F, 0.0F);
        standRightSide.showModel = true;
        standLeftSide = new ModelRenderer(this, 48, 16);
        standLeftSide.addBox(1.0F, 3.0F, -1.0F, 2, 7, 2, modelSize);
        standLeftSide.setRotationPoint(0.0F, 0.0F, 0.0F);
        standWaist = new ModelRenderer(this, 0, 48);
        standWaist.addBox(-4.0F, 10.0F, -1.0F, 8, 2, 2, modelSize);
        standWaist.setRotationPoint(0.0F, 0.0F, 0.0F);
        standBase = new ModelRenderer(this, 0, 32);
        standBase.addBox(-6.0F, 11.0F, -6.0F, 12, 1, 12, modelSize);
        standBase.setRotationPoint(0.0F, 12.0F, 0.0F);
        bipedHeadwear.showModel = false;
    }

    private static void pose(ModelRenderer part, net.minecraft.core.Rotations r) {
        part.rotateAngleX = 0.017453292F * r.getX();
        part.rotateAngleY = 0.017453292F * r.getY();
        part.rotateAngleZ = 0.017453292F * r.getZ();
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        if (!(entityIn != null && entityIn.mcc$entity instanceof net.minecraft.world.entity.decoration.ArmorStand stand)) {
            super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
            return;
        }
        pose(bipedHead, stand.getHeadPose());
        pose(bipedBody, stand.getBodyPose());
        pose(bipedLeftArm, stand.getLeftArmPose());
        pose(bipedRightArm, stand.getRightArmPose());
        pose(bipedLeftLeg, stand.getLeftLegPose());
        pose(bipedRightLeg, stand.getRightLegPose());
        bipedLeftArm.showModel = stand.isShowArms();
        bipedRightArm.showModel = stand.isShowArms();
        standBase.showModel = !stand.isNoBasePlate();
        bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
        bipedRightLeg.setRotationPoint(-1.9F, 12.0F, 0.0F);
        pose(standRightSide, stand.getBodyPose());
        pose(standLeftSide, stand.getBodyPose());
        pose(standWaist, stand.getBodyPose());
        standBase.rotateAngleX = 0.0F;
        standBase.rotateAngleY = 0.017453292F * -Entity.mcc$get$rotationYaw(entityIn);
        standBase.rotateAngleZ = 0.0F;
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.pushMatrix();
        if (isChild) {
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
        }
        standRightSide.render(scale);
        standLeftSide.render(scale);
        standWaist.render(scale);
        standBase.render(scale);
        GlStateManager.popMatrix();
    }

    @Override
    public void postRenderArm(float scale, EnumHandSide side) {
        ModelRenderer arm = side == EnumHandSide.LEFT ? bipedLeftArm : bipedRightArm;
        boolean shown = arm.showModel;
        arm.showModel = true;
        super.postRenderArm(scale, side);
        arm.showModel = shown;
    }
}
