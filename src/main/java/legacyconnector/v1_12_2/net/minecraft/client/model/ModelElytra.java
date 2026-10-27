package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;

/** 1.12.2 elytra model: two wings folded at the back, spread while gliding. */
public class ModelElytra extends ModelBase {
    private final ModelRenderer rightWing;
    private final ModelRenderer leftWing = new ModelRenderer(this, 22, 0);

    public ModelElytra() {
        leftWing.addBox(-10.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);
        rightWing = new ModelRenderer(this, 22, 0);
        rightWing.mirror = true;
        rightWing.addBox(0.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableCull();
        if (entityIn instanceof EntityLivingBase living && living.isChild()) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 1.5F, -0.1F);
            leftWing.render(scale);
            rightWing.render(scale);
            GlStateManager.popMatrix();
        } else {
            leftWing.render(scale);
            rightWing.render(scale);
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        float x = 0.2617994F;
        float z = -0.2617994F;
        float y = 0.0F;
        float yaw = 0.0F;
        if (entityIn instanceof EntityLivingBase living && living.isElytraFlying()) {
            float spread = 1.0F;
            double mx = Entity.mcc$get$motionX(entityIn), my = Entity.mcc$get$motionY(entityIn), mz = Entity.mcc$get$motionZ(entityIn);
            if (my < 0.0D) {
                double length = Math.sqrt(mx * mx + my * my + mz * mz);
                double ny = length < 1.0E-4D ? 0.0D : my / length;
                spread = 1.0F - (float) Math.pow(-ny, 1.5D);
            }
            x = spread * 0.34906584F + (1.0F - spread) * x;
            z = spread * -((float) Math.PI / 2F) + (1.0F - spread) * z;
        } else if (entityIn != null && entityIn.isSneaking()) {
            x = (float) Math.PI * 2F / 9F;
            z = -((float) Math.PI / 4F);
            y = 3.0F;
            yaw = 0.08726646F;
        }
        leftWing.rotationPointX = 5.0F;
        leftWing.rotationPointY = y;
        leftWing.rotateAngleX = x;
        leftWing.rotateAngleZ = z;
        leftWing.rotateAngleY = yaw;
        rightWing.rotationPointX = -leftWing.rotationPointX;
        rightWing.rotateAngleY = -leftWing.rotateAngleY;
        rightWing.rotationPointY = leftWing.rotationPointY;
        rightWing.rotateAngleX = leftWing.rotateAngleX;
        rightWing.rotateAngleZ = -leftWing.rotateAngleZ;
    }
}
