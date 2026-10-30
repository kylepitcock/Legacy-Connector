package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelWither;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.boss.EntityWither;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;

/**
 * 1.12.2 wither renderer: twice the model's size (shrinking while it charges up), the invulnerable texture flashing while charging.
 * Vanilla withers (modern entities) are drawn by the modern renderer, which also draws the armour aura.
 */
public class RenderWither extends RenderLiving<EntityWither> {
    private static final ResourceLocation INVULNERABLE_WITHER_TEXTURES = new ResourceLocation("textures/entity/wither/wither_invulnerable.png");
    private static final ResourceLocation WITHER_TEXTURES = new ResourceLocation("textures/entity/wither/wither.png");

    public RenderWither(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelWither(0.0F), 1.0F);
    }

    @Override
    public void doRender(EntityWither entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.mcc$entity != null && !(entity.mcc$entity instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122)
                && getClass() == RenderWither.class) {
            LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
            return;
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityWither entity) {
        int i = entity.getInvulTime();
        return i > 0 && (i > 80 || i / 5 % 2 != 1) ? INVULNERABLE_WITHER_TEXTURES : WITHER_TEXTURES;
    }

    @Override
    protected void preRenderCallback(EntityWither entitylivingbaseIn, float partialTickTime) {
        float f = 2.0F;
        int i = entitylivingbaseIn.getInvulTime();
        if (i > 0) f -= ((float) i - partialTickTime) / 220.0F * 0.5F;
        GlStateManager.scale(f, f, f);
    }
}
