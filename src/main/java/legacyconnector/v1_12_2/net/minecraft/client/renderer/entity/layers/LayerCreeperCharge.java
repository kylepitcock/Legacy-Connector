package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelCreeper;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderCreeper;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityCreeper;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 charged creeper layer: the swirling lightning armor drawn around a powered creeper. */
public class LayerCreeperCharge implements LayerRenderer<EntityCreeper> {
    private static final ResourceLocation LIGHTNING_TEXTURE = new ResourceLocation("textures/entity/creeper/creeper_armor.png");
    protected final RenderCreeper creeperRenderer;
    protected final ModelCreeper creeperModel = new ModelCreeper(2.0F);

    public LayerCreeperCharge(RenderCreeper creeperRendererIn) {
        this.creeperRenderer = creeperRendererIn;
    }

    @Override
    public void doRenderLayer(EntityCreeper entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (!entitylivingbaseIn.getPowered()) return;
        boolean invisible = entitylivingbaseIn.isInvisible();
        GlStateManager.depthMask(!invisible);
        creeperRenderer.bindTexture(LIGHTNING_TEXTURE);
        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        float f = ageInTicks * 0.01F;
        GlStateManager.translate(f, f, 0.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.enableBlend();
        GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        creeperModel.setModelAttributes(creeperRenderer.getMainModel());
        creeperModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(5888);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(invisible);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
