package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.SpiderEntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;

/** Beta 1.7.3 {@code RenderSpider}: the spider model, a half-turn death roll and glowing eyes drawn over it. */
public class SpiderEntityRenderer extends LivingEntityRenderer {
    public SpiderEntityRenderer() { super(new SpiderEntityModel(), 1.0F); setDecorationModel(new SpiderEntityModel()); }

    @Override protected float getDeathYaw(LivingEntity entity) { return 180.0F; }

    @Override protected boolean bindTexture(LivingEntity entity, int pass, float partial) {
        if (pass != 0) return false;
        bindTexture("/mob/spider_eyes.png");
        GL11.glEnable(3042);
        GL11.glBlendFunc(1, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }
}
