package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;

public class ModelChicken extends ModelBase {
    public ModelRenderer head;
    public ModelRenderer body;
    public ModelRenderer rightLeg;
    public ModelRenderer leftLeg;
    public ModelRenderer rightWing;
    public ModelRenderer leftWing;
    public ModelRenderer bill;
    public ModelRenderer chin;

    public ModelChicken() {
        byte b = 16;
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-2.0F, -6.0F, -2.0F, 4, 6, 3, 0.0F);
        head.setRotationPoint(0.0F, -1 + b, -4.0F);
        bill = new ModelRenderer(this, 14, 0);
        bill.addBox(-2.0F, -4.0F, -4.0F, 4, 2, 2, 0.0F);
        bill.setRotationPoint(0.0F, -1 + b, -4.0F);
        chin = new ModelRenderer(this, 14, 4);
        chin.addBox(-1.0F, -2.0F, -3.0F, 2, 2, 2, 0.0F);
        chin.setRotationPoint(0.0F, -1 + b, -4.0F);
        body = new ModelRenderer(this, 0, 9);
        body.addBox(-3.0F, -4.0F, -3.0F, 6, 8, 6, 0.0F);
        body.setRotationPoint(0.0F, b, 0.0F);
        rightLeg = new ModelRenderer(this, 26, 0);
        rightLeg.addBox(-1.0F, 0.0F, -3.0F, 3, 5, 3);
        rightLeg.setRotationPoint(-2.0F, 3 + b, 1.0F);
        leftLeg = new ModelRenderer(this, 26, 0);
        leftLeg.addBox(-1.0F, 0.0F, -3.0F, 3, 5, 3);
        leftLeg.setRotationPoint(1.0F, 3 + b, 1.0F);
        rightWing = new ModelRenderer(this, 24, 13);
        rightWing.addBox(0.0F, 0.0F, -3.0F, 1, 4, 6);
        rightWing.setRotationPoint(-4.0F, -3 + b, 0.0F);
        leftWing = new ModelRenderer(this, 24, 13);
        leftWing.addBox(-1.0F, 0.0F, -3.0F, 1, 4, 6);
        leftWing.setRotationPoint(4.0F, -3 + b, 0.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, entity);
        head.render(scale);
        bill.render(scale);
        chin.render(scale);
        body.render(scale);
        rightLeg.render(scale);
        leftLeg.render(scale);
        rightWing.render(scale);
        leftWing.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity) {
        head.rotateAngleX = headPitch / (180F / (float) Math.PI);
        head.rotateAngleY = headYaw / (180F / (float) Math.PI);
        bill.rotateAngleX = head.rotateAngleX;
        bill.rotateAngleY = head.rotateAngleY;
        chin.rotateAngleX = head.rotateAngleX;
        chin.rotateAngleY = head.rotateAngleY;
        body.rotateAngleX = (float) Math.PI / 2F;
        rightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        leftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        rightWing.rotateAngleZ = age;
        leftWing.rotateAngleZ = -age;
    }
}
