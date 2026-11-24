package legacyconnector.v1_12_2.mezz.jei.util;

/** JEI 4 (Minecraft 1.12.2) internals some plugins reach into; here a small working copy. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public final class ErrorUtil {
    private ErrorUtil() {}

    public static <T> T checkNotNull(T object, String name) {
        if (object == null) throw new NullPointerException(name + " must not be null.");
        return object;
    }

    public static void checkNotEmpty(Object[] values, String name) {
        if (values == null || values.length == 0) throw new IllegalArgumentException(name + " must not be empty.");
    }
}
