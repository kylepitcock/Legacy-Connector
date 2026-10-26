package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import net.minecraft.client.Minecraft;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * What {@link RenderManager} answers for an entity no legacy renderer is registered for (1.7.10 mapped every vanilla entity to
 * its renderer, and Entity itself to a fallback, so lookups never failed): the modern entity behind the legacy one, drawn by
 * the modern renderer in the current legacy pose. Mods draw entities this way inside their own renderers.
 */
public class RenderModernEntity extends Render {
    public static final RenderModernEntity INSTANCE = new RenderModernEntity();

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        var pose = RenderContext.pose();
        var modern = entity == null ? null : entity.mcc$entity;
        // a hosted legacy entity is drawn by its own legacy renderer or not at all (drawing the host would come back here)
        if (pose == null || modern == null || modern instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHost) return;
        try {
            Minecraft.getInstance().getEntityRenderDispatcher().render(modern, x, y, z, yaw, partialTicks, pose, RenderContext.buffers(), RenderContext.light());
        } catch (RuntimeException e) {
            // an entity a mod built only for drawing may lack what its modern renderer reads; draw nothing, as before
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
