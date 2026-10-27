package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** 1.12.2 shulker model: base, lid and head (64x64 texture). The lid's opening is the entity's, so it stays closed here. */
public class ModelShulker extends ModelBase {
    public final ModelRenderer base;
    public final ModelRenderer lid;
    public ModelRenderer head;

    public ModelShulker() {
        textureHeight = 64;
        textureWidth = 64;
        lid = new ModelRenderer(this);
        base = new ModelRenderer(this);
        head = new ModelRenderer(this);
        lid.setTextureOffset(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16, 12, 16);
        lid.setRotationPoint(0.0F, 24.0F, 0.0F);
        base.setTextureOffset(0, 28).addBox(-8.0F, -8.0F, -8.0F, 16, 8, 16);
        base.setRotationPoint(0.0F, 24.0F, 0.0F);
        head.setTextureOffset(0, 52).addBox(-3.0F, 0.0F, -3.0F, 6, 6, 6);
        head.setRotationPoint(0.0F, 12.0F, 0.0F);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        head.rotateAngleX = headPitch * 0.017453292F;
        head.rotateAngleY = netHeadYaw * 0.017453292F;
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        base.render(scale);
        lid.render(scale);
    }
}
