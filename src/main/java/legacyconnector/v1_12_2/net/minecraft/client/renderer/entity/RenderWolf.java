package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelWolf;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityWolf;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 wolf renderer: wild, tame and angry textures, darkened while wet, the tail angle as the model's age argument. */
public class RenderWolf extends RenderLiving<EntityWolf> {
    private static final ResourceLocation WOLF_TEXTURES = new ResourceLocation("textures/entity/wolf/wolf.png");
    private static final ResourceLocation TAMED_WOLF_TEXTURES = new ResourceLocation("textures/entity/wolf/wolf_tame.png");
    private static final ResourceLocation ANRGY_WOLF_TEXTURES = new ResourceLocation("textures/entity/wolf/wolf_angry.png");

    public RenderWolf(RenderManager p_i47187_1_) {
        super(p_i47187_1_, new ModelWolf(), 0.5F);
    }

    private static net.minecraft.world.entity.animal.Wolf modern(EntityWolf wolf) {
        return wolf.mcc$entity instanceof net.minecraft.world.entity.animal.Wolf w ? w : null;
    }

    @Override
    protected float handleRotationFloat(EntityWolf livingBase, float partialTicks) {
        var w = modern(livingBase);
        return w == null ? ((float) Math.PI / 5F) : w.getTailAngle();
    }

    @Override
    public void doRender(EntityWolf entity, double x, double y, double z, float entityYaw, float partialTicks) {
        var w = modern(entity);
        if (w != null && w.isWet()) {
            float f = w.getWetShade(partialTicks);
            GlStateManager.color(f, f, f);
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityWolf entity) {
        var w = modern(entity);
        if (w != null && w.isTame()) return TAMED_WOLF_TEXTURES;
        return w != null && w.isAngry() ? ANRGY_WOLF_TEXTURES : WOLF_TEXTURES;
    }
}
