package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

public class ModelLeashKnot extends ModelBase {
    public ModelRenderer knotRenderer;

    public ModelLeashKnot() {
        this(0, 0, 32, 32);
    }

    public ModelLeashKnot(int i, int j, int k, int l) {
        this.textureWidth = k;
        this.textureHeight = l;
        this.knotRenderer = new ModelRenderer(this, i, j);
        this.knotRenderer.addBox(-3.0F, -6.0F, -3.0F, 6, 8, 6, 0.0F);
        this.knotRenderer.setRotationPoint(0.0F, 0.0F, 0.0F);
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        this.knotRenderer.render(f5);
    }

    @Override
    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        this.knotRenderer.rotateAngleY = f3 / (180.0F / (float)Math.PI);
        this.knotRenderer.rotateAngleX = f4 / (180.0F / (float)Math.PI);
    }
}
