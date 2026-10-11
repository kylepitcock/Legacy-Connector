package mccompat.v1_7_10.net.minecraft.client.renderer.entity;

import mccompat.v1_7_10.net.minecraft.client.model.ModelBiped;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.mccompat.v1_7_10.client.RenderContext;

public class RenderBiped extends RenderLiving {
    protected ModelBiped modelBipedMain;
    protected float field_77071_a;
    protected ModelBiped field_82423_g;
    protected ModelBiped field_82425_h;
    protected ModelBiped modelArmourChestplate;

    public RenderBiped(ModelBiped model, float shadowSize) {
        this(model, shadowSize, 1.0F);
    }

    public RenderBiped(ModelBiped model, float shadowSize, float scale) {
        super(model, shadowSize);
        this.modelBipedMain = model;
        this.field_77071_a = scale;
        func_82421_b();
    }

    protected void func_82421_b() {
        field_82423_g = new ModelBiped(1.0F);
        field_82425_h = new ModelBiped(0.5F);
        modelArmourChestplate = field_82423_g;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return new ResourceLocation("textures/entity/steve.png");
    }

    @Override
    protected void preRenderCallback(mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase entity, float partialTicks) {
        if (field_77071_a != 1.0F) RenderContext.scale(field_77071_a, field_77071_a, field_77071_a);
    }
}
