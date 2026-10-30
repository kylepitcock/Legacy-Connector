package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.AbstractChestHorse;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.AbstractHorse;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/**
 * The 1.12 horse (texture 128x128, the pre-1.14 layout mods' own horse textures use): legs in three parts, saddle and reins
 * while saddled, donkey ears and chests for chested horses, grazing, rearing and mouth animations read from the horse.
 */
public class ModelHorse extends ModelBase {
    private final ModelRenderer head;
    private final ModelRenderer upperMouth;
    private final ModelRenderer lowerMouth;
    private final ModelRenderer horseLeftEar;
    private final ModelRenderer horseRightEar;
    private final ModelRenderer muleLeftEar;
    private final ModelRenderer muleRightEar;
    private final ModelRenderer neck;
    private final ModelRenderer horseFaceRopes;
    private final ModelRenderer mane;
    private final ModelRenderer body;
    private final ModelRenderer tailBase;
    private final ModelRenderer tailMiddle;
    private final ModelRenderer tailTip;
    private final ModelRenderer backLeftLeg;
    private final ModelRenderer backLeftShin;
    private final ModelRenderer backLeftHoof;
    private final ModelRenderer backRightLeg;
    private final ModelRenderer backRightShin;
    private final ModelRenderer backRightHoof;
    private final ModelRenderer frontLeftLeg;
    private final ModelRenderer frontLeftShin;
    private final ModelRenderer frontLeftHoof;
    private final ModelRenderer frontRightLeg;
    private final ModelRenderer frontRightShin;
    private final ModelRenderer frontRightHoof;
    private final ModelRenderer muleLeftChest;
    private final ModelRenderer muleRightChest;
    private final ModelRenderer horseSaddleBottom;
    private final ModelRenderer horseSaddleFront;
    private final ModelRenderer horseSaddleBack;
    private final ModelRenderer horseLeftSaddleRope;
    private final ModelRenderer horseLeftSaddleMetal;
    private final ModelRenderer horseRightSaddleRope;
    private final ModelRenderer horseRightSaddleMetal;
    private final ModelRenderer horseLeftFaceMetal;
    private final ModelRenderer horseRightFaceMetal;
    private final ModelRenderer horseLeftRein;
    private final ModelRenderer horseRightRein;

