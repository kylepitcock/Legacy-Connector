package mccompat.v1_7_10.org.lwjgl.opengl;

/** LWJGL 2's context query. */
public final class GLContext {
    private static final ContextCapabilities CAPABILITIES = new ContextCapabilities();

    private GLContext() {}

    public static ContextCapabilities getCapabilities() {
        return CAPABILITIES;
    }
}
