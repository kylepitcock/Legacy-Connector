package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/** 1.12.2 rabbit: hind legs, body, arms, head with ears and nose; the legs fold through the hop (the modern rabbit's jump progress). */
public class ModelRabbit extends ModelBase {
    private final ModelRenderer rabbitLeftFoot;
    private final ModelRenderer rabbitRightFoot;
    private final ModelRenderer rabbitLeftThigh;
    private final ModelRenderer rabbitRightThigh;
    private final ModelRenderer rabbitBody;
    private final ModelRenderer rabbitLeftArm;
    private final ModelRenderer rabbitRightArm;
    private final ModelRenderer rabbitHead;
    private final ModelRenderer rabbitRightEar;
    private final ModelRenderer rabbitLeftEar;
    private final ModelRenderer rabbitTail;
    private final ModelRenderer rabbitNose;
    private float jumpRotation;

    public ModelRabbit() {
        setTextureOffset("head.main", 0, 0);
        setTextureOffset("head.nose", 0, 24);
        setTextureOffset("head.ear1", 0, 10);
        setTextureOffset("head.ear2", 6, 10);
        rabbitLeftFoot = part(26, 24, -1.0F, 5.5F, -3.7F, 2, 1, 7, 3.0F, 17.5F, 3.7F, 0.0F, 0.0F);
        rabbitRightFoot = part(8, 24, -1.0F, 5.5F, -3.7F, 2, 1, 7, -3.0F, 17.5F, 3.7F, 0.0F, 0.0F);
        rabbitLeftThigh = part(30, 15, -1.0F, 0.0F, 0.0F, 2, 4, 5, 3.0F, 17.5F, 3.7F, -0.34906584F, 0.0F);
        rabbitRightThigh = part(16, 15, -1.0F, 0.0F, 0.0F, 2, 4, 5, -3.0F, 17.5F, 3.7F, -0.34906584F, 0.0F);
        rabbitBody = part(0, 0, -3.0F, -2.0F, -10.0F, 6, 5, 10, 0.0F, 19.0F, 8.0F, -0.34906584F, 0.0F);
        rabbitLeftArm = part(8, 15, -1.0F, 0.0F, -1.0F, 2, 7, 2, 3.0F, 17.0F, -1.0F, -0.17453292F, 0.0F);
        rabbitRightArm = part(0, 15, -1.0F, 0.0F, -1.0F, 2, 7, 2, -3.0F, 17.0F, -1.0F, -0.17453292F, 0.0F);
        rabbitHead = part(32, 0, -2.5F, -4.0F, -5.0F, 5, 4, 5, 0.0F, 16.0F, -1.0F, 0.0F, 0.0F);
        rabbitRightEar = part(52, 0, -2.5F, -9.0F, -1.0F, 2, 5, 1, 0.0F, 16.0F, -1.0F, 0.0F, -0.2617994F);
        rabbitLeftEar = part(58, 0, 0.5F, -9.0F, -1.0F, 2, 5, 1, 0.0F, 16.0F, -1.0F, 0.0F, 0.2617994F);
        rabbitTail = part(52, 6, -1.5F, -1.5F, 0.0F, 3, 3, 2, 0.0F, 20.0F, 7.0F, -0.3490659F, 0.0F);
        rabbitNose = part(32, 9, -0.5F, -2.5F, -5.5F, 1, 1, 1, 0.0F, 16.0F, -1.0F, 0.0F, 0.0F);
    }

    private ModelRenderer part(int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz, float rotX, float rotY) {
        ModelRenderer r = new ModelRenderer(this, u, v);
        r.addBox(x, y, z, w, h, d);
        r.setRotationPoint(px, py, pz);
        r.mirror = true;
        r.rotateAngleX = rotX;
        r.rotateAngleY = rotY;
        r.rotateAngleZ = 0.0F;
        return r;
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
        if (isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.56666666F, 0.56666666F, 0.56666666F);
            GlStateManager.translate(0.0F, 22.0F * scale, 2.0F * scale);
            rabbitHead.render(scale);
            rabbitLeftEar.render(scale);
            rabbitRightEar.render(scale);
            rabbitNose.render(scale);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.4F, 0.4F, 0.4F);
            GlStateManager.translate(0.0F, 36.0F * scale, 0.0F);
            renderBody(scale);
            GlStateManager.popMatrix();
        } else {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.6F, 0.6F, 0.6F);
            GlStateManager.translate(0.0F, 16.0F * scale, 0.0F);
            renderBody(scale);
            rabbitHead.render(scale);
            rabbitRightEar.render(scale);
            rabbitLeftEar.render(scale);
            rabbitNose.render(scale);
            GlStateManager.popMatrix();
        }
    }

    private void renderBody(float scale) {
        rabbitLeftFoot.render(scale);
        rabbitRightFoot.render(scale);
        rabbitLeftThigh.render(scale);
        rabbitRightThigh.render(scale);
        rabbitBody.render(scale);
        rabbitLeftArm.render(scale);
        rabbitRightArm.render(scale);
        rabbitTail.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        float f = entityIn == null ? 0.0F : ageInTicks - (float) Entity.mcc$get$ticksExisted(entityIn);
        rabbitNose.rotateAngleX = headPitch * 0.017453292F;
        rabbitHead.rotateAngleX = headPitch * 0.017453292F;
        rabbitRightEar.rotateAngleX = headPitch * 0.017453292F;
        rabbitLeftEar.rotateAngleX = headPitch * 0.017453292F;
        rabbitNose.rotateAngleY = netHeadYaw * 0.017453292F;
        rabbitHead.rotateAngleY = netHeadYaw * 0.017453292F;
        rabbitRightEar.rotateAngleY = rabbitNose.rotateAngleY - 0.2617994F;
        rabbitLeftEar.rotateAngleY = rabbitNose.rotateAngleY + 0.2617994F;
        jumpRotation = MathHelper.sin(mcc$jumpCompletion(entityIn, f) * (float) Math.PI);
        rabbitLeftThigh.rotateAngleX = (jumpRotation * 50.0F - 21.0F) * 0.017453292F;
        rabbitRightThigh.rotateAngleX = (jumpRotation * 50.0F - 21.0F) * 0.017453292F;
        rabbitLeftFoot.rotateAngleX = jumpRotation * 50.0F * 0.017453292F;
        rabbitRightFoot.rotateAngleX = jumpRotation * 50.0F * 0.017453292F;
        rabbitLeftArm.rotateAngleX = (jumpRotation * -40.0F - 11.0F) * 0.017453292F;
        rabbitRightArm.rotateAngleX = (jumpRotation * -40.0F - 11.0F) * 0.017453292F;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
        super.setLivingAnimations(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTickTime);
        jumpRotation = MathHelper.sin(mcc$jumpCompletion(entitylivingbaseIn, partialTickTime) * (float) Math.PI);
    }

    /** 1.12's {@code EntityRabbit.setJumpCompletion}: how far through its hop the rabbit is (the modern rabbit's jump progress). */
    private static float mcc$jumpCompletion(Entity entity, float partialTicks) {
        if (entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.animal.Rabbit rabbit) return rabbit.getJumpCompletion(partialTicks);
        return 0.0F;
    }
}
