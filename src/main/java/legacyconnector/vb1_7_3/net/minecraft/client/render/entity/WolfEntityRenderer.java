package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.EntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.passive.WolfEntity;

/** Beta 1.7.3 {@code RenderWolf}: the wolf model with its tail angle (raised when angry, by health when tamed). */
public class WolfEntityRenderer extends LivingEntityRenderer {
    public WolfEntityRenderer(EntityModel model, float shadowRadius) { super(model, shadowRadius); }

    @Override protected float getHeadBob(LivingEntity entity, float partial) {
        return entity instanceof WolfEntity wolf ? wolf.getTailAngle() : super.getHeadBob(entity, partial);
    }
}
