package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** Humanoid model: head, hat layer, body, arms and legs on the 64x32 player/zombie layout. */
public class ModelBiped extends ModelBase {
    public ModelRenderer bipedHead;
    public ModelRenderer bipedHeadwear;
    public ModelRenderer bipedBody;
    public ModelRenderer bipedRightArm;
    public ModelRenderer bipedLeftArm;
    public ModelRenderer bipedRightLeg;
    public ModelRenderer bipedLeftLeg;
    public ModelRenderer bipedEars;
    public ModelRenderer bipedCloak;
    public int heldItemLeft;
    public int heldItemRight;
    public boolean isSneak;
    public boolean aimedBow;

    public ModelBiped() {
        this(0.0F);
    }

    public ModelBiped(float grow) {
        this(grow, 0.0F, 64, 32);
    }

    public ModelBiped(float grow, float yOffset, int textureWidth, int textureHeight) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        bipedCloak = new ModelRenderer(this, 0, 0);
        bipedCloak.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1, grow);
        bipedEars = new ModelRenderer(this, 24, 0);
        bipedEars.addBox(-3.0F, -6.0F, -1.0F, 6, 6, 1, grow);
        bipedHead = new ModelRenderer(this, 0, 0);
        bipedHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, grow);
        bipedHead.setRotationPoint(0.0F, yOffset, 0.0F);
        bipedHeadwear = new ModelRenderer(this, 32, 0);
        bipedHeadwear.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, grow + 0.5F);
        bipedHeadwear.setRotationPoint(0.0F, yOffset, 0.0F);
        bipedBody = new ModelRenderer(this, 16, 16);
        bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, grow);
        bipedBody.setRotationPoint(0.0F, yOffset, 0.0F);
        bipedRightArm = new ModelRenderer(this, 40, 16);
        bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, grow);
        bipedRightArm.setRotationPoint(-5.0F, 2.0F + yOffset, 0.0F);
        bipedLeftArm = new ModelRenderer(this, 40, 16);
        bipedLeftArm.mirror = true;
        bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, grow);
        bipedLeftArm.setRotationPoint(5.0F, 2.0F + yOffset, 0.0F);
        bipedRightLeg = new ModelRenderer(this, 0, 16);
        bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, grow);
        bipedRightLeg.setRotationPoint(-1.9F, 12.0F + yOffset, 0.0F);
        bipedLeftLeg = new ModelRenderer(this, 0, 16);
        bipedLeftLeg.mirror = true;
        bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, grow);
        bipedLeftLeg.setRotationPoint(1.9F, 12.0F + yOffset, 0.0F);
        // The ears and cape are drawn by their own code paths, never by render().
        boxList.remove(bipedCloak);
        boxList.remove(bipedEars);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, entity);
        if (isChild) {
            RenderContext.push();
            RenderContext.scale(0.75F, 0.75F, 0.75F);
            RenderContext.translate(0.0F, 16.0F * scale, 0.0F);
            bipedHead.render(scale);
            RenderContext.pop();
            RenderContext.push();
            RenderContext.scale(0.5F, 0.5F, 0.5F);
            RenderContext.translate(0.0F, 24.0F * scale, 0.0F);
            renderBody(scale);
            bipedHeadwear.render(scale);
            RenderContext.pop();
        } else {
            bipedHead.render(scale);
            renderBody(scale);
            bipedHeadwear.render(scale);
        }
    }

    private void renderBody(float scale) {
        bipedBody.render(scale);
        bipedRightArm.render(scale);
        bipedLeftArm.render(scale);
        bipedRightLeg.render(scale);
        bipedLeftLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity) {
        final float deg = (float) (180.0 / Math.PI);
        final float pi = (float) Math.PI;
        bipedHead.rotateAngleY = headYaw / deg;
        bipedHead.rotateAngleX = headPitch / deg;
        bipedHeadwear.rotateAngleY = bipedHead.rotateAngleY;
        bipedHeadwear.rotateAngleX = bipedHead.rotateAngleX;
        bipedRightArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + pi) * 2.0F * limbSwingAmount * 0.5F;
        bipedLeftArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        bipedRightArm.rotateAngleZ = 0.0F;
        bipedLeftArm.rotateAngleZ = 0.0F;
        bipedRightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        bipedLeftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + pi) * 1.4F * limbSwingAmount;
        bipedRightLeg.rotateAngleY = 0.0F;
        bipedLeftLeg.rotateAngleY = 0.0F;
        if (isRiding) {
            bipedRightArm.rotateAngleX -= pi / 5.0F;
            bipedLeftArm.rotateAngleX -= pi / 5.0F;
            bipedRightLeg.rotateAngleX = -pi * 2.0F / 5.0F;
            bipedLeftLeg.rotateAngleX = -pi * 2.0F / 5.0F;
            bipedRightLeg.rotateAngleY = pi / 10.0F;
            bipedLeftLeg.rotateAngleY = -pi / 10.0F;
        }
        if (heldItemLeft != 0) bipedLeftArm.rotateAngleX = bipedLeftArm.rotateAngleX * 0.5F - pi / 10.0F * heldItemLeft;
        if (heldItemRight != 0) bipedRightArm.rotateAngleX = bipedRightArm.rotateAngleX * 0.5F - pi / 10.0F * heldItemRight;
        bipedRightArm.rotateAngleY = 0.0F;
        bipedLeftArm.rotateAngleY = 0.0F;
        if (onGround > -9990.0F) {
            // Attack swing: the body twists and the right arm swings down.
            float swing = onGround;
            bipedBody.rotateAngleY = MathHelper.sin(MathHelper.sqrt_float(swing) * pi * 2.0F) * 0.2F;
            bipedRightArm.rotationPointZ = MathHelper.sin(bipedBody.rotateAngleY) * 5.0F;
            bipedRightArm.rotationPointX = -MathHelper.cos(bipedBody.rotateAngleY) * 5.0F;
            bipedLeftArm.rotationPointZ = -MathHelper.sin(bipedBody.rotateAngleY) * 5.0F;
            bipedLeftArm.rotationPointX = MathHelper.cos(bipedBody.rotateAngleY) * 5.0F;
            bipedRightArm.rotateAngleY += bipedBody.rotateAngleY;
            bipedLeftArm.rotateAngleY += bipedBody.rotateAngleY;
            bipedLeftArm.rotateAngleX += bipedBody.rotateAngleY;
            float s = 1.0F - swing;
            s = 1.0F - s * s * s * s;
            float a = MathHelper.sin(s * pi);
            float b = MathHelper.sin(swing * pi) * -(bipedHead.rotateAngleX - 0.7F) * 0.75F;
            bipedRightArm.rotateAngleX -= a * 1.2F + b;
            bipedRightArm.rotateAngleY += bipedBody.rotateAngleY * 2.0F;
            bipedRightArm.rotateAngleZ = MathHelper.sin(swing * pi) * -0.4F;
        }
        if (isSneak) {
            bipedBody.rotateAngleX = 0.5F;
            bipedRightArm.rotateAngleX += 0.4F;
            bipedLeftArm.rotateAngleX += 0.4F;
            bipedRightLeg.rotationPointZ = 4.0F;
            bipedLeftLeg.rotationPointZ = 4.0F;
            bipedRightLeg.rotationPointY = 9.0F;
            bipedLeftLeg.rotationPointY = 9.0F;
            bipedHead.rotationPointY = 1.0F;
            bipedHeadwear.rotationPointY = 1.0F;
        } else {
            bipedBody.rotateAngleX = 0.0F;
            bipedRightLeg.rotationPointZ = 0.1F;
            bipedLeftLeg.rotationPointZ = 0.1F;
            bipedRightLeg.rotationPointY = 12.0F;
            bipedLeftLeg.rotationPointY = 12.0F;
            bipedHead.rotationPointY = 0.0F;
            bipedHeadwear.rotationPointY = 0.0F;
        }
        // Idle breathing sway.
        bipedRightArm.rotateAngleZ += MathHelper.cos(age * 0.09F) * 0.05F + 0.05F;
        bipedLeftArm.rotateAngleZ -= MathHelper.cos(age * 0.09F) * 0.05F + 0.05F;
        bipedRightArm.rotateAngleX += MathHelper.sin(age * 0.067F) * 0.05F;
        bipedLeftArm.rotateAngleX -= MathHelper.sin(age * 0.067F) * 0.05F;
        if (aimedBow) {
            bipedRightArm.rotateAngleZ = 0.0F;
            bipedLeftArm.rotateAngleZ = 0.0F;
            bipedRightArm.rotateAngleY = -0.1F + bipedHead.rotateAngleY;
            bipedLeftArm.rotateAngleY = 0.1F + bipedHead.rotateAngleY + 0.4F;
            bipedRightArm.rotateAngleX = -pi / 2.0F + bipedHead.rotateAngleX;
            bipedLeftArm.rotateAngleX = -pi / 2.0F + bipedHead.rotateAngleX;
        }
    }

    public void renderEars(float scale) {
        bipedEars.rotateAngleY = bipedHead.rotateAngleY;
        bipedEars.rotateAngleX = bipedHead.rotateAngleX;
        bipedEars.rotationPointX = 0.0F;
        bipedEars.rotationPointY = 0.0F;
        bipedEars.render(scale);
    }

    public void renderCloak(float scale) {
        bipedCloak.render(scale);
    }
}
