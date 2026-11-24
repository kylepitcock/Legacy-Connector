package legacyconnector.v1_12_2.org.lwjgl.opengl;

/**
 * LWJGL 2's callback object for KHR_debug / GL 4.3 debug output ({@code glDebugMessageCallback(KHRDebugCallback)}). LWJGL 3 passes a native
 * callback instead; {@link KHRDebug} and {@link GL43} adapt this one.
 */
public final class KHRDebugCallback {
    /** Receives the driver's debug messages. */
    public interface Handler {
        void handleMessage(int source, int type, int id, int severity, String message);
    }

    private static volatile Handler defaultHandler = (source, type, id, severity, message) ->
            System.err.println("[KHR_debug] source=" + source + " type=" + type + " id=" + id + " severity=" + severity + ": " + message);

    private final Handler handler;

    public KHRDebugCallback() {
        this(defaultHandler);
    }

    public KHRDebugCallback(Handler handler) {
        this.handler = handler;
    }

    public Handler getHandler() {
        return handler;
    }

    /** Binds this callback to the current context's debug output (a no-op when the driver has no debug output). */
    static void install(KHRDebugCallback callback) {
        try {
            if (callback == null) {
                org.lwjgl.opengl.KHRDebug.glDebugMessageCallback(null, 0L);
                return;
            }
            Handler h = callback.handler;
            org.lwjgl.opengl.GLDebugMessageCallback lwjgl3 = org.lwjgl.opengl.GLDebugMessageCallback.create(
                    (source, type, id, severity, length, message, userParam) ->
                            h.handleMessage(source, type, id, severity, org.lwjgl.opengl.GLDebugMessageCallback.getMessage(length, message)));
            KEEP = lwjgl3; // the native callback must outlive this call
            org.lwjgl.opengl.KHRDebug.glDebugMessageCallback(lwjgl3, 0L);
        } catch (Throwable t) {
            // no debug output on this context: the mod only wanted extra diagnostics
        }
    }

    private static Object KEEP;
}
