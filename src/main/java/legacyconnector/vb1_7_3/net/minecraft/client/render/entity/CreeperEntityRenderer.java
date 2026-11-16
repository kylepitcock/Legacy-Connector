package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.CreeperEntityModel;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.EntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.mob.CreeperEntity;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;

/**
 * Beta 1.7.3 {@code RenderCreeper}: the creeper model, swelling and flashing white as its fuse burns. Mods register it for their own creepers
 * (Elemental Creepers) or extend it.
 */
public class CreeperEntityRenderer extends LivingEntityRenderer {
    public EntityModel model = new CreeperEntityModel(2.0F);

    public CreeperEntityRenderer() { super(new CreeperEntityModel(), 0.5F); }

    private static float fuse(LivingEntity entity, float partial) {
        if (entity instanceof CreeperEntity creeper) return creeper.getScale(partial);
        return 0;
    }

    @Override protected void applyScale(LivingEntity entity, float partial) {
        float fuse = fuse(entity, partial);
        float wobble = 1.0F + (float) Math.sin(fuse * 100.0F) * fuse * 0.01F;
        fuse = Math.max(0, Math.min(1, fuse));
        fuse *= fuse;
        fuse *= fuse;
        float wide = (1.0F + fuse * 0.4F) * wobble, tall = (1.0F + fuse * 0.1F) / wobble;
        GL11.glScalef(wide, tall, wide);
    }

    @Override protected int getOverlayColor(LivingEntity entity, float brightness, float partial) {
        float fuse = fuse(entity, partial);
        if ((int) (fuse * 10.0F) % 2 == 0) return 0;
        int alpha = Math.max(0, Math.min(255, (int) (fuse * 0.2F * 255.0F)));
        return alpha << 24 | 0xFFFFFF;
    }
}
