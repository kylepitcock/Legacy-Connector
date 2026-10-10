package org.kylepitcock.mccompat.compat.jdk;

/** Replacement for {@code java.lang.Compiler}, a no-op since forever and removed in Java 21. */
public final class Compiler {
    private Compiler() {}

    public static boolean compileClass(Class<?> clazz) {
        return false;
    }

    public static boolean compileClasses(String string) {
        return false;
    }

    public static Object command(Object any) {
        return null;
    }

    public static void enable() {}

    public static void disable() {}
}
