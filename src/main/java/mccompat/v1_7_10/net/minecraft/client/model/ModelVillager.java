package mccompat.v1_7_10.net.minecraft.client.model;

import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.util.MathHelper;

/** The villager model (the base of witches and mods' villager-shaped mobs). */
public class ModelVillager extends ModelBase {
    public ModelRenderer villagerHead;
    public ModelRenderer villagerBody;
    public ModelRenderer villagerArms;
    public ModelRenderer rightVillagerLeg;
    public ModelRenderer leftVillagerLeg;
    public ModelRenderer villagerNose;

    public ModelVillager(float scale) {
        this(scale, 0.0F, 64, 64);
    }

    public ModelVillager(float scale, float yOffset, int texWidth, int texHeight) {
        villagerHead = new ModelRenderer(this).setTextureSize(texWidth, texHeight);
        villagerHead.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
        villagerHead.setTextureOffset(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, scale);
        villagerNose = new ModelRenderer(this).setTextureSize(texWidth, texHeight);
        villagerNose.setRotationPoint(0.0F, yOffset - 2.0F, 0.0F);
        villagerNose.setTextureOffset(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2, 4, 2, scale);
        villagerHead.addChild(villagerNose);
        villagerBody = new ModelRenderer(this).setTextureSize(texWidth, texHeight);
        villagerBody.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
        villagerBody.setTextureOffset(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6, scale);
        villagerBody.setTextureOffset(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8, 18, 6, scale + 0.5F);
        villagerArms = new ModelRenderer(this).setTextureSize(texWidth, texHeight);
        villagerArms.setRotationPoint(0.0F, 0.0F + yOffset + 2.0F, 0.0F);
        villagerArms.setTextureOffset(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4, 8, 4, scale);
        villagerArms.setTextureOffset(44, 22).addBox(4.0F, -2.0F, -2.0F, 4, 8, 4, scale);
        villagerArms.setTextureOffset(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8, 4, 4, scale);
        rightVillagerLeg = new ModelRenderer(this, 0, 22).setTextureSize(texWidth, texHeight);
        rightVillagerLeg.setRotationPoint(-2.0F, 12.0F + yOffset, 0.0F);
        rightVillagerLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, scale);
        leftVillagerLeg = new ModelRenderer(this, 0, 22).setTextureSize(texWidth, texHeight);
        leftVillagerLeg.mirror = true;
        leftVillagerLeg.setRotationPoint(2.0F, 12.0F + yOffset, 0.0F);
        leftVillagerLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, scale);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, entity);
        villagerHead.render(scale);
        villagerBody.render(scale);
        rightVillagerLeg.render(scale);
        leftVillagerLeg.render(scale);
        villagerArms.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity) {
        villagerHead.rotateAngleY = headYaw / (180F / (float) Math.PI);
        villagerHead.rotateAngleX = headPitch / (180F / (float) Math.PI);
        villagerArms.rotationPointY = 3.0F;
        villagerArms.rotationPointZ = -1.0F;
        villagerArms.rotateAngleX = -0.75F;
        rightVillagerLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount * 0.5F;
        leftVillagerLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount * 0.5F;
        rightVillagerLeg.rotateAngleY = 0.0F;
        leftVillagerLeg.rotateAngleY = 0.0F;
    }
}
