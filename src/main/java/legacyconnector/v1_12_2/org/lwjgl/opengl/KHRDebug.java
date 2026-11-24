package legacyconnector.v1_12_2.org.lwjgl.opengl;

/** Legacy {@code org.lwjgl.opengl.KHRDebug}: LWJGL 3's class plus LWJGL 2's {@link KHRDebugCallback} overload. */
public class KHRDebug extends org.lwjgl.opengl.KHRDebug {
    protected KHRDebug() {
    }

    public static void glDebugMessageCallback(KHRDebugCallback callback) {
        KHRDebugCallback.install(callback);
    }
}
