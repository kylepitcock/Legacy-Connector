package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelSheep1;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderSheep;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntitySheep;
import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 sheep wool: the fur model tinted with the fleece colour (cycling through all dyes for a sheep named jeb_), hidden once shorn. */
public class LayerSheepWool implements LayerRenderer<EntitySheep> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/sheep/sheep_fur.png");
    private final RenderSheep sheepRenderer;
    private final ModelSheep1 sheepModel = new ModelSheep1();

    public LayerSheepWool(RenderSheep sheepRendererIn) {
        this.sheepRenderer = sheepRendererIn;
    }

    @Override
    public void doRenderLayer(EntitySheep entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
            float scale) {
        if (entitylivingbaseIn.getSheared() || entitylivingbaseIn.isInvisible()) return;
        sheepRenderer.bindTexture(TEXTURE);
        if (entitylivingbaseIn.hasCustomName() && "jeb_".equals(entitylivingbaseIn.getCustomNameTag())) {
            int ticks = Entity.mcc$get$ticksExisted(entitylivingbaseIn);
            int i = ticks / 25 + entitylivingbaseIn.getEntityId();
            int j = EnumDyeColor.values().length;
            int k = Math.floorMod(i, j);
            int l = Math.floorMod(i + 1, j);
            float f = ((float) (ticks % 25) + partialTicks) / 25.0F;
            float[] a = EntitySheep.getDyeRgb(EnumDyeColor.byMetadata(k));
            float[] b = EntitySheep.getDyeRgb(EnumDyeColor.byMetadata(l));
            GlStateManager.color(a[0] * (1.0F - f) + b[0] * f, a[1] * (1.0F - f) + b[1] * f, a[2] * (1.0F - f) + b[2] * f);
        } else {
            float[] rgb = EntitySheep.getDyeRgb(entitylivingbaseIn.getFleeceColor());
            GlStateManager.color(rgb[0], rgb[1], rgb[2]);
        }
        sheepModel.setModelAttributes(sheepRenderer.getMainModel());
        sheepModel.setLivingAnimations(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks);
        sheepModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
    }

    @Override
    public boolean shouldCombineTextures() {
        return true;
    }
}