    public ModelHorse() {
        textureWidth = 128;
        textureHeight = 128;
        body = box(0, 34, -5.0F, -8.0F, -19.0F, 10, 10, 24, 0.0F, 11.0F, 9.0F);
        tailBase = box(44, 0, -1.0F, -1.0F, 0.0F, 2, 2, 3, 0.0F, 3.0F, 14.0F);
        tailBase.rotateAngleX = -1.134464F;
        tailMiddle = box(38, 7, -1.5F, -2.0F, 3.0F, 3, 4, 7, 0.0F, 3.0F, 14.0F);
        tailMiddle.rotateAngleX = -1.134464F;
        tailTip = box(24, 3, -1.5F, -4.5F, 9.0F, 3, 4, 7, 0.0F, 3.0F, 14.0F);
        tailTip.rotateAngleX = -1.3962634F;
        backLeftLeg = box(78, 29, -2.5F, -2.0F, -2.5F, 4, 9, 5, 4.0F, 9.0F, 11.0F);
        backLeftShin = box(78, 43, -2.0F, 0.0F, -1.5F, 3, 5, 3, 4.0F, 16.0F, 11.0F);
        backLeftHoof = box(78, 51, -2.5F, 5.1F, -2.0F, 4, 3, 4, 4.0F, 16.0F, 11.0F);
        backRightLeg = box(96, 29, -1.5F, -2.0F, -2.5F, 4, 9, 5, -4.0F, 9.0F, 11.0F);
        backRightShin = box(96, 43, -1.0F, 0.0F, -1.5F, 3, 5, 3, -4.0F, 16.0F, 11.0F);
        backRightHoof = box(96, 51, -1.5F, 5.1F, -2.0F, 4, 3, 4, -4.0F, 16.0F, 11.0F);
        frontLeftLeg = box(44, 29, -1.9F, -1.0F, -2.1F, 3, 8, 4, 4.0F, 9.0F, -8.0F);
        frontLeftShin = box(44, 41, -1.9F, 0.0F, -1.6F, 3, 5, 3, 4.0F, 16.0F, -8.0F);
        frontLeftHoof = box(44, 51, -2.4F, 5.1F, -2.1F, 4, 3, 4, 4.0F, 16.0F, -8.0F);
        frontRightLeg = box(60, 29, -1.1F, -1.0F, -2.1F, 3, 8, 4, -4.0F, 9.0F, -8.0F);
        frontRightShin = box(60, 41, -1.1F, 0.0F, -1.6F, 3, 5, 3, -4.0F, 16.0F, -8.0F);
        frontRightHoof = box(60, 51, -1.6F, 5.1F, -2.1F, 4, 3, 4, -4.0F, 16.0F, -8.0F);
        head = box(0, 0, -2.5F, -10.0F, -1.5F, 5, 5, 7, 0.0F, 4.0F, -10.0F);
        head.rotateAngleX = 0.5235988F;
        upperMouth = box(24, 18, -2.0F, -10.0F, -7.0F, 4, 3, 6, 0.0F, 3.95F, -10.0F);
        upperMouth.rotateAngleX = 0.5235988F;
        lowerMouth = box(24, 27, -2.0F, -7.0F, -6.5F, 4, 2, 5, 0.0F, 4.0F, -10.0F);
        lowerMouth.rotateAngleX = 0.5235988F;
        head.addChild(upperMouth);
        head.addChild(lowerMouth);
        horseLeftEar = box(0, 0, 0.45F, -12.0F, 4.0F, 2, 3, 1, 0.0F, 4.0F, -10.0F);
        horseLeftEar.rotateAngleX = 0.5235988F;
        horseRightEar = box(0, 0, -2.45F, -12.0F, 4.0F, 2, 3, 1, 0.0F, 4.0F, -10.0F);
        horseRightEar.rotateAngleX = 0.5235988F;
        muleLeftEar = box(0, 12, -2.0F, -16.0F, 4.0F, 2, 7, 1, 0.0F, 4.0F, -10.0F);
        muleLeftEar.rotateAngleX = 0.5235988F;
        muleLeftEar.rotateAngleZ = 0.2617994F;
        muleRightEar = box(0, 12, 0.0F, -16.0F, 4.0F, 2, 7, 1, 0.0F, 4.0F, -10.0F);
        muleRightEar.rotateAngleX = 0.5235988F;
        muleRightEar.rotateAngleZ = -0.2617994F;
        neck = box(0, 12, -2.05F, -9.8F, -2.0F, 4, 14, 8, 0.0F, 4.0F, -10.0F);
        neck.rotateAngleX = 0.5235988F;
        muleLeftChest = box(0, 34, -3.0F, 0.0F, 0.0F, 8, 8, 3, -7.5F, 3.0F, 10.0F);
        muleLeftChest.rotateAngleY = (float) Math.PI / 2F;
        muleRightChest = box(0, 47, -3.0F, 0.0F, 0.0F, 8, 8, 3, 4.5F, 3.0F, 10.0F);
        muleRightChest.rotateAngleY = (float) Math.PI / 2F;
        horseSaddleBottom = box(80, 0, -5.0F, 0.0F, -3.0F, 10, 1, 8, 0.0F, 2.0F, 2.0F);
        horseSaddleFront = box(106, 9, -1.5F, -1.0F, -3.0F, 3, 1, 2, 0.0F, 2.0F, 2.0F);
        horseSaddleBack = box(80, 9, -4.0F, -1.0F, 3.0F, 8, 1, 2, 0.0F, 2.0F, 2.0F);
        horseLeftSaddleMetal = box(74, 0, -0.5F, 6.0F, -1.0F, 1, 2, 2, 5.0F, 3.0F, 2.0F);
        horseLeftSaddleRope = box(70, 0, -0.5F, 0.0F, -0.5F, 1, 6, 1, 5.0F, 3.0F, 2.0F);
        horseRightSaddleMetal = box(74, 4, -0.5F, 6.0F, -1.0F, 1, 2, 2, -5.0F, 3.0F, 2.0F);
        horseRightSaddleRope = box(80, 0, -0.5F, 0.0F, -0.5F, 1, 6, 1, -5.0F, 3.0F, 2.0F);
        horseLeftFaceMetal = box(74, 13, 1.5F, -8.0F, -4.0F, 1, 2, 2, 0.0F, 4.0F, -10.0F);
        horseLeftFaceMetal.rotateAngleX = 0.5235988F;
        horseRightFaceMetal = box(74, 13, -2.5F, -8.0F, -4.0F, 1, 2, 2, 0.0F, 4.0F, -10.0F);
        horseRightFaceMetal.rotateAngleX = 0.5235988F;
        horseLeftRein = box(44, 10, 2.6F, -6.0F, -6.0F, 0, 3, 16, 0.0F, 4.0F, -10.0F);
        horseRightRein = box(44, 5, -2.6F, -6.0F, -6.0F, 0, 3, 16, 0.0F, 4.0F, -10.0F);
        mane = box(58, 0, -1.0F, -11.5F, 5.0F, 2, 16, 4, 0.0F, 4.0F, -10.0F);
        mane.rotateAngleX = 0.5235988F;
        horseFaceRopes = new ModelRenderer(this, 80, 12);
        horseFaceRopes.addBox(-2.5F, -10.1F, -7.0F, 5, 5, 12, 0.2F);
        horseFaceRopes.setRotationPoint(0.0F, 4.0F, -10.0F);
        horseFaceRopes.rotateAngleX = 0.5235988F;
    }

