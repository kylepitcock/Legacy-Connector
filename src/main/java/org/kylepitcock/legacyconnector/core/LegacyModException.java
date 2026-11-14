package org.kylepitcock.legacyconnector.core;

/** Wraps a failure inside legacy code so crash reports name the legacy mod and lifecycle phase. */
public final class LegacyModException extends RuntimeException {
    private final String phase;

    /** The lifecycle phase the mod failed in (construction, preInit, init, imc, postInit, loadComplete). */
    public String phase() {
        return phase;
    }

    public LegacyModException(LegacyModRecord mod, String phase, Throwable cause) {
        super(missing(mod) + "Legacy mod " + mod.legacyId + " (" + mod.version + ", from " + safeFile(mod) + ") failed during " + phase
                + ": " + rootMessage(cause), cause);
        this.phase = phase;
    }

    /** "X requires Y (range), which is not installed. " for each required dependency the loader found missing: the likely cause of the failure. */
    private static String missing(LegacyModRecord mod) {
        StringBuilder sb = new StringBuilder();
        for (String m : LegacyLifecycle.missingDependencies(mod.legacyId)) sb.append(m).append(". ");
        return sb.toString();
    }

    private static String safeFile(LegacyModRecord mod) {
        try {
            return mod.sourceFile().getFileName().toString();
        } catch (Exception e) {
            return "?";
        }
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null && t.getCause() != t && (t instanceof java.lang.reflect.InvocationTargetException || t.getMessage() == null)) {
            t = t.getCause();
        }
        String hint = "";
        if (t instanceof NoSuchMethodError || t instanceof NoSuchFieldError || t instanceof NoClassDefFoundError || t instanceof AbstractMethodError) {
            hint = " (the Legacy Connector layer does not implement this legacy API yet - see logs/legacyconnector-report.txt)";
        }
        String plain = plainReason(t);
        return plain != null ? plain + " [" + t + "]" : t + hint;
    }

    /** A plain-words reason for the failures that are not layer defects (null otherwise). */
    static String plainReason(Throwable t) {
        String m = String.valueOf(t.getMessage());
        if (m.contains("net/java/games/input") || m.contains("net.java.games.input")) {
            return "needs LWJGL joystick libs (JInput, net.java.games.input), which this game does not ship";
        }
        if (t instanceof ClassNotFoundException && m.startsWith("mod_")) {
            return "does not start: patched-game jar mod (it replaces vanilla classes of minecraft.jar, which the layer cannot express)";
        }
        return null;
    }
}
