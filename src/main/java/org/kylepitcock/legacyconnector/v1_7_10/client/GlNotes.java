package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.kylepitcock.legacyconnector.core.degraded.Degraded;
import org.kylepitcock.legacyconnector.core.degraded.DegradedOwners;

/**
 * Reports to {@link Degraded} where the legacy GL emulation is not the real thing: queries the emulation does not answer (they read the
 * real core-profile context, which does not know what the legacy code set), and entry points that are empty or only approximate.
 *
 * <p>Called on every such GL call, so the cost matters: one map lookup and a counter. The calling mod (the first mod frame on the stack)
 * is looked up with a stack walk for the first {@link #WALK_FIRST} calls of each (function, pname) and then every {@link #WALK_EVERY}th
 * call; in between the last answer is reused, so a function called by two mods may attribute some calls to the wrong one.
 */
public final class GlNotes {
    private static final int WALK_FIRST = 16;
    private static final int WALK_EVERY = 512;
    private static final String LAYER = "(layer)";

    private static final class Site {
        final String fn;
        final int pname;
        final int kind;
        final AtomicLong calls = new AtomicLong();
        volatile String owner = LAYER;
        volatile String key;
        volatile String message;

        Site(String fn, int pname, int kind) {
            this.fn = fn;
            this.pname = pname;
            this.kind = kind;
        }
    }

    private static final ConcurrentHashMap<Long, Site> SITES = new ConcurrentHashMap<>();

    private GlNotes() {}

    /** A query ({@code glGetInteger}, {@code glIsEnabled}...) of a state the emulation does not hold; the real context answered. */
    public static void realQuery(String fn, int pname) {
        note(fn, pname, REAL);
    }

    /** A query of fixed-function state that has no core-profile equivalent: the emulation answers with zero/false. */
    public static void zeroQuery(String fn, int pname) {
        note(fn, pname, ZERO);
    }

    /** An entry point that does nothing or only approximates in this layer (fixed-function state with no core-profile equivalent). */
    public static void unemulated(String fn) {
        note(fn, 0, NOOP);
    }

    private static final int REAL = 0, ZERO = 1, NOOP = 2;

    private static void note(String fn, int pname, int kind) {
        try {
            long id = ((long) fn.hashCode() << 32) ^ (pname & 0xFFFFFFFFL) ^ ((long) kind << 62);
            Site site = SITES.get(id);
            if (site == null) site = SITES.computeIfAbsent(id, k -> new Site(fn, pname, kind));
            long n = site.calls.incrementAndGet();
            if (n <= WALK_FIRST || n % WALK_EVERY == 0) {
                String owner = callingMod();
                if (!owner.equals(site.owner) || site.key == null) {
                    site.owner = owner;
                    String hex = "0x" + Integer.toHexString(pname);
                    site.key = kind == NOOP ? fn : fn + "(" + hex + ")" + (kind == ZERO ? " = 0" : "");
                    site.message = switch (kind) {
                        case REAL -> fn + " reads the real GL context for " + hex + ", which is not the state the legacy code set (not emulated)";
                        case ZERO -> fn + " answers " + hex + " with zero: fixed-function state this layer does not track";
                        default -> fn + " has no effect in this layer (fixed-function state with no core-profile equivalent)";
                    };
                }
            }
            String key = site.key;
            if (key != null) Degraded.info(Degraded.Category.GL_EMULATION, site.owner, key, site.message);
        } catch (RuntimeException | LinkageError ignored) {
            // diagnostics only
        }
    }

    private static String callingMod() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(frames -> frames
                .map(f -> DegradedOwners.ofMod(f.getDeclaringClass()))
                .filter(m -> m != null)
                .findFirst().orElse(LAYER));
    }

    /** Clears the per-site counters (tests). */
    static void reset() {
        SITES.clear();
    }
}
