package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

/** Minimal stub (area R1: entity rendering) so RenderPlayerEvent can name the type; R1's class replaces it. */
public class RenderPlayer extends RenderLivingBase<legacyconnector.v1_12_2.net.minecraft.client.entity.AbstractClientPlayer> {
    public RenderPlayer(RenderManager renderManager) {
        this(renderManager, false);
    }

    public RenderPlayer(RenderManager renderManager, boolean useSmallArms) {
        super(renderManager, new legacyconnector.v1_12_2.net.minecraft.client.model.ModelPlayer(0.0F, useSmallArms), 0.5F);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.client.model.ModelPlayer getMainModel() {
        return (legacyconnector.v1_12_2.net.minecraft.client.model.ModelPlayer) super.getMainModel();
    }
}
