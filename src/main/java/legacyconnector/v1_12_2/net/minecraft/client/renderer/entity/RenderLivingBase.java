package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

/** Minimal stub (area R1: entity rendering) so RenderLivingEvent can name the type; R1's class replaces it. */
public abstract class RenderLivingBase<T extends legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase> extends Render<T> {
    protected legacyconnector.v1_12_2.net.minecraft.client.model.ModelBase mainModel;

    public RenderLivingBase(RenderManager renderManager, legacyconnector.v1_12_2.net.minecraft.client.model.ModelBase model, float shadowSize) {
        super(renderManager);
        this.mainModel = model;
        this.shadowSize = shadowSize;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.model.ModelBase getMainModel() {
        return mainModel;
    }
}
