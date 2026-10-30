package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;

/** 1.12.2 sheep body (the shorn sheep): a quadruped with a long head that lowers while the sheep eats grass. */
public class ModelSheep2 extends ModelQuadruped {
    private float headRotationAngleX;

    public ModelSheep2() {
        super(12, 0.0F);
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-3.0F, -4.0F, -6.0F, 6, 6, 8, 0.0F);
        head.setRotationPoint(0.0F, 6.0F, -8.0F);
        body = new ModelRenderer(this, 28, 8);
        body.addBox(-4.0F, -10.0F, -7.0F, 8, 16, 6, 0.0F);
        body.setRotationPoint(0.0F, 5.0F, 2.0F);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
        super.setLivingAnimations(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTickTime);
        head.rotationPointY = 6.0F + mcc$headRotationPointY(entitylivingbaseIn, partialTickTime) * 9.0F;
        headRotationAngleX = mcc$headRotationAngleX(entitylivingbaseIn, partialTickTime);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        head.rotateAngleX = headRotationAngleX;
    }

    /** 1.12's {@code EntitySheep.getHeadRotationPointY}: how far the head is lowered to eat (the modern sheep's eat animation). */
    static float mcc$headRotationPointY(Entity entity, float partialTicks) {
        if (entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.animal.Sheep sheep) return sheep.getHeadEatPositionScale(partialTicks);
        return 0.0F;
    }

    /** 1.12's {@code EntitySheep.getHeadRotationAngleX}: the head's pitch, nodding while it eats. */
    static float mcc$headRotationAngleX(Entity entity, float partialTicks) {
        if (entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.animal.Sheep sheep) return sheep.getHeadEatAngleScale(partialTicks);
        return entity == null ? 0.0F : Entity.mcc$get$rotationPitch(entity) * 0.017453292F;
    }
}
