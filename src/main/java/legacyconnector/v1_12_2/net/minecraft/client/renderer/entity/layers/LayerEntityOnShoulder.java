package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderLivingBase;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderManager;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * 1.12.2 shoulder-parrot layer of the player renderer. Players (and their parrots) are drawn by the modern player renderer, which
 * draws shoulder parrots itself; this layer keeps 1.12's fields for mods that read them and draws nothing.
 */
public class LayerEntityOnShoulder implements LayerRenderer<EntityPlayer> {
    private final RenderManager renderManager;
    protected RenderLivingBase<? extends legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase> leftRenderer;
    private ModelBase leftModel;
    private ResourceLocation leftResource;
    private java.util.UUID leftUniqueId;
    private Class<?> leftEntityClass;
    protected RenderLivingBase<? extends legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase> rightRenderer;
    private ModelBase rightModel;
    private ResourceLocation rightResource;
    private java.util.UUID rightUniqueId;
    private Class<?> rightEntityClass;

    public LayerEntityOnShoulder(RenderManager p_i47370_1_) {
        this.renderManager = p_i47370_1_;
    }

    @Override
    public void doRenderLayer(EntityPlayer entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
