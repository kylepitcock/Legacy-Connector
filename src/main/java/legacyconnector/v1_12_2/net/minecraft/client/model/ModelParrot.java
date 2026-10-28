package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/** 1.12.2 parrot model (32x32 texture), boxes as vanilla's: standing, flying, sitting and dancing poses; the age argument is the wing flap. */
public class ModelParrot extends ModelBase {
    ModelRenderer body;
    ModelRenderer tail;
    ModelRenderer wingLeft;
    ModelRenderer wingRight;
    ModelRenderer head;
    ModelRenderer head2;
    ModelRenderer beak1;
    ModelRenderer beak2;
    ModelRenderer feather;
    ModelRenderer legLeft;
    ModelRenderer legRight;
    private State state = State.STANDING;

    public ModelParrot() {
        textureWidth = 32;
        textureHeight = 32;
        body = new ModelRenderer(this, 2, 8);
        body.addBox(-1.5F, 0.0F, -1.5F, 3, 6, 3);
        body.setRotationPoint(0.0F, 16.5F, -3.0F);
        tail = new ModelRenderer(this, 22, 1);
        tail.addBox(-1.5F, -1.0F, -1.0F, 3, 4, 1);
        tail.setRotationPoint(0.0F, 21.07F, 1.16F);
        wingLeft = new ModelRenderer(this, 19, 8);
        wingLeft.addBox(-0.5F, 0.0F, -1.5F, 1, 5, 3);
        wingLeft.setRotationPoint(1.5F, 16.94F, -2.76F);
        wingRight = new ModelRenderer(this, 19, 8);
        wingRight.addBox(-0.5F, 0.0F, -1.5F, 1, 5, 3);
        wingRight.setRotationPoint(-1.5F, 16.94F, -2.76F);
        head = new ModelRenderer(this, 2, 2);
        head.addBox(-1.0F, -1.5F, -1.0F, 2, 3, 2);
        head.setRotationPoint(0.0F, 15.69F, -2.76F);
        head2 = new ModelRenderer(this, 10, 0);
        head2.addBox(-1.0F, -0.5F, -2.0F, 2, 1, 4);
        head2.setRotationPoint(0.0F, -2.0F, -1.0F);
        head.addChild(head2);
        beak1 = new ModelRenderer(this, 11, 7);
        beak1.addBox(-0.5F, -1.0F, -0.5F, 1, 2, 1);
        beak1.setRotationPoint(0.0F, -0.5F, -1.5F);
        head.addChild(beak1);
        beak2 = new ModelRenderer(this, 16, 7);
        beak2.addBox(-0.5F, 0.0F, -0.5F, 1, 2, 1);
        beak2.setRotationPoint(0.0F, -1.75F, -2.45F);
        head.addChild(beak2);
        feather = new ModelRenderer(this, 2, 18);
        feather.addBox(0.0F, -4.0F, -2.0F, 0, 5, 4);
        feather.setRotationPoint(0.0F, -2.15F, 0.15F);
        head.addChild(feather);
        legLeft = new ModelRenderer(this, 14, 18);
        legLeft.addBox(-0.5F, 0.0F, -0.5F, 1, 2, 1);
        legLeft.setRotationPoint(1.0F, 22.0F, -1.05F);
        legRight = new ModelRenderer(this, 14, 18);
        legRight.addBox(-0.5F, 0.0F, -0.5F, 1, 2, 1);
        legRight.setRotationPoint(-1.0F, 22.0F, -1.05F);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        body.render(scale);
        wingLeft.render(scale);
        wingRight.render(scale);
        tail.render(scale);
        head.render(scale);
        legLeft.render(scale);
        legRight.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        int tickCount = entityIn == null ? 0 : Entity.mcc$get$ticksExisted(entityIn);
        head.rotateAngleX = headPitch * 0.017453292F;
        head.rotateAngleY = netHeadYaw * 0.017453292F;
        head.rotateAngleZ = 0.0F;
        head.rotationPointX = 0.0F;
        body.rotationPointX = 0.0F;
        tail.rotationPointX = 0.0F;
        wingRight.rotationPointX = -1.5F;
        wingLeft.rotationPointX = 1.5F;
        switch (state) {
            case SITTING -> {
            }
            case PARTY -> {
                float f = MathHelper.cos((float) tickCount);
                float f1 = MathHelper.sin((float) tickCount);
                head.rotationPointX = f;
                head.rotationPointY = 15.69F + f1;
                head.rotateAngleX = 0.0F;
                head.rotateAngleY = 0.0F;
                head.rotateAngleZ = MathHelper.sin((float) tickCount) * 0.4F;
                body.rotationPointX = f;
                body.rotationPointY = 16.5F + f1;
                wingLeft.rotateAngleZ = -0.0873F - ageInTicks;
                wingLeft.rotationPointX = 1.5F + f;
                wingLeft.rotationPointY = 16.94F + f1;
                wingRight.rotateAngleZ = 0.0873F + ageInTicks;
                wingRight.rotationPointX = -1.5F + f;
                wingRight.rotationPointY = 16.94F + f1;
                tail.rotationPointX = f;
                tail.rotationPointY = 21.07F + f1;
            }
            default -> {
                if (state == State.STANDING) {
                    legLeft.rotateAngleX += MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
                    legRight.rotateAngleX += MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
                }
                float f2 = ageInTicks * 0.3F;
                head.rotationPointY = 15.69F + f2;
                tail.rotateAngleX = 1.015F + MathHelper.cos(limbSwing * 0.6662F) * 0.3F * limbSwingAmount;
                tail.rotationPointY = 21.07F + f2;
                body.rotationPointY = 16.5F + f2;
                wingLeft.rotateAngleZ = -0.0873F - ageInTicks;
                wingLeft.rotationPointY = 16.94F + f2;
                wingRight.rotateAngleZ = 0.0873F + ageInTicks;
                wingRight.rotationPointY = 16.94F + f2;
                legLeft.rotationPointY = 22.0F + f2;
                legRight.rotationPointY = 22.0F + f2;
            }
        }
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
        var parrot = entitylivingbaseIn != null && entitylivingbaseIn.mcc$entity instanceof net.minecraft.world.entity.animal.Parrot p ? p : null;
        if (parrot == null) state = State.STANDING;
        else if (parrot.isPartyParrot()) state = State.PARTY;
        else if (parrot.isInSittingPose()) state = State.SITTING;
        else if (parrot.isFlying()) state = State.FLYING;
        else state = State.STANDING;
        feather.rotateAngleX = -0.2214F;
        body.rotateAngleX = 0.4937F;
        wingLeft.rotateAngleX = -0.6981F;
        wingLeft.rotateAngleY = -(float) Math.PI;
        wingRight.rotateAngleX = -0.6981F;
        wingRight.rotateAngleY = -(float) Math.PI;
        legLeft.rotateAngleX = -0.0299F;
        legRight.rotateAngleX = -0.0299F;
        legLeft.rotationPointY = 22.0F;
        legRight.rotationPointY = 22.0F;
        legLeft.rotateAngleZ = 0.0F;
        legRight.rotateAngleZ = 0.0F;
        switch (state) {
            case FLYING -> {
                legLeft.rotateAngleX += ((float) Math.PI * 2F / 9F);
                legRight.rotateAngleX += ((float) Math.PI * 2F / 9F);
            }
            case SITTING -> {
                head.rotationPointY = 17.59F;
                tail.rotateAngleX = 1.5388988F;
                tail.rotationPointY = 22.97F;
                body.rotationPointY = 18.4F;
                wingLeft.rotateAngleZ = -0.0873F;
                wingLeft.rotationPointY = 18.84F;
                wingRight.rotateAngleZ = 0.0873F;
                wingRight.rotationPointY = 18.84F;
                ++legLeft.rotationPointY;
                ++legRight.rotationPointY;
                ++legLeft.rotateAngleX;
                ++legRight.rotateAngleX;
            }
            case PARTY -> {
                legLeft.rotateAngleZ = -0.34906584F;
                legRight.rotateAngleZ = 0.34906584F;
            }
            default -> {
            }
        }
    }

    enum State {
        FLYING, STANDING, SITTING, PARTY
    }
}
