package mccompat.v1_7_10.net.minecraft.client.model;

import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.util.MathHelper;

public class ModelCreeper extends ModelBase {
    public ModelRenderer head;
    public ModelRenderer field_78133_b;
    public ModelRenderer body;
    public ModelRenderer leg1;
    public ModelRenderer leg2;
    public ModelRenderer leg3;
    public ModelRenderer leg4;

    public ModelCreeper() {
        this(0.0F);
    }

    public ModelCreeper(float grow) {
        int yOffset = 4;
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, grow);
        head.setRotationPoint(0.0F, yOffset, 0.0F);
        field_78133_b = new ModelRenderer(this, 32, 0);
        field_78133_b.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, grow + 0.5F);
        field_78133_b.setRotationPoint(0.0F, yOffset, 0.0F);
        body = new ModelRenderer(this, 16, 16);
        body.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, grow);
        body.setRotationPoint(0.0F, yOffset, 0.0F);
        leg1 = new ModelRenderer(this, 0, 16);
        leg1.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, grow);
        leg1.setRotationPoint(-2.0F, 12 + yOffset, 4.0F);
        leg2 = new ModelRenderer(this, 0, 16);
        leg2.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, grow);
        leg2.setRotationPoint(2.0F, 12 + yOffset, 4.0F);
        leg3 = new ModelRenderer(this, 0, 16);
        leg3.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, grow);
        leg3.setRotationPoint(-2.0F, 12 + yOffset, -4.0F);
        leg4 = new ModelRenderer(this, 0, 16);
        leg4.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, grow);
        leg4.setRotationPoint(2.0F, 12 + yOffset, -4.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, entity);
        head.render(scale);
        body.render(scale);
        leg1.render(scale);
        leg2.render(scale);
        leg3.render(scale);
        leg4.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity) {
        head.rotateAngleY = headYaw / (180F / (float) Math.PI);
        head.rotateAngleX = headPitch / (180F / (float) Math.PI);
        leg1.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        leg2.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        leg3.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        leg4.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
    }
}
