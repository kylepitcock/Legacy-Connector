package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * Renderer of thrown items (snowballs, eggs, mod grenades): draws the item's icon as a billboard. Constructed by mods when they
 * register their projectile renderers; the billboard drawing is not implemented yet, so such projectiles are invisible.
 */
public class RenderSnowball extends Render {
    private final Item field_94151_a;
    private final int field_94150_f;

    public RenderSnowball(Item item) {
        this(item, 0);
    }

    public RenderSnowball(Item item, int damage) {
        this.field_94151_a = item;
        this.field_94150_f = damage;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
