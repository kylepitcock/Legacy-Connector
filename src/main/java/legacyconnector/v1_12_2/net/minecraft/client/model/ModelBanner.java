package legacyconnector.v1_12_2.net.minecraft.client.model;

/** 1.12.2 standing banner model: the cloth (64x64 texture), pole and crossbar. */
public class ModelBanner extends ModelBase {
    public ModelRenderer bannerSlate;
    public ModelRenderer bannerStand;
    public ModelRenderer bannerTop;

    public ModelBanner() {
        textureWidth = 64;
        textureHeight = 64;
        bannerSlate = new ModelRenderer(this, 0, 0);
        bannerSlate.addBox(-10.0F, 0.0F, -2.0F, 20, 40, 1, 0.0F);
        bannerStand = new ModelRenderer(this, 44, 0);
        bannerStand.addBox(-1.0F, -30.0F, -1.0F, 2, 42, 2, 0.0F);
        bannerTop = new ModelRenderer(this, 0, 42);
        bannerTop.addBox(-10.0F, -32.0F, -1.0F, 20, 2, 2, 0.0F);
    }

    public void renderBanner() {
        bannerSlate.rotationPointY = -32.0F;
        bannerSlate.render(0.0625F);
        bannerStand.render(0.0625F);
        bannerTop.render(0.0625F);
    }
}
