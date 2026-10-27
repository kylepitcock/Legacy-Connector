package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBiped;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerElytra;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 renderer of humanoid mobs: the biped model with its head item, elytra and held item layers. */
public class RenderBiped<T extends EntityLiving> extends RenderLiving<T> {
    public static final ResourceLocation DEFAULT_RES_LOC = new ResourceLocation("textures/entity/steve.png");

    public RenderBiped(RenderManager renderManagerIn, ModelBiped modelBipedIn, float shadowSize) {
        super(renderManagerIn, modelBipedIn, shadowSize);
        addLayer(new LayerCustomHead(modelBipedIn.bipedHead));
        addLayer(new LayerElytra(this));
        addLayer(new LayerHeldItem(this));
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return DEFAULT_RES_LOC;
    }

    @Override
    public void transformHeldFull3DItemLayer() {
        GlStateManager.translate(0.0F, 0.1875F, 0.0F);
    }
}
