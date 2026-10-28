package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelParrot;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityParrot;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/** 1.12.2 parrot renderer: the five colour variants; the wing flap is passed as the model's age argument. */
public class RenderParrot extends RenderLiving<EntityParrot> {
    public static final ResourceLocation[] PARROT_TEXTURES = new ResourceLocation[] {new ResourceLocation("textures/entity/parrot/parrot_red_blue.png"),
        new ResourceLocation("textures/entity/parrot/parrot_blue.png"), new ResourceLocation("textures/entity/parrot/parrot_green.png"),
        new ResourceLocation("textures/entity/parrot/parrot_yellow_blue.png"), new ResourceLocation("textures/entity/parrot/parrot_grey.png")};

    public RenderParrot(RenderManager p_i47375_1_) {
        super(p_i47375_1_, new ModelParrot(), 0.3F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityParrot entity) {
        int v = entity.getVariant();
        return PARROT_TEXTURES[Math.floorMod(v, PARROT_TEXTURES.length)];
    }

    @Override
    public float handleRotationFloat(EntityParrot livingBase, float partialTicks) {
        if (!(livingBase.mcc$entity instanceof net.minecraft.world.entity.animal.Parrot p)) return 0.0F;
        float flap = p.oFlap + (p.flap - p.oFlap) * partialTicks;
        float speed = p.oFlapSpeed + (p.flapSpeed - p.oFlapSpeed) * partialTicks;
        return (MathHelper.sin(flap) + 1.0F) * speed;
    }
}
