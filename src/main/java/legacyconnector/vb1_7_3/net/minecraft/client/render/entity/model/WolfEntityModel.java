package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta 1.7.3 {@code ModelWolf}: head with ears and snout, body, mane, legs and tail; {@code age} is the tail angle (Beta's handleRotationFloat). */
public class WolfEntityModel extends EntityModel {
    public ModelPart head, torso, rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg, field_2095, field_2096, field_2097, tail, neck;

    public WolfEntityModel() {
        float headY = 13.5F;
        head = part(0, 0, -3.0F, -3.0F, -2.0F, 6, 6, 4, -1.0F, headY, -7.0F);
        torso = part(18, 14, -4.0F, -2.0F, -3.0F, 6, 9, 6, 0.0F, 14.0F, 2.0F);
        neck = part(21, 0, -4.0F, -3.0F, -3.0F, 8, 6, 7, -1.0F, 14.0F, 2.0F);
        rightHindLeg = part(0, 18, -1.0F, 0.0F, -1.0F, 2, 8, 2, -2.5F, 16.0F, 7.0F);
        leftHindLeg = part(0, 18, -1.0F, 0.0F, -1.0F, 2, 8, 2, 0.5F, 16.0F, 7.0F);
        rightFrontLeg = part(0, 18, -1.0F, 0.0F, -1.0F, 2, 8, 2, -2.5F, 16.0F, -4.0F);
        leftFrontLeg = part(0, 18, -1.0F, 0.0F, -1.0F, 2, 8, 2, 0.5F, 16.0F, -4.0F);
        tail = part(9, 18, -1.0F, 0.0F, -1.0F, 2, 8, 2, -1.0F, 12.0F, 8.0F);
        field_2095 = part(16, 14, -3.0F, -5.0F, 0.0F, 2, 2, 1, -1.0F, headY, -7.0F);
        field_2096 = part(16, 14, 1.0F, -5.0F, 0.0F, 2, 2, 1, -1.0F, headY, -7.0F);
        field_2097 = part(0, 10, -2.0F, 0.0F, -5.0F, 3, 3, 4, -0.5F, headY, -7.0F);
    }

    private static ModelPart part(int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz) {
        ModelPart p = new ModelPart(u, v);
        p.addCuboid(x, y, z, w, h, d, 0.0F);
        p.setPivot(px, py, pz);
        return p;
    }

    @Override public void render(float limb, float amount, float age, float yaw, float pitch, float scale) {
        setAngles(limb, amount, age, yaw, pitch, scale);
        head.render(scale); torso.render(scale);
        rightHindLeg.render(scale); leftHindLeg.render(scale); rightFrontLeg.render(scale); leftFrontLeg.render(scale);
        field_2095.render(scale); field_2096.render(scale); field_2097.render(scale); tail.render(scale); neck.render(scale);
    }

    @Override public void setAngles(float limb, float amount, float age, float yaw, float pitch, float scale) {
        float pi = (float) Math.PI;
        torso.setPivot(0.0F, 14.0F, 2.0F); torso.pitch = pi / 2.0F;
        neck.setPivot(-1.0F, 14.0F, -3.0F); neck.pitch = torso.pitch;
        tail.setPivot(-1.0F, 12.0F, 8.0F);
        rightHindLeg.pitch = MathHelper.cos(limb * 0.6662F) * 1.4F * amount;
        leftHindLeg.pitch = MathHelper.cos(limb * 0.6662F + pi) * 1.4F * amount;
        rightFrontLeg.pitch = MathHelper.cos(limb * 0.6662F + pi) * 1.4F * amount;
        leftFrontLeg.pitch = MathHelper.cos(limb * 0.6662F) * 1.4F * amount;
        head.pitch = pitch / (180.0F / pi);
        head.yaw = yaw / (180.0F / pi);
        for (ModelPart p : new ModelPart[] {field_2095, field_2096, field_2097}) { p.yaw = head.yaw; p.pitch = head.pitch; }
        tail.pitch = age;
    }
}
