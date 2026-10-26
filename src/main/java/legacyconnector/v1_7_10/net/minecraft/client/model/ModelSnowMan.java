package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;

public class ModelSnowMan extends ModelBase {
    public ModelRenderer body;
    public ModelRenderer bottomBody;
    public ModelRenderer head;
    public ModelRenderer rightHand;
    public ModelRenderer leftHand;

    public ModelSnowMan() {
        float f = 4.0F;
        float f1 = 0.0F;
        this.head = new ModelRenderer(this, 0, 0).setTextureSize(64, 64);
        this.head.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, f1 - 0.5F);
        this.head.setRotationPoint(0.0F, 0.0F + f, 0.0F);
        this.rightHand = new ModelRenderer(this, 32, 0).setTextureSize(64, 64);
        this.rightHand.addBox(-1.0F, 0.0F, -1.0F, 12, 2, 2, f1 - 0.5F);
        this.rightHand.setRotationPoint(0.0F, 0.0F + f + 9.0F - 7.0F, 0.0F);
        this.leftHand = new ModelRenderer(this, 32, 0).setTextureSize(64, 64);
        this.leftHand.addBox(-1.0F, 0.0F, -1.0F, 12, 2, 2, f1 - 0.5F);
        this.leftHand.setRotationPoint(0.0F, 0.0F + f + 9.0F - 7.0F, 0.0F);
        this.body = new ModelRenderer(this, 0, 16).setTextureSize(64, 64);
        this.body.addBox(-5.0F, -10.0F, -5.0F, 10, 10, 10, f1 - 0.5F);
        this.body.setRotationPoint(0.0F, 0.0F + f + 9.0F, 0.0F);
        this.bottomBody = new ModelRenderer(this, 0, 36).setTextureSize(64, 64);
        this.bottomBody.addBox(-6.0F, -12.0F, -6.0F, 12, 12, 12, f1 - 0.5F);
        this.bottomBody.setRotationPoint(0.0F, 0.0F + f + 20.0F, 0.0F);
    }

    @Override
    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        this.head.rotateAngleY = f3 / (180.0F / (float)Math.PI);
        this.head.rotateAngleX = f4 / (180.0F / (float)Math.PI);
        this.body.rotateAngleY = f3 / (180.0F / (float)Math.PI) * 0.25F;
        float f6 = MathHelper.sin(this.body.rotateAngleY);
        float f7 = MathHelper.cos(this.body.rotateAngleY);
        this.rightHand.rotateAngleZ = 1.0F;
        this.leftHand.rotateAngleZ = -1.0F;
        this.rightHand.rotateAngleY = 0.0F + this.body.rotateAngleY;
        this.leftHand.rotateAngleY = (float) Math.PI + this.body.rotateAngleY;
        this.rightHand.rotationPointX = f7 * 5.0F;
        this.rightHand.rotationPointZ = -f6 * 5.0F;
        this.leftHand.rotationPointX = -f7 * 5.0F;
        this.leftHand.rotationPointZ = f6 * 5.0F;
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        this.body.render(f5);
        this.bottomBody.render(f5);
        this.head.render(f5);
        this.rightHand.render(f5);
        this.leftHand.render(f5);
    }
}
