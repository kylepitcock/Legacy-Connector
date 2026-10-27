package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelElytra;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/** 1.12.2 elytra layer: the elytra worn in the chest slot, behind the body. */
public class LayerElytra implements LayerRenderer<EntityLivingBase> {
    private static final ResourceLocation TEXTURE_ELYTRA = new ResourceLocation("textures/entity/elytra.png");
    protected final RenderLivingBase<?> renderPlayer;
    private final ModelElytra modelElytra = new ModelElytra();

    public LayerElytra(RenderLivingBase<?> p_i47185_1_) {
        this.renderPlayer = p_i47185_1_;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack stack = entitylivingbaseIn.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (stack.isEmpty() || !Stacks1122.toModern(stack).is(net.minecraft.world.item.Items.ELYTRA)) return;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        renderPlayer.bindTexture(TEXTURE_ELYTRA);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 0.125F);
        modelElytra.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entitylivingbaseIn);
        modelElytra.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        if (stack.isItemEnchanted()) LayerArmorBase.renderEnchantedGlint(renderPlayer, entitylivingbaseIn, modelElytra, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
