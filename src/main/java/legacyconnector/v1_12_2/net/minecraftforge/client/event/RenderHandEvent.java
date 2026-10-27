package legacyconnector.v1_12_2.net.minecraftforge.client.event;

/** The first-person hands are about to be drawn (once per frame); cancelling hides both. */
@legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
public class RenderHandEvent extends legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event {
    private final legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal context;
    private final float partialTicks;
    private final int renderPass;

    public RenderHandEvent(legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal context, float partialTicks, int renderPass) {
        this.context = context;
        this.partialTicks = partialTicks;
        this.renderPass = renderPass;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal getContext() {
        return context;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public int getRenderPass() {
        return renderPass;
    }
}
