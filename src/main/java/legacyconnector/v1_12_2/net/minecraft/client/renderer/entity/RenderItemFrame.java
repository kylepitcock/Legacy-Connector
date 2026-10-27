package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

/** Minimal stub (area R1: entity rendering) so RenderItemInFrameEvent can name the type; R1's class replaces it. */
public class RenderItemFrame extends Render<legacyconnector.v1_12_2.net.minecraft.entity.Entity> {
    protected final legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem itemRenderer;

    public RenderItemFrame(RenderManager renderManager, legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem itemRenderer) {
        super(renderManager);
        this.itemRenderer = itemRenderer;
    }
}
