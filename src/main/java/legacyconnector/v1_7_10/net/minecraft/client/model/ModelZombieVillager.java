package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;

public class ModelZombieVillager extends ModelBiped {
    public ModelZombieVillager() {
        this(0.0F, 0.0F, false);
    }

    public ModelZombieVillager(float f, float f1, boolean flag) {
        super(f, 0.0F, 64, flag ? 32 : 64);
        if (flag) {
            this.bipedHead = new ModelRenderer(this, 0, 0);
            this.bipedHead.addBox(-4.0F, -10.0F, -4.0F, 8, 6, 8, f);
            this.bipedHead.setRotationPoint(0.0F, 0.0F + f1, 0.0F);
        } else {
            this.bipedHead = new ModelRenderer(this);
            this.bipedHead.setRotationPoint(0.0F, 0.0F + f1, 0.0F);
            this.bipedHead.setTextureOffset(0, 32).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, f);
            this.bipedHead.setTextureOffset(24, 32).addBox(-1.0F, -3.0F, -6.0F, 2, 4, 2, f);
        }
    }

    public int func_82897_a() {
        return 10;
    }

    @Override
    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        float f6 = MathHelper.sin(this.onGround * (float) Math.PI);
        float f7 = MathHelper.sin((1.0F - (1.0F - this.onGround) * (1.0F - this.onGround)) * (float) Math.PI);
        this.bipedRightArm.rotateAngleZ = 0.0F;
        this.bipedLeftArm.rotateAngleZ = 0.0F;
        this.bipedRightArm.rotateAngleY = -(0.1F - f6 * 0.6F);
        this.bipedLeftArm.rotateAngleY = 0.1F - f6 * 0.6F;
        this.bipedRightArm.rotateAngleX = (float) (-Math.PI / 2);
        this.bipedLeftArm.rotateAngleX = (float) (-Math.PI / 2);
        this.bipedRightArm.rotateAngleX -= f6 * 1.2F - f7 * 0.4F;
        this.bipedLeftArm.rotateAngleX -= f6 * 1.2F - f7 * 0.4F;
        this.bipedRightArm.rotateAngleZ = this.bipedRightArm.rotateAngleZ + MathHelper.cos(f2 * 0.09F) * 0.05F + 0.05F;
        this.bipedLeftArm.rotateAngleZ = this.bipedLeftArm.rotateAngleZ - (MathHelper.cos(f2 * 0.09F) * 0.05F + 0.05F);
        this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX + MathHelper.sin(f2 * 0.067F) * 0.05F;
        this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX - MathHelper.sin(f2 * 0.067F) * 0.05F;
    }
}
