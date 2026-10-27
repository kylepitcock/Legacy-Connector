package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;

/**
 * 1.12.2 item frame renderer. Item frames are modern entities, drawn (frame, item and map) by the modern item frame renderer at the
 * legacy matrix. Typed with {@code Entity} as the shim has no {@code EntityItemFrame} class.
 */
public class RenderItemFrame extends Render<Entity> {
    protected final legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem itemRenderer;

    public RenderItemFrame(RenderManager renderManager, legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem itemRenderer) {
        super(renderManager);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.mcc$entity != null) LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
