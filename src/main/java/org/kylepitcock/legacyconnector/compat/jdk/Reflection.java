package org.kylepitcock.legacyconnector.compat.jdk;

/**
 * Replacement for {@code sun.reflect.Reflection}, encapsulated since Java 9 and removed later.
 * Legacy code used {@code getCallerClass(int)} to find out who called it.
 */
public final class Reflection {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private Reflection() {}

    /**
     * Depth semantics of the old API: 0 = Reflection itself, 1 = the method calling getCallerClass,
     * 2 = its caller, and so on.
     */
    public static Class<?> getCallerClass(int depth) {
        return WALKER.walk(frames -> frames.skip(Math.max(0, depth)).findFirst().map(StackWalker.StackFrame::getDeclaringClass).orElse(null));
    }

    public static Class<?> getCallerClass() {
        return getCallerClass(3);
    }

    public static int getClassAccessFlags(Class<?> c) {
        return c.getModifiers();
    }
}
