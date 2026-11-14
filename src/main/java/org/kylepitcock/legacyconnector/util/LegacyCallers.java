package org.kylepitcock.legacyconnector.util;

import java.net.URI;
import java.util.Optional;

/**
 * Tells whether code on the current stack comes from a jar the loader translated (a legacy mod), so that a compatibility shim can change behaviour for
 * legacy mods only and leave the pack's native mods on NeoForge's own rules. Callers should only ask on their rare path (a stack walk is not free).
 */
public final class LegacyCallers {
    private LegacyCallers() {}

    /** True when any frame of the current stack belongs to a translated legacy jar. */
    public static boolean calledFromTranslatedJar() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(s -> s.map(StackWalker.StackFrame::getDeclaringClass).anyMatch(LegacyCallers::fromTranslatedJar));
    }

    /** True when the class is loaded from a translated jar of the loader's cache (not the layer's own jar). */
    public static boolean fromTranslatedJar(Class<?> c) {
        try {
            Module m = c.getModule();
            if (!m.isNamed() || m.getLayer() == null) return false;
            Optional<URI> loc = m.getLayer().configuration().findModule(m.getName()).flatMap(r -> r.reference().location());
            if (loc.isEmpty()) return false;
            // FML's union file system: union:/<path>/.legacyconnector/cache/<translated jar>.jar%23<n>!/
            String u = loc.get().toString();
            int k = u.indexOf("/.legacyconnector/cache/");
            if (k < 0) return false;
            String name = u.substring(k + "/.legacyconnector/cache/".length());
            return name.contains(".jar") && !name.startsWith("legacyconnector-");
        } catch (RuntimeException e) {
            return false;
        }
    }
}
