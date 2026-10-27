package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBox;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderHelper;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderArrow;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/** 1.12.2 stuck-arrows layer: each arrow at a random point of a random box of the model (the same points every frame, by entity id). */
public class LayerArrow implements LayerRenderer<EntityLivingBase> {
    private static final ResourceLocation ARROW = new ResourceLocation("textures/entity/projectiles/arrow.png");
    private final RenderLivingBase<?> renderer;

    public LayerArrow(RenderLivingBase<?> rendererIn) {
        this.renderer = rendererIn;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        int count = entitylivingbaseIn.getArrowCountInEntity();
        if (count <= 0 || renderer.getMainModel() == null || renderer.getMainModel().boxList.isEmpty()) return;
        Random random = new Random((long) entitylivingbaseIn.getEntityId());
        RenderHelper.disableStandardItemLighting();
        renderer.bindTexture(ARROW);
        for (int i = 0; i < count; ++i) {
            GlStateManager.pushMatrix();
            ModelRenderer part = renderer.getMainModel().getRandomModelBox(random);
            if (part.cubeList.isEmpty()) {
                GlStateManager.popMatrix();
                continue;
            }
            ModelBox box = part.cubeList.get(random.nextInt(part.cubeList.size()));
            part.postRender(0.0625F);
            float fx = random.nextFloat(), fy = random.nextFloat(), fz = random.nextFloat();
            GlStateManager.translate((box.posX1 + (box.posX2 - box.posX1) * fx) / 16.0F, (box.posY1 + (box.posY2 - box.posY1) * fy) / 16.0F,
                    (box.posZ1 + (box.posZ2 - box.posZ1) * fz) / 16.0F);
            fx = fx * 2.0F - 1.0F;
            fy = fy * 2.0F - 1.0F;
            fz = fz * 2.0F - 1.0F;
            fx = fx * -1.0F;
            fy = fy * -1.0F;
            fz = fz * -1.0F;
            float horizontal = MathHelper.sqrt(fx * fx + fz * fz);
            float yaw = (float) (Math.atan2((double) fx, (double) fz) * (180D / Math.PI));
            float pitch = (float) (Math.atan2((double) fy, (double) horizontal) * (180D / Math.PI));
            GlStateManager.rotate(yaw - 90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 0.0F, 0.0F, 1.0F);
            RenderArrow.mcc$drawArrow(0.0F);
            GlStateManager.popMatrix();
        }
        RenderHelper.enableStandardItemLighting();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
