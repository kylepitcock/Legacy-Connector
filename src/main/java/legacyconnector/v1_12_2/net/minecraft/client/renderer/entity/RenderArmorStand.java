package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelArmorStand;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBiped;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerElytra;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityArmorStand;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 armour stand renderer: the stand model with armour, held items, elytra and head item layers. */
public class RenderArmorStand extends RenderLivingBase<EntityArmorStand> {
    public static final ResourceLocation TEXTURE_ARMOR_STAND = new ResourceLocation("textures/entity/armorstand/wood.png");

    public RenderArmorStand(RenderManager manager) {
        super(manager, new ModelArmorStand(), 0.0F);
        addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                this.modelLeggings = new ModelBiped(0.5F);
                this.modelArmor = new ModelBiped(1.0F);
            }
        });
        addLayer(new LayerHeldItem(this));
        addLayer(new LayerElytra(this));
        addLayer(new LayerCustomHead(getMainModel().bipedHead));
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityArmorStand entity) {
        return TEXTURE_ARMOR_STAND;
    }

    @Override
    public ModelArmorStand getMainModel() {
        return (ModelArmorStand) super.getMainModel();
    }

    @Override
    protected void applyRotations(EntityArmorStand entityLiving, float p_77043_2_, float rotationYaw, float partialTicks) {
        GlStateManager.rotate(180.0F - rotationYaw, 0.0F, 1.0F, 0.0F);
    }

    @Override
    protected boolean canRenderName(EntityArmorStand entity) {
        return entity.getAlwaysRenderNameTag();
    }
}
