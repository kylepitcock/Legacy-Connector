package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.boss.EntityWither;

/** 1.7.10's wither model: shoulders, spine and ribs, tail; the centre head follows the look, the side heads their targets. */
public class ModelWither extends ModelBase {
    private final ModelRenderer[] field_82905_a;
    private final ModelRenderer[] field_82904_b;

    public ModelWither() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.field_82905_a = new ModelRenderer[3];
        this.field_82905_a[0] = new ModelRenderer(this, 0, 16);
        this.field_82905_a[0].addBox(-10.0F, 3.9F, -0.5F, 20, 3, 3);
        this.field_82905_a[1] = new ModelRenderer(this).setTextureSize(this.textureWidth, this.textureHeight);
        this.field_82905_a[1].setRotationPoint(-2.0F, 6.9F, -0.5F);
        this.field_82905_a[1].setTextureOffset(0, 22).addBox(0.0F, 0.0F, 0.0F, 3, 10, 3);
        this.field_82905_a[1].setTextureOffset(24, 22).addBox(-4.0F, 1.5F, 0.5F, 11, 2, 2);
        this.field_82905_a[1].setTextureOffset(24, 22).addBox(-4.0F, 4.0F, 0.5F, 11, 2, 2);
        this.field_82905_a[1].setTextureOffset(24, 22).addBox(-4.0F, 6.5F, 0.5F, 11, 2, 2);
        this.field_82905_a[2] = new ModelRenderer(this, 12, 22);
        this.field_82905_a[2].addBox(0.0F, 0.0F, 0.0F, 3, 6, 3);
        this.field_82904_b = new ModelRenderer[3];
        this.field_82904_b[0] = new ModelRenderer(this, 0, 0);
        this.field_82904_b[0].addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8);
        this.field_82904_b[1] = new ModelRenderer(this, 32, 0);
        this.field_82904_b[1].addBox(-4.0F, -4.0F, -4.0F, 6, 6, 6);
        this.field_82904_b[1].rotationPointX = -8.0F;
        this.field_82904_b[1].rotationPointY = 4.0F;
        this.field_82904_b[2] = new ModelRenderer(this, 32, 0);
        this.field_82904_b[2].addBox(-4.0F, -4.0F, -4.0F, 6, 6, 6);
        this.field_82904_b[2].rotationPointX = 10.0F;
        this.field_82904_b[2].rotationPointY = 4.0F;
    }

    /** The texture height the model was built for (RenderWither rebuilds the model when it changes). */
    public int func_82903_a() {
        return 32;
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        for (ModelRenderer head : field_82904_b) head.render(f5);
        for (ModelRenderer part : field_82905_a) part.render(f5);
    }

    @Override
    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        float breath = (float) Math.cos(f2 * 0.1F);
        field_82905_a[1].rotateAngleX = (0.065F + 0.05F * breath) * (float) Math.PI;
        field_82905_a[2].setRotationPoint(-2.0F, 6.9F + (float) Math.cos(field_82905_a[1].rotateAngleX) * 10.0F,
                -0.5F + (float) Math.sin(field_82905_a[1].rotateAngleX) * 10.0F);
        field_82905_a[2].rotateAngleX = (0.265F + 0.1F * breath) * (float) Math.PI;
        field_82904_b[0].rotateAngleY = f3 / (180F / (float) Math.PI);
        field_82904_b[0].rotateAngleX = f4 / (180F / (float) Math.PI);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float f, float f1, float partialTicks) {
        if (!(entity instanceof EntityWither wither)) return;
        for (int i = 1; i < 3; ++i) {
            field_82904_b[i].rotateAngleY = (wither.func_82207_a(i - 1) - EntityLivingBase.mcc$get$renderYawOffset(entity)) / (180F / (float) Math.PI);
            field_82904_b[i].rotateAngleX = wither.func_82210_r(i - 1) / (180F / (float) Math.PI);
        }
    }
}
