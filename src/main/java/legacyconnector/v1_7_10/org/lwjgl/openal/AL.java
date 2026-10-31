package legacyconnector.v1_7_10.org.lwjgl.openal;

/**
 * LWJGL 2's OpenAL context holder. The game creates the context (LWJGL 3's own {@code AL}), so legacy code that asks whether it exists gets
 * the answer for that one, and legacy code that creates or destroys it changes nothing. The rest of OpenAL ({@code AL10}...) is LWJGL 3's own class.
 */
public final class AL {
    private AL() {}

    public static boolean isCreated() {
        try {
            return org.lwjgl.openal.AL.getCapabilities() != null;
        } catch (Throwable e) { // no sound system (a dedicated server, or a game that did not start OpenAL)
            return false;
        }
    }

    public static void create() {
        // the game owns the context
    }

    public static void create(String deviceArguments, int contextFrequency, int contextRefresh, boolean contextSynchronized) {
        // the game owns the context
    }

    public static void destroy() {
        // the game owns the context
    }
}