    private ModelRenderer box(int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz) {
        ModelRenderer r = new ModelRenderer(this, u, v);
        r.addBox(x, y, z, w, h, d);
        r.setRotationPoint(px, py, pz);
        return r;
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        AbstractHorse horse = entityIn instanceof AbstractHorse h ? h : null;
        float grazing = horse == null ? 0.0F : horse.getGrassEatingAmount(0.0F);
        boolean child = horse != null && horse.isChild();
        boolean saddled = !child && horse != null && horse.isHorseSaddled();
        boolean chestType = horse instanceof AbstractChestHorse;
        boolean chest = !child && chestType && ((AbstractChestHorse) horse).hasChest();
        float size = horse == null ? 1.0F : horse.getHorseSize();
        boolean ridden = horse != null && horse.isBeingRidden();
        if (saddled) {
            horseFaceRopes.render(scale);
            horseSaddleBottom.render(scale);
            horseSaddleFront.render(scale);
            horseSaddleBack.render(scale);
            horseLeftSaddleRope.render(scale);
            horseLeftSaddleMetal.render(scale);
            horseRightSaddleRope.render(scale);
            horseRightSaddleMetal.render(scale);
            horseLeftFaceMetal.render(scale);
            horseRightFaceMetal.render(scale);
            if (ridden) {
                horseLeftRein.render(scale);
                horseRightRein.render(scale);
            }
        }
        if (child) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(size, 0.5F + size * 0.5F, size);
            GlStateManager.translate(0.0F, 0.95F * (1.0F - size), 0.0F);
        }
        backLeftLeg.render(scale);
        backLeftShin.render(scale);
        backLeftHoof.render(scale);
        backRightLeg.render(scale);
        backRightShin.render(scale);
        backRightHoof.render(scale);
        frontLeftLeg.render(scale);
        frontLeftShin.render(scale);
        frontLeftHoof.render(scale);
        frontRightLeg.render(scale);
        frontRightShin.render(scale);
        frontRightHoof.render(scale);
        if (child) {
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(size, size, size);
            GlStateManager.translate(0.0F, 1.35F * (1.0F - size), 0.0F);
        }
        body.render(scale);
        tailBase.render(scale);
        tailMiddle.render(scale);
        tailTip.render(scale);
        neck.render(scale);
        mane.render(scale);
        if (child) {
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            float headScale = 0.5F + size * size * 0.5F;
            GlStateManager.scale(headScale, headScale, headScale);
            if (grazing <= 0.0F) {
                GlStateManager.translate(0.0F, 1.35F * (1.0F - size), 0.0F);
            } else {
                GlStateManager.translate(0.0F, 0.9F * (1.0F - size) * grazing + 1.35F * (1.0F - size) * (1.0F - grazing), 0.15F * (1.0F - size) * grazing);
            }
        }
        if (chestType) {
            muleLeftEar.render(scale);
            muleRightEar.render(scale);
        } else {
            horseLeftEar.render(scale);
            horseRightEar.render(scale);
        }
        head.render(scale);
        if (child) GlStateManager.popMatrix();
        if (chest) {
            muleLeftChest.render(scale);
            muleRightChest.render(scale);
        }
    }

    private float updateHorseRotation(float prev, float current, float partialTicks) {
        float f = current - prev;
        while (f < -180.0F) f += 360.0F;
        while (f >= 180.0F) f -= 360.0F;
        return prev + partialTicks * f;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTickTime) {
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTickTime);
        float bodyYaw = updateHorseRotation(EntityLivingBase.mcc$get$prevRenderYawOffset(entity), EntityLivingBase.mcc$get$renderYawOffset(entity), partialTickTime);
        float headYaw = updateHorseRotation(EntityLivingBase.mcc$get$prevRotationYawHead(entity), EntityLivingBase.mcc$get$rotationYawHead(entity), partialTickTime);
        float prevPitch = Entity.mcc$get$prevRotationPitch(entity);
        float pitch = prevPitch + (Entity.mcc$get$rotationPitch(entity) - prevPitch) * partialTickTime;
        float yawDelta = MathHelper.clamp(headYaw - bodyYaw, -20.0F, 20.0F);
        float f4 = pitch * 0.017453292F;
        if (limbSwingAmount > 0.2F) f4 += MathHelper.cos(limbSwing * 0.4F) * 0.15F * limbSwingAmount;
        AbstractHorse horse = entity instanceof AbstractHorse h ? h : null;
        float f5 = horse == null ? 0.0F : horse.getGrassEatingAmount(partialTickTime);
        float f6 = horse == null ? 0.0F : horse.getRearingAmount(partialTickTime);
        float f7 = 1.0F - f6;
        float f8 = horse == null ? 0.0F : horse.getMouthOpennessAngle(partialTickTime);
        boolean tailSwing = horse != null && AbstractHorse.mcc$get$tailCounter(horse) != 0;
        boolean saddled = horse != null && horse.isHorseSaddled();
        boolean ridden = entity.isBeingRidden();
        float f9 = Entity.mcc$get$ticksExisted(entity) + partialTickTime;
        float f10 = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI);
        float f11 = f10 * 0.8F * limbSwingAmount;
        head.rotationPointY = 4.0F;
        head.rotationPointZ = -10.0F;
        tailBase.rotationPointY = 3.0F;
        tailMiddle.rotationPointZ = 14.0F;
        muleRightChest.rotationPointY = 3.0F;
        muleRightChest.rotationPointZ = 10.0F;
        body.rotateAngleX = 0.0F;
        head.rotateAngleX = 0.5235988F + f4;
        head.rotateAngleY = yawDelta * 0.017453292F;
        float still = 1.0F - Math.max(f6, f5);
        head.rotateAngleX = f6 * (0.2617994F + f4) + f5 * 2.1816616F + still * head.rotateAngleX;
        head.rotateAngleY = f6 * yawDelta * 0.017453292F + still * head.rotateAngleY;
        head.rotationPointY = f6 * -6.0F + f5 * 11.0F + still * head.rotationPointY;
        head.rotationPointZ = f6 * -1.0F + f5 * -10.0F + still * head.rotationPointZ;
        tailBase.rotationPointY = f6 * 9.0F + f7 * tailBase.rotationPointY;
        tailMiddle.rotationPointZ = f6 * 18.0F + f7 * tailMiddle.rotationPointZ;
        muleRightChest.rotationPointY = f6 * 5.5F + f7 * muleRightChest.rotationPointY;
        muleRightChest.rotationPointZ = f6 * 15.0F + f7 * muleRightChest.rotationPointZ;
        body.rotateAngleX = f6 * -((float) Math.PI / 4F) + f7 * body.rotateAngleX;
        for (ModelRenderer part : new ModelRenderer[] {horseLeftEar, horseRightEar, muleLeftEar, muleRightEar, neck, mane}) {
            part.rotationPointY = head.rotationPointY;
            part.rotationPointZ = head.rotationPointZ;
            part.rotateAngleX = head.rotateAngleX;
            part.rotateAngleY = head.rotateAngleY;
        }
        upperMouth.rotationPointY = 0.02F;
        lowerMouth.rotationPointY = 0.0F;
        upperMouth.rotationPointZ = 0.02F - f8;
        lowerMouth.rotationPointZ = f8;
        upperMouth.rotateAngleX = -0.09424778F * f8;
        lowerMouth.rotateAngleX = 0.15707964F * f8;
        upperMouth.rotateAngleY = 0.0F;
        lowerMouth.rotateAngleY = 0.0F;
        muleLeftChest.rotateAngleX = f11 / 5.0F;
        muleRightChest.rotateAngleX = -f11 / 5.0F;
        float f15 = 0.2617994F * f6;
        float f16 = MathHelper.cos(f9 * 0.6F + (float) Math.PI);
        frontLeftLeg.rotationPointY = -2.0F * f6 + 9.0F * f7;
        frontLeftLeg.rotationPointZ = -2.0F * f6 + -8.0F * f7;
        frontRightLeg.rotationPointY = frontLeftLeg.rotationPointY;
        frontRightLeg.rotationPointZ = frontLeftLeg.rotationPointZ;
        float half = (float) Math.PI / 2F, threeHalves = (float) Math.PI * 3F / 2F;
        backLeftShin.rotationPointY = backLeftLeg.rotationPointY + MathHelper.sin(half + f15 + f7 * -f10 * 0.5F * limbSwingAmount) * 7.0F;
        backLeftShin.rotationPointZ = backLeftLeg.rotationPointZ + MathHelper.cos(threeHalves + f15 + f7 * -f10 * 0.5F * limbSwingAmount) * 7.0F;
        backRightShin.rotationPointY = backRightLeg.rotationPointY + MathHelper.sin(half + f15 + f7 * f10 * 0.5F * limbSwingAmount) * 7.0F;
        backRightShin.rotationPointZ = backRightLeg.rotationPointZ + MathHelper.cos(threeHalves + f15 + f7 * f10 * 0.5F * limbSwingAmount) * 7.0F;
        float f17 = (-1.0471976F + f16) * f6 + f11 * f7;
        float f18 = (-1.0471976F - f16) * f6 + -f11 * f7;
        frontLeftShin.rotationPointY = frontLeftLeg.rotationPointY + MathHelper.sin(half + f17) * 7.0F;
        frontLeftShin.rotationPointZ = frontLeftLeg.rotationPointZ + MathHelper.cos(threeHalves + f17) * 7.0F;
        frontRightShin.rotationPointY = frontRightLeg.rotationPointY + MathHelper.sin(half + f18) * 7.0F;
        frontRightShin.rotationPointZ = frontRightLeg.rotationPointZ + MathHelper.cos(threeHalves + f18) * 7.0F;
        backLeftLeg.rotateAngleX = f15 + -f10 * 0.5F * limbSwingAmount * f7;
        backLeftShin.rotateAngleX = -0.08726646F * f6 + (-f10 * 0.5F * limbSwingAmount - Math.max(0.0F, f10 * 0.5F * limbSwingAmount)) * f7;
        backLeftHoof.rotateAngleX = backLeftShin.rotateAngleX;
        backRightLeg.rotateAngleX = f15 + f10 * 0.5F * limbSwingAmount * f7;
        backRightShin.rotateAngleX = -0.08726646F * f6 + (f10 * 0.5F * limbSwingAmount - Math.max(0.0F, -f10 * 0.5F * limbSwingAmount)) * f7;
        backRightHoof.rotateAngleX = backRightShin.rotateAngleX;
        frontLeftLeg.rotateAngleX = f17;
        frontLeftShin.rotateAngleX = (frontLeftLeg.rotateAngleX + (float) Math.PI * Math.max(0.0F, 0.2F + f16 * 0.2F)) * f6 + (f11 + Math.max(0.0F, f10 * 0.5F * limbSwingAmount)) * f7;
        frontLeftHoof.rotateAngleX = frontLeftShin.rotateAngleX;
        frontRightLeg.rotateAngleX = f18;
        frontRightShin.rotateAngleX = (frontRightLeg.rotateAngleX + (float) Math.PI * Math.max(0.0F, 0.2F - f16 * 0.2F)) * f6 + (-f11 + Math.max(0.0F, -f10 * 0.5F * limbSwingAmount)) * f7;
        frontRightHoof.rotateAngleX = frontRightShin.rotateAngleX;
        backLeftHoof.rotationPointY = backLeftShin.rotationPointY;
        backLeftHoof.rotationPointZ = backLeftShin.rotationPointZ;
        backRightHoof.rotationPointY = backRightShin.rotationPointY;
        backRightHoof.rotationPointZ = backRightShin.rotationPointZ;
        frontLeftHoof.rotationPointY = frontLeftShin.rotationPointY;
        frontLeftHoof.rotationPointZ = frontLeftShin.rotationPointZ;
        frontRightHoof.rotationPointY = frontRightShin.rotationPointY;
        frontRightHoof.rotationPointZ = frontRightShin.rotationPointZ;
        if (saddled) {
            horseSaddleBottom.rotationPointY = f6 * 0.5F + f7 * 2.0F;
            horseSaddleBottom.rotationPointZ = f6 * 11.0F + f7 * 2.0F;
            for (ModelRenderer part : new ModelRenderer[] {horseSaddleFront, horseSaddleBack, horseLeftSaddleRope, horseRightSaddleRope,
                    horseLeftSaddleMetal, horseRightSaddleMetal}) {
                part.rotationPointY = horseSaddleBottom.rotationPointY;
                part.rotationPointZ = horseSaddleBottom.rotationPointZ;
            }
            muleLeftChest.rotateAngleX = body.rotateAngleX;
            horseSaddleBottom.rotateAngleX = body.rotateAngleX;
            horseSaddleFront.rotateAngleX = body.rotateAngleX;
            horseSaddleBack.rotateAngleX = body.rotateAngleX;
            for (ModelRenderer part : new ModelRenderer[] {horseLeftRein, horseRightRein, horseFaceRopes, horseLeftFaceMetal, horseRightFaceMetal}) {
                part.rotationPointY = head.rotationPointY;
                part.rotationPointZ = head.rotationPointZ;
            }
            horseLeftRein.rotateAngleX = f4;
            horseRightRein.rotateAngleX = f4;
            horseFaceRopes.rotateAngleX = head.rotateAngleX;
            horseLeftFaceMetal.rotateAngleX = head.rotateAngleX;
            horseRightFaceMetal.rotateAngleX = head.rotateAngleX;
            horseFaceRopes.rotateAngleY = head.rotateAngleY;
            horseLeftFaceMetal.rotateAngleY = head.rotateAngleY;
            horseLeftRein.rotateAngleY = head.rotateAngleY;
            horseRightFaceMetal.rotateAngleY = head.rotateAngleY;
            horseRightRein.rotateAngleY = head.rotateAngleY;
            if (ridden) {
                horseLeftSaddleRope.rotateAngleX = -1.0471976F;
                horseLeftSaddleMetal.rotateAngleX = -1.0471976F;
                horseRightSaddleRope.rotateAngleX = -1.0471976F;
                horseRightSaddleMetal.rotateAngleX = -1.0471976F;
                horseLeftSaddleRope.rotateAngleZ = 0.0F;
                horseLeftSaddleMetal.rotateAngleZ = 0.0F;
                horseRightSaddleRope.rotateAngleZ = 0.0F;
                horseRightSaddleMetal.rotateAngleZ = 0.0F;
            } else {
                horseLeftSaddleRope.rotateAngleX = f11 / 3.0F;
                horseLeftSaddleMetal.rotateAngleX = f11 / 3.0F;
                horseRightSaddleRope.rotateAngleX = f11 / 3.0F;
                horseRightSaddleMetal.rotateAngleX = f11 / 3.0F;
                horseLeftSaddleRope.rotateAngleZ = f11 / 5.0F;
                horseLeftSaddleMetal.rotateAngleZ = f11 / 5.0F;
                horseRightSaddleRope.rotateAngleZ = -f11 / 5.0F;
                horseRightSaddleMetal.rotateAngleZ = -f11 / 5.0F;
            }
        }
        float tail = -1.3089969F + limbSwingAmount * 1.5F;
        if (tail > 0.0F) tail = 0.0F;
        if (tailSwing) {
            tailBase.rotateAngleY = MathHelper.cos(f9 * 0.7F);
            tail = 0.0F;
        } else {
            tailBase.rotateAngleY = 0.0F;
        }
        tailMiddle.rotateAngleY = tailBase.rotateAngleY;
        tailTip.rotateAngleY = tailBase.rotateAngleY;
        tailMiddle.rotationPointY = tailBase.rotationPointY;
        tailTip.rotationPointY = tailBase.rotationPointY;
        tailMiddle.rotationPointZ = tailBase.rotationPointZ;
        tailTip.rotationPointZ = tailBase.rotationPointZ;
        tailBase.rotateAngleX = tail;
        tailMiddle.rotateAngleX = tail;
        tailTip.rotateAngleX = -0.2617994F + tail;
    }
}
