package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelVillager;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** Villager renderer (also the base of mods' villager renderers): the profession's registered skin, else the plain villager. */
public class RenderVillager extends RenderLiving {
    private static final ResourceLocation VILLAGER = new ResourceLocation("textures/entity/villager/villager.png");

    public RenderVillager() {
        super(new ModelVillager(0.0F), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        if (entity instanceof legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager villager) {
            return legacyconnector.v1_7_10.cpw.mods.fml.common.registry.VillagerRegistry.getVillagerSkin(villager.getProfession(), VILLAGER);
        }
        return VILLAGER;
    }
}
