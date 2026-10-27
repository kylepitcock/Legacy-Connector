package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

public class ModelBlaze extends ModelBase {
    private ModelRenderer[] blazeSticks = new ModelRenderer[12];
    private ModelRenderer blazeHead;

    public ModelBlaze() {
        for (int i = 0; i < this.blazeSticks.length; i++) {
            this.blazeSticks[i] = new ModelRenderer(this, 0, 16);
            this.blazeSticks[i].addBox(0.0F, 0.0F, 0.0F, 2, 8, 2);
        }

        this.blazeHead = new ModelRenderer(this, 0, 0);
        this.blazeHead.addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8);
    }

    public int func_78104_a() {
        return 8;
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        this.blazeHead.render(f5);

        for (int i = 0; i < this.blazeSticks.length; i++) {
            this.blazeSticks[i].render(f5);
        }
    }

    @Override
    public void setRotationAngles(float f4, float f5, float f, float f1, float f2, float f6, Entity entity) {
        float f3 = f * (float) Math.PI * -0.1F;

        for (int i = 0; i < 4; i++) {
            this.blazeSticks[i].rotationPointY = -2.0F + MathHelper.cos(((float)(i * 2) + f) * 0.25F);
            this.blazeSticks[i].rotationPointX = MathHelper.cos(f3) * 9.0F;
            this.blazeSticks[i].rotationPointZ = MathHelper.sin(f3) * 9.0F;
            f3++;
        }

        f3 = (float) (Math.PI / 4) + f * (float) Math.PI * 0.03F;

        for (int j = 4; j < 8; j++) {
            this.blazeSticks[j].rotationPointY = 2.0F + MathHelper.cos(((float)(j * 2) + f) * 0.25F);
            this.blazeSticks[j].rotationPointX = MathHelper.cos(f3) * 7.0F;
            this.blazeSticks[j].rotationPointZ = MathHelper.sin(f3) * 7.0F;
            f3++;
        }

        f3 = 0.47123894F + f * (float) Math.PI * -0.05F;

        for (int k = 8; k < 12; k++) {
            this.blazeSticks[k].rotationPointY = 11.0F + MathHelper.cos(((float)k * 1.5F + f) * 0.5F);
            this.blazeSticks[k].rotationPointX = MathHelper.cos(f3) * 5.0F;
            this.blazeSticks[k].rotationPointZ = MathHelper.sin(f3) * 5.0F;
            f3++;
        }

        this.blazeHead.rotateAngleY = f1 / (180.0F / (float)Math.PI);
        this.blazeHead.rotateAngleX = f2 / (180.0F / (float)Math.PI);
    }
}
