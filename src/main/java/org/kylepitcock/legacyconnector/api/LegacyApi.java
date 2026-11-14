package org.kylepitcock.legacyconnector.api;

import java.util.List;
import java.util.Optional;
import org.kylepitcock.legacyconnector.api.internal.AddonHost;
import org.kylepitcock.legacyconnector.api.internal.ModViews;
import org.kylepitcock.legacyconnector.api.internal.VersionLookup;

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
    public static final String API_VERSION = "0.3";

    private LegacyApi() {}

    /** Every legacy mod (failed ones included, see {@link LegacyMod#failed()}) in load order. Empty before discovery. */
    public static List<LegacyMod> mods() {
        return ModViews.all();
    }

    /** The legacy mods written for one Minecraft version (added in API 0.2). */
    public static List<LegacyMod> mods(LegacyVersion version) {
        return mods().stream().filter(m -> m.minecraftVersion() == version).toList();
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

    /** The legacy mods that failed and were disabled (added in API 0.3). */
    public static List<LegacyMod> failedMods() {
        return mods().stream().filter(LegacyMod::failed).toList();
    }

    /**
     * The legacy Minecraft version something belongs to (added in API 0.3). Accepts a {@link LegacyMod}, a registry id
     * ({@code ResourceLocation}: by its namespace), a modern {@code ItemStack}, {@code Item}, {@code Block}, {@code BlockState} or
     * {@code BlockEntity} (by the mod that owns it), a legacy object of the shim or of a mod, or a {@code Class} of those (a mod's class
     * by its class loader, which tells 1.4.7, 1.5.2 and 1.6.4 apart from 1.7.10; a plain shim object only by its shim, where the 1.4.7 to
     * 1.6.4 family answers {@link LegacyVersion#V1_7_10}). Empty for vanilla, modern and unknown objects.
     */
    public static Optional<LegacyVersion> versionOf(Object object) {
        return VersionLookup.of(object);
    }

    /** The legacy version of the mod that owns a registry namespace ({@code "thaumcraft"}), empty for vanilla and modern mods (added in API 0.3). */
    public static Optional<LegacyVersion> versionOfNamespace(String namespace) {
        return VersionLookup.ofNamespace(namespace);
    }

    /** A class of a legacy mod by name (see {@link LegacyMod#classOf}); empty if the mod or the class does not exist. */
    public static Optional<Class<?>> classOf(String modId, String className) {
        return mod(modId).flatMap(m -> m.classOf(className));
    }
}
