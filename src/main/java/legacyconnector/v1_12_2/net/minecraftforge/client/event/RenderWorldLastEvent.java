package legacyconnector.v1_12_2.net.minecraftforge.client.event;

/** The world has been drawn: handlers draw in world space relative to the camera (as 1.12 did). */
public class RenderWorldLastEvent extends legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event {
    private final legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal context;
    private final float partialTicks;

    public RenderWorldLastEvent(legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal context, float partialTicks) {
        this.context = context;
        this.partialTicks = partialTicks;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal getContext() {
        return context;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
