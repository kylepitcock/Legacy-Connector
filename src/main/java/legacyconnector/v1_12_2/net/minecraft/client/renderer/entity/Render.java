package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

/** Minimal stub (area R1: entity rendering) so client events and {@code Minecraft} can name the type; R1's class replaces it. */
public abstract class Render<T extends legacyconnector.v1_12_2.net.minecraft.entity.Entity> {
    protected final RenderManager renderManager;
    protected float shadowSize;
    protected float shadowOpaque = 1.0F;

    protected Render(RenderManager renderManager) {
        this.renderManager = renderManager;
    }

    public RenderManager getRenderManager() {
        return renderManager;
    }
}
