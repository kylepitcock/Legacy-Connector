package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityTippedArrow;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 arrow renderer for (tipped) arrows: the tipped texture while the arrow carries a potion colour. */
public class RenderTippedArrow extends RenderArrow<EntityTippedArrow> {
    public static final ResourceLocation RES_ARROW = new ResourceLocation("textures/entity/projectiles/arrow.png");
    public static final ResourceLocation RES_TIPPED_ARROW = new ResourceLocation("textures/entity/projectiles/tipped_arrow.png");

    public RenderTippedArrow(RenderManager manager) {
        super(manager);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityTippedArrow entity) {
        return entity.getColor() > 0 ? RES_TIPPED_ARROW : RES_ARROW;
    }
}
