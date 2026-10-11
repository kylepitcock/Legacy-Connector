package mccompat.v1_7_10.net.minecraft.client.renderer.entity;

import mccompat.v1_7_10.net.minecraft.client.model.ModelBiped;
import mccompat.v1_7_10.net.minecraft.client.model.ModelZombie;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.util.ResourceLocation;

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

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return ZOMBIE;
    }
}
