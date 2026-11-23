package org.kylepitcock.legacyconnector.core.degraded;

import java.util.function.Supplier;

/**
 * Calls into legacy (mod) code that may throw: the failure is counted and logged once per (mod, site, legacy class) through
 * {@link Degraded}, and the caller gets its fallback value. Two forms:
 * <ul>
 *   <li>{@link #get}/{@link #run}: wrap the call (allocates a lambda; for code that is not on a per-frame hot path);</li>
 *   <li>{@link #failed}/{@link #expectedFailure}: for an existing {@code catch} block, so the fallback expression and the hot path stay
 *       exactly as they were: {@code catch (RuntimeException | LinkageError e) { return LegacyCall.failed("getLightBlock", legacy, e, super.getLightBlock(...)); }}</li>
 * </ul>
 * {@code site} is the legacy member or callback name; {@code legacy} is the legacy object (its class loader names the owning mod).
 */
public final class LegacyCall {
    private LegacyCall() {}

    /** Runs the call; on a runtime exception or linkage error notes it and returns {@code fallback}. */
    public static <T> T get(String site, Object legacy, T fallback, Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException | LinkageError e) {
            return failed(site, legacy, e, fallback);
        }
    }

    /** As {@link #get} for a call without a result. */
    public static void run(String site, Object legacy, Runnable call) {
        try {
            call.run();
        } catch (RuntimeException | LinkageError e) {
            failed(site, legacy, e, null);
        }
    }

    /** Notes a failed legacy call (WARN once per mod, site and legacy class) and returns the fallback. */
    public static <T> T failed(String site, Object legacy, Throwable e, T fallback) {
        note(site, legacy, e, true);
        return fallback;
    }

    /** As {@link #failed} but only INFO: failures that are part of normal operation (bounds read without a world, a mod's own throw on odd metadata). */
    public static <T> T expectedFailure(String site, Object legacy, Throwable e, T fallback) {
        note(site, legacy, e, false);
        return fallback;
    }

    private static void note(String site, Object legacy, Throwable e, boolean warn) {
        try {
            Class<?> cls = legacy instanceof Class<?> c ? c : legacy == null ? null : legacy.getClass();
            String owner = DegradedOwners.of(cls);
            String key = cls == null ? site : site + " of " + cls.getName();
            Supplier<String> message = () -> site + " threw " + describe(e) + "; fallback value used";
            if (warn) Degraded.note(Degraded.Category.LEGACY_CALL, owner, key, message);
            else Degraded.info(Degraded.Category.LEGACY_CALL, owner, key, message);
        } catch (RuntimeException | LinkageError ignored) {
            // the channel never breaks the caller
        }
    }

    private static String describe(Throwable e) {
        StringBuilder sb = new StringBuilder(e.toString());
        StackTraceElement[] st = e.getStackTrace();
        if (st.length > 0) sb.append(" at ").append(st[0]);
        return sb.toString();
    }
}
