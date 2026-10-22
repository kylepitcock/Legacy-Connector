package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

/** 1.7.10 spider model: head, neck, body and eight legs. */
public class ModelSpider extends ModelBase {
    public ModelRenderer spiderHead;
    public ModelRenderer spiderNeck;
    public ModelRenderer spiderBody;
    public ModelRenderer spiderLeg1;
    public ModelRenderer spiderLeg2;
    public ModelRenderer spiderLeg3;
    public ModelRenderer spiderLeg4;
    public ModelRenderer spiderLeg5;
    public ModelRenderer spiderLeg6;
    public ModelRenderer spiderLeg7;
    public ModelRenderer spiderLeg8;

    public ModelSpider() {
        float f = 0.0F;
        int b0 = 15;
        spiderHead = new ModelRenderer(this, 32, 4);
        spiderHead.addBox(-4.0F, -4.0F, -8.0F, 8, 8, 8, f);
        spiderHead.setRotationPoint(0.0F, b0, -3.0F);
        spiderNeck = new ModelRenderer(this, 0, 0);
        spiderNeck.addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6, f);
        spiderNeck.setRotationPoint(0.0F, b0, 0.0F);
        spiderBody = new ModelRenderer(this, 0, 12);
        spiderBody.addBox(-5.0F, -4.0F, -6.0F, 10, 8, 12, f);
        spiderBody.setRotationPoint(0.0F, b0, 9.0F);
        spiderLeg1 = leg(-15.0F, -4.0F, b0, 2.0F);
        spiderLeg2 = leg(-1.0F, 4.0F, b0, 2.0F);
        spiderLeg3 = leg(-15.0F, -4.0F, b0, 1.0F);
        spiderLeg4 = leg(-1.0F, 4.0F, b0, 1.0F);
        spiderLeg5 = leg(-15.0F, -4.0F, b0, 0.0F);
        spiderLeg6 = leg(-1.0F, 4.0F, b0, 0.0F);
        spiderLeg7 = leg(-15.0F, -4.0F, b0, -1.0F);
        spiderLeg8 = leg(-1.0F, 4.0F, b0, -1.0F);
    }

    private ModelRenderer leg(float boxX, float x, int y, float z) {
        ModelRenderer leg = new ModelRenderer(this, 18, 0);
        leg.addBox(boxX, -1.0F, -1.0F, 16, 2, 2, 0.0F);
        leg.setRotationPoint(x, y, z);
        return leg;
    }

    @Override
    public void render(Entity entity, float p2, float p3, float p4, float p5, float p6, float scale) {
        setRotationAngles(p2, p3, p4, p5, p6, scale, entity);
        spiderHead.render(scale);
        spiderNeck.render(scale);
        spiderBody.render(scale);
        spiderLeg1.render(scale);
        spiderLeg2.render(scale);
        spiderLeg3.render(scale);
        spiderLeg4.render(scale);
        spiderLeg5.render(scale);
        spiderLeg6.render(scale);
        spiderLeg7.render(scale);
        spiderLeg8.render(scale);
    }

    @Override
    public void setRotationAngles(float p1, float p2, float p3, float p4, float p5, float scale, Entity entity) {
        spiderHead.rotateAngleY = p4 / (180F / (float) Math.PI);
        spiderHead.rotateAngleX = p5 / (180F / (float) Math.PI);
        float f6 = (float) Math.PI / 4F;
        spiderLeg1.rotateAngleZ = -f6;
        spiderLeg2.rotateAngleZ = f6;
        spiderLeg3.rotateAngleZ = -f6 * 0.74F;
        spiderLeg4.rotateAngleZ = f6 * 0.74F;
        spiderLeg5.rotateAngleZ = -f6 * 0.74F;
        spiderLeg6.rotateAngleZ = f6 * 0.74F;
        spiderLeg7.rotateAngleZ = -f6;
        spiderLeg8.rotateAngleZ = f6;
        float f8 = 0.3926991F;
        spiderLeg1.rotateAngleY = f8 * 2.0F;
        spiderLeg2.rotateAngleY = -f8 * 2.0F;
        spiderLeg3.rotateAngleY = f8;
        spiderLeg4.rotateAngleY = -f8;
        spiderLeg5.rotateAngleY = -f8;
        spiderLeg6.rotateAngleY = f8;
        spiderLeg7.rotateAngleY = -f8 * 2.0F;
        spiderLeg8.rotateAngleY = f8 * 2.0F;
        float pi = (float) Math.PI;
        float f9 = -((float) Math.cos(p1 * 0.6662F * 2.0F + 0.0F) * 0.4F) * p2;
        float f10 = -((float) Math.cos(p1 * 0.6662F * 2.0F + pi) * 0.4F) * p2;
        float f11 = -((float) Math.cos(p1 * 0.6662F * 2.0F + pi / 2.0F) * 0.4F) * p2;
        float f12 = -((float) Math.cos(p1 * 0.6662F * 2.0F + pi * 3.0F / 2.0F) * 0.4F) * p2;
        float f13 = Math.abs((float) Math.sin(p1 * 0.6662F + 0.0F) * 0.4F) * p2;
        float f14 = Math.abs((float) Math.sin(p1 * 0.6662F + pi) * 0.4F) * p2;
        float f15 = Math.abs((float) Math.sin(p1 * 0.6662F + pi / 2.0F) * 0.4F) * p2;
        float f16 = Math.abs((float) Math.sin(p1 * 0.6662F + pi * 3.0F / 2.0F) * 0.4F) * p2;
        spiderLeg1.rotateAngleY += f9;
        spiderLeg2.rotateAngleY += -f9;
        spiderLeg3.rotateAngleY += f10;
        spiderLeg4.rotateAngleY += -f10;
        spiderLeg5.rotateAngleY += f11;
        spiderLeg6.rotateAngleY += -f11;
        spiderLeg7.rotateAngleY += f12;
        spiderLeg8.rotateAngleY += -f12;
        spiderLeg1.rotateAngleZ += f13;
        spiderLeg2.rotateAngleZ += -f13;
        spiderLeg3.rotateAngleZ += f14;
        spiderLeg4.rotateAngleZ += -f14;
        spiderLeg5.rotateAngleZ += f15;
        spiderLeg6.rotateAngleZ += -f15;
        spiderLeg7.rotateAngleZ += f16;
        spiderLeg8.rotateAngleZ += -f16;
    }
}
