package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** 1.12.2 polar bear model (128x64 texture), boxes as vanilla's: a quadruped that rears up on its hind legs. */
public class ModelPolarBear extends ModelQuadruped {
    public ModelPolarBear() {
        super(12, 0.0F);
        textureWidth = 128;
        textureHeight = 64;
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-3.5F, -3.0F, -3.0F, 7, 7, 7, 0.0F);
        head.setRotationPoint(0.0F, 10.0F, -16.0F);
        head.setTextureOffset(0, 44).addBox(-2.5F, 1.0F, -6.0F, 5, 3, 3, 0.0F);
        head.setTextureOffset(26, 0).addBox(-4.5F, -4.0F, -1.0F, 2, 2, 1, 0.0F);
        ModelRenderer ear = head.setTextureOffset(26, 0);
        ear.mirror = true;
        ear.addBox(2.5F, -4.0F, -1.0F, 2, 2, 1, 0.0F);
        ear.mirror = false;
        body = new ModelRenderer(this);
        body.setTextureOffset(0, 19).addBox(-5.0F, -13.0F, -7.0F, 14, 14, 11, 0.0F);
        body.setTextureOffset(39, 0).addBox(-4.0F, -25.0F, -7.0F, 12, 12, 10, 0.0F);
        body.setRotationPoint(-2.0F, 9.0F, 12.0F);
        leg1 = new ModelRenderer(this, 50, 22);
        leg1.addBox(-2.0F, 0.0F, -2.0F, 4, 10, 8, 0.0F);
        leg1.setRotationPoint(-4.5F, 14.0F, 6.0F);
        leg2 = new ModelRenderer(this, 50, 22);
        leg2.addBox(-2.0F, 0.0F, -2.0F, 4, 10, 8, 0.0F);
        leg2.setRotationPoint(4.5F, 14.0F, 6.0F);
        leg3 = new ModelRenderer(this, 50, 40);
        leg3.addBox(-2.0F, 0.0F, -2.0F, 4, 10, 6, 0.0F);
        leg3.setRotationPoint(-3.5F, 14.0F, -8.0F);
        leg4 = new ModelRenderer(this, 50, 40);
        leg4.addBox(-2.0F, 0.0F, -2.0F, 4, 10, 6, 0.0F);
        leg4.setRotationPoint(3.5F, 14.0F, -8.0F);
        childYOffset = 16.0F;
        childZOffset = 4.0F;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        float partial = entityIn == null ? 0.0F : ageInTicks - (float) Entity.mcc$get$ticksExisted(entityIn);
        float stand = entityIn != null && entityIn.mcc$entity instanceof net.minecraft.world.entity.animal.PolarBear bear ? bear.getStandingAnimationScale(partial) : 0.0F;
        stand = stand * stand;
        float rest = 1.0F - stand;
        body.rotateAngleX = ((float) Math.PI / 2F) - stand * (float) Math.PI * 0.35F;
        body.rotationPointY = 9.0F * rest + 11.0F * stand;
        leg3.rotationPointY = 14.0F * rest - 6.0F * stand;
        leg3.rotationPointZ = -8.0F * rest - 4.0F * stand;
        leg3.rotateAngleX -= stand * (float) Math.PI * 0.45F;
        leg4.rotationPointY = leg3.rotationPointY;
        leg4.rotationPointZ = leg3.rotationPointZ;
        leg4.rotateAngleX -= stand * (float) Math.PI * 0.45F;
        head.rotationPointY = 10.0F * rest - 12.0F * stand;
        head.rotationPointZ = -16.0F * rest - 3.0F * stand;
        head.rotateAngleX += stand * (float) Math.PI * 0.15F;
    }
}
