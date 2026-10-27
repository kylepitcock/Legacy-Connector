package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** The witch model: a villager with a pointed hat and a mole. */
public class ModelWitch extends ModelVillager {
    public boolean holdingItem;
    private final ModelRenderer mole = new ModelRenderer(this).setTextureSize(64, 128);
    private final ModelRenderer witchHat;

    public ModelWitch(float scale) {
        super(scale, 0.0F, 64, 128);
        mole.setRotationPoint(0.0F, -2.0F, 0.0F);
        mole.setTextureOffset(0, 0).addBox(0.0F, 3.0F, -6.75F, 1, 1, 1, -0.25F);
        villagerNose.addChild(mole);
        witchHat = new ModelRenderer(this).setTextureSize(64, 128);
        witchHat.setRotationPoint(-5.0F, -10.03125F, -5.0F);
        witchHat.setTextureOffset(0, 64).addBox(0.0F, 0.0F, 0.0F, 10, 2, 10);
        villagerHead.addChild(witchHat);
        ModelRenderer hat2 = new ModelRenderer(this).setTextureSize(64, 128);
        hat2.setRotationPoint(1.75F, -4.0F, 2.0F);
        hat2.setTextureOffset(0, 76).addBox(0.0F, 0.0F, 0.0F, 7, 4, 7);
        hat2.rotateAngleX = -0.05235988F;
        hat2.rotateAngleZ = 0.02617994F;
        witchHat.addChild(hat2);
        ModelRenderer hat3 = new ModelRenderer(this).setTextureSize(64, 128);
        hat3.setRotationPoint(1.75F, -4.0F, 2.0F);
        hat3.setTextureOffset(0, 87).addBox(0.0F, 0.0F, 0.0F, 4, 4, 4);
        hat3.rotateAngleX = -0.10471976F;
        hat3.rotateAngleZ = 0.05235988F;
        hat2.addChild(hat3);
        ModelRenderer hat4 = new ModelRenderer(this).setTextureSize(64, 128);
        hat4.setRotationPoint(1.75F, -2.0F, 2.0F);
        hat4.setTextureOffset(0, 95).addBox(0.0F, 0.0F, 0.0F, 1, 2, 1, 0.25F);
        hat4.rotateAngleX = -0.20943952F;
        hat4.rotateAngleZ = 0.10471976F;
        hat3.addChild(hat4);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, entity);
        villagerNose.offsetX = villagerNose.offsetY = villagerNose.offsetZ = 0.0F;
        villagerNose.rotationPointX = 0.0F;
        villagerNose.rotationPointZ = 0.0F;
        if (holdingItem) {
            villagerNose.rotateAngleZ = 0.0F;
            villagerNose.offsetY = -0.0625F;
            villagerNose.offsetZ = 0.0625F;
        }
    }
}
