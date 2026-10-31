package org.kylepitcock.legacyconnector.api;

import java.util.List;
import java.util.Optional;
import org.kylepitcock.legacyconnector.api.internal.AddonHost;
import org.kylepitcock.legacyconnector.api.internal.ModViews;

/**
 * Static facade over the legacy mods Legacy Connector runs: what is loaded, how far loading has come.
 *
 * <pre>
 * if (LegacyApi.isLoaded("thaumcraft")) { ... }
 * LegacyApi.runWhen(LegacyPhase.LOAD_COMPLETE, () -> registerAspects());
 * </pre>
 */
public final class LegacyApi {
    /** Version of this API ({@code major.minor}); incremented in the minor part for additions, the major part for breaking changes. */
    public static final String API_VERSION = "0.1";

    private LegacyApi() {}

    /** Every legacy mod (failed ones included, see {@link LegacyMod#failed()}) in load order. Empty before discovery. */
    public static List<LegacyMod> mods() {
        return ModViews.all();
    }

    /** A legacy mod by its NeoForge id ({@code thaumcraft}) or the id it declared ({@code Thaumcraft}). */
    public static Optional<LegacyMod> mod(String id) {
        return ModViews.find(id);
    }

    /** True if the legacy mod is present and did not fail. Before {@link LegacyPhase#DISCOVERED} no legacy mod is known yet. */
    public static boolean isLoaded(String id) {
        return mod(id).map(m -> !m.failed()).orElse(false);
    }

    /** True if at least one legacy mod runs. */
    public static boolean hasLegacyMods() {
        return !mods().isEmpty();
    }

    /** The phase reached so far. */
    public static LegacyPhase phase() {
        return AddonHost.phase();
    }

    /**
     * Runs {@code action} when {@code phase} is reached: at once if it already was (on the calling thread), else on the main thread
     * right after the phase's addon callbacks. Exceptions are logged.
     */
    public static void runWhen(LegacyPhase phase, Runnable action) {
        AddonHost.runWhen(phase, action);
    }

    /** A class of a legacy mod by name (see {@link LegacyMod#classOf}); empty if the mod or the class does not exist. */
    public static Optional<Class<?>> classOf(String modId, String className) {
        return mod(modId).flatMap(m -> m.classOf(className));
    }
}
