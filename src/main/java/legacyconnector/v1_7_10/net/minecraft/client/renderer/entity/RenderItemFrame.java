package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** The item frame renderer as legacy code names it ({@code RenderItemInFrameEvent.renderer}); the modern renderer draws frames. */
public class RenderItemFrame extends Render {
    public static final RenderItemFrame INSTANCE = new RenderItemFrame();

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
