package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntitySheep;

public class ModelSheep2 extends ModelQuadruped {
    private float field_78153_i;

    public ModelSheep2() {
        super(12, 0.0F);
        this.head = new ModelRenderer(this, 0, 0);
        this.head.addBox(-3.0F, -4.0F, -6.0F, 6, 6, 8, 0.0F);
        this.head.setRotationPoint(0.0F, 6.0F, -8.0F);
        this.body = new ModelRenderer(this, 28, 8);
        this.body.addBox(-4.0F, -10.0F, -7.0F, 8, 16, 6, 0.0F);
        this.body.setRotationPoint(0.0F, 5.0F, 2.0F);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbase, float f, float f1, float f2) {
        super.setLivingAnimations(entitylivingbase, f, f1, f2);
        this.head.rotationPointY = 6.0F + ((EntitySheep)entitylivingbase).getHeadRotationPointY(f2) * 9.0F;
        this.field_78153_i = ((EntitySheep)entitylivingbase).getHeadRotationAngleX(f2);
    }

    @Override
    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        this.head.rotateAngleX = this.field_78153_i;
    }
}
