package legacyconnector.v1_12_2.net.minecraft.client.renderer.debug;

/** 1.12.2 debug overlays (path finding, water, chunk borders...): the modern game draws its own; none of the legacy ones is shown. */
public class DebugRenderer {
    public boolean shouldRender() {
        return false;
    }

    public void renderDebug(float partialTicks, long finishTimeNano) {
    }
}
