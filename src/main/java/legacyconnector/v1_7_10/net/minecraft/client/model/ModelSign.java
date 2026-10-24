package legacyconnector.v1_7_10.net.minecraft.client.model;

/** The vanilla 1.7.10 sign model: a board on a stick (texture 64x32). */
public class ModelSign extends ModelBase {
    public ModelRenderer signBoard = new ModelRenderer(this, 0, 0);
    public ModelRenderer signStick;

    public ModelSign() {
        signBoard.addBox(-12.0F, -14.0F, -1.0F, 24, 12, 2, 0.0F);
        signStick = new ModelRenderer(this, 0, 14);
        signStick.addBox(-1.0F, -2.0F, -1.0F, 2, 14, 2, 0.0F);
    }

    public void renderSign() {
        signBoard.render(0.0625F);
        signStick.render(0.0625F);
    }
}
