package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelShulker;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityShulker;
import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;

/**
 * 1.12.2 shulker renderer and its per-colour textures (indexed by dye metadata; mods' shulker-box renderers read them). Vanilla
 * shulkers (modern entities) are drawn by the modern renderer; a mod's shulker uses the shulker model.
 */
public class RenderShulker extends RenderLiving<EntityShulker> {
    public static final ResourceLocation[] SHULKER_ENDERGOLEM_TEXTURE = {
            new ResourceLocation("textures/entity/shulker/shulker_white.png"),
            new ResourceLocation("textures/entity/shulker/shulker_orange.png"),
            new ResourceLocation("textures/entity/shulker/shulker_magenta.png"),
            new ResourceLocation("textures/entity/shulker/shulker_light_blue.png"),
            new ResourceLocation("textures/entity/shulker/shulker_yellow.png"),
            new ResourceLocation("textures/entity/shulker/shulker_lime.png"),
            new ResourceLocation("textures/entity/shulker/shulker_pink.png"),
            new ResourceLocation("textures/entity/shulker/shulker_gray.png"),
            new ResourceLocation("textures/entity/shulker/shulker_light_gray.png"),
            new ResourceLocation("textures/entity/shulker/shulker_cyan.png"),
            new ResourceLocation("textures/entity/shulker/shulker_purple.png"),
            new ResourceLocation("textures/entity/shulker/shulker_blue.png"),
            new ResourceLocation("textures/entity/shulker/shulker_brown.png"),
            new ResourceLocation("textures/entity/shulker/shulker_green.png"),
            new ResourceLocation("textures/entity/shulker/shulker_red.png"),
            new ResourceLocation("textures/entity/shulker/shulker_black.png")};
    private static final ResourceLocation SHULKER_DEFAULT = new ResourceLocation("textures/entity/shulker/shulker.png");

    public RenderShulker(RenderManager manager) {
        super(manager, new ModelShulker(), 0.0F);
    }

    @Override
    public ModelShulker getMainModel() {
        return (ModelShulker) super.getMainModel();
    }

    @Override
    public void doRender(EntityShulker entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.mcc$entity != null && !(entity.mcc$entity instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122)
                && getClass() == RenderShulker.class) {
            LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
            return;
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityShulker entity) {
        EnumDyeColor color = entity.getColor();
        return color == null ? SHULKER_DEFAULT : SHULKER_ENDERGOLEM_TEXTURE[color.getMetadata()];
    }
}
