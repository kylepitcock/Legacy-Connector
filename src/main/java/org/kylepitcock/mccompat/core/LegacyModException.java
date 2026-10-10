package org.kylepitcock.mccompat.core;

/** Wraps a failure inside legacy code so crash reports name the legacy mod and lifecycle phase. */
public final class LegacyModException extends RuntimeException {
    public LegacyModException(LegacyModRecord mod, String phase, Throwable cause) {
        super("Legacy mod " + mod.legacyId + " (" + mod.version + ", from " + safeFile(mod) + ") failed during " + phase
                + ": " + rootMessage(cause), cause);
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
            hint = " (the MC Compatibility shim does not implement this legacy API yet - see logs/mccompat-report.txt)";
        }
        return t + hint;
    }
}
