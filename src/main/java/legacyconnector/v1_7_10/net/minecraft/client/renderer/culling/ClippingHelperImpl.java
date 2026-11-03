package legacyconnector.v1_7_10.net.minecraft.client.renderer.culling;

/** 1.7.10's current-frame clipping helper; its tests go through the modern culling frustum unless a subclass fills the planes. */
public class ClippingHelperImpl extends ClippingHelper {
    private static final ClippingHelperImpl instance = new ClippingHelperImpl();

    public static ClippingHelper getInstance() {
        instance.init();
        return instance;
    }

    public void init() {
    }
}
