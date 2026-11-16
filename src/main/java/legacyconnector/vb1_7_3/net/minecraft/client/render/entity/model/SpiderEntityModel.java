package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta 1.7.3 {@code ModelSpider}: head, neck, abdomen and eight legs with Beta's walking animation. */
public class SpiderEntityModel extends EntityModel {
    public ModelPart head, body0, body1, rightHindLeg, leftHindLeg, rightMiddleHindLeg, leftMiddleHindLeg, rightMiddleFrontLeg, leftMiddleFrontLeg, rightFrontLeg, leftFrontLeg;

    public SpiderEntityModel() {
        float grow = 0.0F;
        int y = 15;
        head = new ModelPart(32, 4); head.addCuboid(-4.0F, -4.0F, -8.0F, 8, 8, 8, grow); head.setPivot(0.0F, y, -3.0F);
        body0 = new ModelPart(0, 0); body0.addCuboid(-3.0F, -3.0F, -3.0F, 6, 6, 6, grow); body0.setPivot(0.0F, y, 0.0F);
        body1 = new ModelPart(0, 12); body1.addCuboid(-5.0F, -4.0F, -6.0F, 10, 8, 12, grow); body1.setPivot(0.0F, y, 9.0F);
        rightHindLeg = leg(-15.0F, -4.0F, y, 2.0F); leftHindLeg = leg(-1.0F, 4.0F, y, 2.0F);
        rightMiddleHindLeg = leg(-15.0F, -4.0F, y, 1.0F); leftMiddleHindLeg = leg(-1.0F, 4.0F, y, 1.0F);
        rightMiddleFrontLeg = leg(-15.0F, -4.0F, y, 0.0F); leftMiddleFrontLeg = leg(-1.0F, 4.0F, y, 0.0F);
        rightFrontLeg = leg(-15.0F, -4.0F, y, -1.0F); leftFrontLeg = leg(-1.0F, 4.0F, y, -1.0F);
    }

    private static ModelPart leg(float boxX, float pivotX, int pivotY, float pivotZ) {
        ModelPart part = new ModelPart(18, 0);
        part.addCuboid(boxX, -1.0F, -1.0F, 16, 2, 2, 0.0F);
        part.setPivot(pivotX, pivotY, pivotZ);
        return part;
    }

    @Override public void render(float limb, float amount, float age, float yaw, float pitch, float scale) {
        setAngles(limb, amount, age, yaw, pitch, scale);
        head.render(scale); body0.render(scale); body1.render(scale);
        rightHindLeg.render(scale); leftHindLeg.render(scale); rightMiddleHindLeg.render(scale); leftMiddleHindLeg.render(scale);
        rightMiddleFrontLeg.render(scale); leftMiddleFrontLeg.render(scale); rightFrontLeg.render(scale); leftFrontLeg.render(scale);
    }

    @Override public void setAngles(float limb, float amount, float age, float yaw, float pitch, float scale) {
        float pi = (float) Math.PI;
        head.yaw = yaw / (180.0F / pi);
        head.pitch = pitch / (180.0F / pi);
        float spread = pi / 4.0F;
        rightHindLeg.roll = -spread; leftHindLeg.roll = spread;
        rightMiddleHindLeg.roll = -spread * 0.74F; leftMiddleHindLeg.roll = spread * 0.74F;
        rightMiddleFrontLeg.roll = -spread * 0.74F; leftMiddleFrontLeg.roll = spread * 0.74F;
        rightFrontLeg.roll = -spread; leftFrontLeg.roll = spread;
        float turn = pi / 8.0F;
        rightHindLeg.yaw = turn * 2.0F; leftHindLeg.yaw = -turn * 2.0F;
        rightMiddleHindLeg.yaw = turn; leftMiddleHindLeg.yaw = -turn;
        rightMiddleFrontLeg.yaw = -turn; leftMiddleFrontLeg.yaw = turn;
        rightFrontLeg.yaw = -turn * 2.0F; leftFrontLeg.yaw = turn * 2.0F;
        float s1 = -(MathHelper.cos(limb * 0.6662F * 2.0F) * 0.4F) * amount;
        float s2 = -(MathHelper.cos(limb * 0.6662F * 2.0F + pi) * 0.4F) * amount;
        float s3 = -(MathHelper.cos(limb * 0.6662F * 2.0F + pi / 2.0F) * 0.4F) * amount;
        float s4 = -(MathHelper.cos(limb * 0.6662F * 2.0F + pi * 3.0F / 2.0F) * 0.4F) * amount;
        float l1 = Math.abs(MathHelper.sin(limb * 0.6662F) * 0.4F) * amount;
        float l2 = Math.abs(MathHelper.sin(limb * 0.6662F + pi) * 0.4F) * amount;
        float l3 = Math.abs(MathHelper.sin(limb * 0.6662F + pi / 2.0F) * 0.4F) * amount;
        float l4 = Math.abs(MathHelper.sin(limb * 0.6662F + pi * 3.0F / 2.0F) * 0.4F) * amount;
        rightHindLeg.yaw += s1; leftHindLeg.yaw += -s1;
        rightMiddleHindLeg.yaw += s2; leftMiddleHindLeg.yaw += -s2;
        rightMiddleFrontLeg.yaw += s3; leftMiddleFrontLeg.yaw += -s3;
        rightFrontLeg.yaw += s4; leftFrontLeg.yaw += -s4;
        rightHindLeg.roll += l1; leftHindLeg.roll += -l1;
        rightMiddleHindLeg.roll += l2; leftMiddleHindLeg.roll += -l2;
        rightMiddleFrontLeg.roll += l3; leftMiddleFrontLeg.roll += -l3;
        rightFrontLeg.roll += l4; leftFrontLeg.roll += -l4;
    }
}
