package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped;
import legacyconnector.v1_7_10.net.minecraft.client.model.ModelZombie;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** Zombie renderer (also the base of mod zombie variants). */
public class RenderZombie extends RenderBiped {
    private static final ResourceLocation ZOMBIE = new ResourceLocation("textures/entity/zombie/zombie.png");
    protected ModelBiped field_82434_o;
    protected ModelBiped field_82432_p;
    protected ModelBiped field_82437_k;
    protected ModelBiped field_82435_l;
    protected ModelBiped field_82436_m;
    protected ModelBiped field_82433_n;

    public RenderZombie() {
        super(new ModelZombie(), 0.5F, 1.0F);
        this.field_82434_o = modelBipedMain;
    }

    @Override
    protected void func_82421_b() {
        field_82423_g = new ModelZombie(1.0F, true);
        field_82425_h = new ModelZombie(0.5F, true);
        modelArmourChestplate = field_82423_g;
        field_82437_k = field_82423_g;
        field_82435_l = field_82425_h;
        field_82436_m = field_82423_g;
        field_82433_n = field_82425_h;
        field_82432_p = new ModelZombie();
    }

    private static final ResourceLocation ZOMBIE_VILLAGER = new ResourceLocation("textures/entity/zombie/zombie_villager.png");

    /** 1.7.10's typed overload: mods' zombie renderers (Thaumcraft's brainy zombies) override this one, not the EntityLiving bridge. */
    protected ResourceLocation getEntityTexture(legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityZombie zombie) {
        return zombie.isVillager() ? ZOMBIE_VILLAGER : ZOMBIE;
    }

    @Override
    protected ResourceLocation getEntityTexture(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving entity) {
        return entity instanceof legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityZombie z ? getEntityTexture(z) : ZOMBIE;
    }
}
