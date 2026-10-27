package legacyconnector.v1_12_2.net.minecraft.client.model;

/** 1.12.2 bed model (the tile entity bed of 1.12): head and foot halves with their legs, drawn one half at a time. */
public class ModelBed extends ModelBase {
    public ModelRenderer headPiece;
    public ModelRenderer footPiece;
    public ModelRenderer[] legs = new ModelRenderer[4];

    public ModelBed() {
        textureWidth = 64;
        textureHeight = 64;
        headPiece = new ModelRenderer(this, 0, 0);
        headPiece.addBox(0.0F, 0.0F, 0.0F, 16, 16, 6, 0.0F);
        footPiece = new ModelRenderer(this, 0, 22);
        footPiece.addBox(0.0F, 0.0F, 0.0F, 16, 16, 6, 0.0F);
        legs[0] = new ModelRenderer(this, 50, 0);
        legs[1] = new ModelRenderer(this, 50, 6);
        legs[2] = new ModelRenderer(this, 50, 12);
        legs[3] = new ModelRenderer(this, 50, 18);
        legs[0].addBox(0.0F, 6.0F, -16.0F, 3, 3, 3);
        legs[1].addBox(0.0F, 6.0F, 0.0F, 3, 3, 3);
        legs[2].addBox(-16.0F, 6.0F, -16.0F, 3, 3, 3);
        legs[3].addBox(-16.0F, 6.0F, 0.0F, 3, 3, 3);
        for (ModelRenderer leg : legs) leg.rotateAngleX = (float) Math.PI / 2F;
        legs[0].rotateAngleZ = 0.0F;
        legs[1].rotateAngleZ = (float) Math.PI / 2F;
        legs[2].rotateAngleZ = (float) Math.PI * 3F / 2F;
        legs[3].rotateAngleZ = (float) Math.PI;
    }

    public int getModelVersion() {
        return 51;
    }

    public void render() {
        headPiece.render(0.0625F);
        footPiece.render(0.0625F);
        for (ModelRenderer leg : legs) leg.render(0.0625F);
    }

    public void preparePiece(boolean isHead) {
        headPiece.showModel = isHead;
        footPiece.showModel = !isHead;
        legs[0].showModel = !isHead;
        legs[1].showModel = isHead;
        legs[2].showModel = !isHead;
        legs[3].showModel = isHead;
    }
}
