package dev.latvian.mods.rhino;

/** Minimal test fixture for Rhino's current-context accessor. */
public final class Context {
    private static final Context CURRENT = new Context();

    private Context() {
    }

    public static Context getCurrentContext() {
        return CURRENT;
    }
}
