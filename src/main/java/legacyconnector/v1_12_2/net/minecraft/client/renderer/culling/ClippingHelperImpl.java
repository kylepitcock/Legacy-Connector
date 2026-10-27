package legacyconnector.v1_12_2.net.minecraft.client.renderer.culling;

/** 1.12.2's current-frame clipping helper (the modern culling frustum). */
public class ClippingHelperImpl extends ClippingHelper {
    private static final ClippingHelperImpl instance = new ClippingHelperImpl();

    public static ClippingHelper getInstance() {
        instance.init();
        return instance;
    }

    public void init() {
    }
}
