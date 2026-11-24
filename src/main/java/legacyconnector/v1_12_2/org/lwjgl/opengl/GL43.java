package legacyconnector.v1_12_2.org.lwjgl.opengl;

/** Legacy {@code org.lwjgl.opengl.GL43}: LWJGL 3's class plus LWJGL 2's {@link KHRDebugCallback} overload. */
public class GL43 extends org.lwjgl.opengl.GL43 {
    protected GL43() {
    }

    public static void glDebugMessageCallback(KHRDebugCallback callback) {
        KHRDebugCallback.install(callback);
    }
}
