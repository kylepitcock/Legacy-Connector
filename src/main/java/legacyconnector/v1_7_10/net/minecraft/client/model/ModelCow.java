package legacyconnector.v1_7_10.net.minecraft.client.model;

/** 1.7.10 cow: a quadruped with a bigger body, horns and an udder. */
public class ModelCow extends ModelQuadruped {
    public ModelRenderer udder;
    public ModelRenderer horn1;
    public ModelRenderer horn2;

    public ModelCow() {
        super(12, 0.0F);
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-4.0F, -4.0F, -6.0F, 8, 8, 6, 0.0F);
        head.setRotationPoint(0.0F, 4.0F, -8.0F);
        head.setTextureOffset(22, 0).addBox(-5.0F, -5.0F, -4.0F, 1, 3, 1);
        head.setTextureOffset(22, 0).addBox(4.0F, -5.0F, -4.0F, 1, 3, 1);
        body = new ModelRenderer(this, 18, 4);
        body.addBox(-6.0F, -10.0F, -7.0F, 12, 18, 10, 0.0F);
        body.setRotationPoint(0.0F, 5.0F, 2.0F);
        body.setTextureOffset(52, 0).addBox(-2.0F, 2.0F, -8.0F, 4, 6, 1);
        body.rotateAngleX = (float) Math.PI / 2F;
        leg1.setRotationPoint(-4.0F, 12.0F, 7.0F);
        leg2.setRotationPoint(4.0F, 12.0F, 7.0F);
        leg3.setRotationPoint(-4.0F, 12.0F, -6.0F);
        leg4.setRotationPoint(4.0F, 12.0F, -6.0F);
        field_78145_g -= 1.0F;
        field_78151_h += 2.0F;
    }
}
