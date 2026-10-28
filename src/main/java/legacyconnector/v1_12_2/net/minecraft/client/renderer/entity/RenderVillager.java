package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelVillager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 villager renderer: the villager model with its head item, textured by profession. */
public class RenderVillager extends RenderLiving<EntityVillager> {
    private static final ResourceLocation VILLAGER_TEXTURES = new ResourceLocation("textures/entity/villager/villager.png");
    private static final ResourceLocation FARMER_VILLAGER_TEXTURES = new ResourceLocation("textures/entity/villager/farmer.png");
    private static final ResourceLocation LIBRARIAN_VILLAGER_TEXTURES = new ResourceLocation("textures/entity/villager/librarian.png");
    private static final ResourceLocation PRIEST_VILLAGER_TEXTURES = new ResourceLocation("textures/entity/villager/priest.png");
    private static final ResourceLocation SMITH_VILLAGER_TEXTURES = new ResourceLocation("textures/entity/villager/smith.png");
    private static final ResourceLocation BUTCHER_VILLAGER_TEXTURES = new ResourceLocation("textures/entity/villager/butcher.png");

    public RenderVillager(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelVillager(0.0F), 0.5F);
        addLayer(new LayerCustomHead(getMainModel().villagerHead));
    }

    @Override
    public ModelVillager getMainModel() {
        return (ModelVillager) super.getMainModel();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityVillager entity) {
        return switch (entity.getProfession()) {
            case 0 -> FARMER_VILLAGER_TEXTURES;
            case 1 -> LIBRARIAN_VILLAGER_TEXTURES;
            case 2 -> PRIEST_VILLAGER_TEXTURES;
            case 3 -> SMITH_VILLAGER_TEXTURES;
            case 4 -> BUTCHER_VILLAGER_TEXTURES;
            default -> VILLAGER_TEXTURES;
        };
    }

    @Override
    protected void preRenderCallback(EntityVillager entitylivingbaseIn, float partialTickTime) {
        float f = 0.9375F;
        if (entitylivingbaseIn.isChild()) {
            f = (float) ((double) f * 0.5D);
            shadowSize = 0.25F;
        } else {
            shadowSize = 0.5F;
        }
        GlStateManager.scale(f, f, f);
    }
}
