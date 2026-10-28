package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelSlime;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderSlime;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntitySlime;

/** 1.12.2 slime gel layer: the translucent outer cube around the slime's inner body. */
public class LayerSlimeGel implements LayerRenderer<EntitySlime> {
    private final RenderSlime slimeRenderer;
    private final ModelBase slimeModel = new ModelSlime(0);

    public LayerSlimeGel(RenderSlime slimeRendererIn) {
        this.slimeRenderer = slimeRendererIn;
    }

    @Override
    public void doRenderLayer(EntitySlime entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (entitylivingbaseIn.isInvisible()) return;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableNormalize();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        slimeModel.setModelAttributes(slimeRenderer.getMainModel());
        slimeModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.disableBlend();
        GlStateManager.disableNormalize();
    }

    @Override
    public boolean shouldCombineTextures() {
        return true;
    }
}
