package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** 1.7.10 enchanting-table book model (also the base of mods' tome models). */
public class ModelBook extends ModelBase {
    public ModelRenderer coverRight = new ModelRenderer(this).setTextureOffset(0, 0);
    public ModelRenderer coverLeft = new ModelRenderer(this).setTextureOffset(16, 0);
    public ModelRenderer pagesRight = new ModelRenderer(this).setTextureOffset(0, 10);
    public ModelRenderer pagesLeft = new ModelRenderer(this).setTextureOffset(12, 10);
    public ModelRenderer flippingPageRight = new ModelRenderer(this).setTextureOffset(24, 10);
    public ModelRenderer flippingPageLeft = new ModelRenderer(this).setTextureOffset(24, 10);
    public ModelRenderer bookSpine = new ModelRenderer(this).setTextureOffset(12, 0);

    public ModelBook() {
        coverRight.addBox(-6.0F, -5.0F, 0.0F, 6, 10, 0);
        coverRight.addBox(-6.0F, -5.0F, 0.0F, 6, 10, 0);
        coverLeft.addBox(0.0F, -5.0F, 0.0F, 6, 10, 0);
        bookSpine.addBox(-1.0F, -5.0F, 0.0F, 2, 10, 0);
        pagesRight.addBox(0.0F, -4.0F, -0.99F, 5, 8, 1);
        pagesLeft.addBox(0.0F, -4.0F, -0.01F, 5, 8, 1);
        flippingPageRight.addBox(0.0F, -4.0F, 0.0F, 5, 8, 0);
        flippingPageLeft.addBox(0.0F, -4.0F, 0.0F, 5, 8, 0);
        coverRight.setRotationPoint(0.0F, 0.0F, -1.0F);
        coverLeft.setRotationPoint(0.0F, 0.0F, 1.0F);
        bookSpine.rotateAngleY = (float) Math.PI / 2F;
    }

    @Override
    public void render(Entity entity, float p2, float p3, float p4, float p5, float p6, float scale) {
        setRotationAngles(p2, p3, p4, p5, p6, scale, entity);
        coverRight.render(scale);
        coverLeft.render(scale);
        bookSpine.render(scale);
        pagesRight.render(scale);
        pagesLeft.render(scale);
        flippingPageRight.render(scale);
        flippingPageLeft.render(scale);
    }

    @Override
    public void setRotationAngles(float p1, float p2, float p3, float p4, float p5, float scale, Entity entity) {
        float f6 = ((float) Math.sin(p1 * 0.02F) * 0.1F + 1.25F) * p4;
        coverRight.rotateAngleY = (float) Math.PI + f6;
        coverLeft.rotateAngleY = -f6;
        pagesRight.rotateAngleY = f6;
        pagesLeft.rotateAngleY = -f6;
        flippingPageRight.rotateAngleY = f6 - f6 * 2.0F * p2;
        flippingPageLeft.rotateAngleY = f6 - f6 * 2.0F * p3;
        float x = (float) Math.sin(f6);
        pagesRight.rotationPointX = x;
        pagesLeft.rotationPointX = x;
        flippingPageRight.rotationPointX = x;
        flippingPageLeft.rotationPointX = x;
    }
}
