package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.AbstractChestHorse;

/** The 1.12 llama (texture 128x64, unchanged in 1.21): long neck and head, four legs, side chests when carrying one. */
public class ModelLlama extends ModelQuadruped {
    private final ModelRenderer chest1;
    private final ModelRenderer chest2;

    public ModelLlama(float scale) {
        super(15, scale);
        textureWidth = 128;
        textureHeight = 64;
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-2.0F, -14.0F, -10.0F, 4, 4, 9, scale);
        head.setRotationPoint(0.0F, 7.0F, -6.0F);
        head.setTextureOffset(0, 14).addBox(-4.0F, -16.0F, -6.0F, 8, 18, 6, scale);
        head.setTextureOffset(17, 0).addBox(-4.0F, -19.0F, -4.0F, 3, 3, 2, scale);
        head.setTextureOffset(17, 0).addBox(1.0F, -19.0F, -4.0F, 3, 3, 2, scale);
        body = new ModelRenderer(this, 29, 0);
        body.addBox(-6.0F, -10.0F, -7.0F, 12, 18, 10, scale);
        body.setRotationPoint(0.0F, 5.0F, 2.0F);
        chest1 = new ModelRenderer(this, 45, 28);
        chest1.addBox(-3.0F, 0.0F, 0.0F, 8, 8, 3, scale);
        chest1.setRotationPoint(-8.5F, 3.0F, 3.0F);
        chest1.rotateAngleY = (float) Math.PI / 2F;
        chest2 = new ModelRenderer(this, 45, 41);
        chest2.addBox(-3.0F, 0.0F, 0.0F, 8, 8, 3, scale);
        chest2.setRotationPoint(5.5F, 3.0F, 3.0F);
        chest2.rotateAngleY = (float) Math.PI / 2F;
        leg1 = leg(scale, -2.5F, 6.0F);
        leg2 = leg(scale, 2.5F, 6.0F);
        leg3 = leg(scale, -2.5F, -4.0F);
        leg4 = leg(scale, 2.5F, -4.0F);
        --leg1.rotationPointX;
        ++leg2.rotationPointX;
        --leg3.rotationPointX;
        ++leg4.rotationPointX;
        --leg3.rotationPointZ;
        --leg4.rotationPointZ;
        childZOffset += 2.0F;
    }

    private ModelRenderer leg(float scale, float x, float z) {
        ModelRenderer leg = new ModelRenderer(this, 29, 29);
        leg.addBox(-2.0F, 0.0F, -2.0F, 4, 14, 4, scale);
        leg.setRotationPoint(x, 10.0F, z);
        return leg;
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        boolean chest = entityIn instanceof AbstractChestHorse horse && !horse.isChild() && horse.hasChest();
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
        if (isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.71428573F, 0.64935064F, 0.7936508F);
            GlStateManager.translate(0.0F, 21.0F * scale, 0.22F);
            head.render(scale);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.625F, 0.45454544F, 0.45454544F);
            GlStateManager.translate(0.0F, 33.0F * scale, 0.0F);
            body.render(scale);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.45454544F, 0.41322312F, 0.45454544F);
            GlStateManager.translate(0.0F, 33.0F * scale, 0.0F);
            leg1.render(scale);
            leg2.render(scale);
            leg3.render(scale);
            leg4.render(scale);
            GlStateManager.popMatrix();
        } else {
            head.render(scale);
            body.render(scale);
            leg1.render(scale);
            leg2.render(scale);
            leg3.render(scale);
            leg4.render(scale);
        }
        if (chest) {
            chest1.render(scale);
            chest2.render(scale);
        }
    }
}
