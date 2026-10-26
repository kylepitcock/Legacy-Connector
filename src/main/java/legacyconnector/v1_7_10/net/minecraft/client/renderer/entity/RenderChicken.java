package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityChicken;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** 1.7.10's chicken renderer (also the base of mod birds): the age value fed to the model is the wing flap. */
public class RenderChicken extends RenderLiving {
    private static final ResourceLocation chickenTextures = new ResourceLocation("textures/entity/chicken.png");

    public RenderChicken(ModelBase model, float shadowSize) {
        super(model, shadowSize);
    }

    protected ResourceLocation getEntityTexture(EntityChicken chicken) {
        return chickenTextures;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntityChicken c ? getEntityTexture(c) : chickenTextures;
    }

    /** The wing angle: the flap phase and how far the wings are spread (a vanilla chicken's own values when it is one). */
    protected float handleRotationFloat(EntityChicken chicken, float partialTicks) {
        if (chicken.mcc$entity instanceof net.minecraft.world.entity.animal.Chicken modern) {
            float flap = net.minecraft.util.Mth.lerp(partialTicks, modern.oFlap, modern.flap);
            float speed = net.minecraft.util.Mth.lerp(partialTicks, modern.oFlapSpeed, modern.flapSpeed);
            return (net.minecraft.util.Mth.sin(flap) + 1.0F) * speed;
        }
        float flap = chicken.field_70888_h + (chicken.field_70886_e - chicken.field_70888_h) * partialTicks;
        float spread = chicken.field_70884_g + (chicken.destPos - chicken.field_70884_g) * partialTicks;
        return ((float) Math.sin(flap) + 1.0F) * spread;
    }

    @Override
    protected float handleRotationFloat(EntityLivingBase entity, float partialTicks) {
        return entity instanceof EntityChicken c ? handleRotationFloat(c, partialTicks) : super.handleRotationFloat(entity, partialTicks);
    }
}
