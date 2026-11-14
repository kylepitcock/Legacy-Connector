package org.kylepitcock.legacyconnector.api;

import java.nio.file.Path;
import java.util.Optional;

/** A legacy mod running through Legacy Connector. Obtain one from {@link LegacyApi}. */
public interface LegacyMod {
    /** The id NeoForge knows the mod by (lower case, e.g. {@code thaumcraft}, {@code buildcraft_core}). Also its registry namespace. */
    String id();

    /** The id the mod declared itself (e.g. {@code BuildCraft|Core}). */
    String legacyId();

    String name();

    String version();

    /** The Minecraft version the mod was written for. */
    LegacyVersion minecraftVersion();

    /** True if the mod failed in one of its lifecycle phases and was disabled. */
    boolean failed();

    /** The reason the mod was disabled, when it {@link #failed()}. */
    Optional<String> failureMessage();

    /**
     * The required dependencies of this mod that are not installed, one line each ("X requires Y (range), which is not installed"), empty
     * when none (and for Forge 1.20.1 mods, whose dependencies FML checks itself). The mod still loads and usually fails later; see
     * {@link #failed()}. Added in API 0.3.
     */
    default java.util.List<String> missingDependencies() {
        return java.util.List.of();
    }

    /** The lifecycle phase the mod failed in, when it {@link #failed()} (added in API 0.3). */
    default Optional<String> failedPhase() {
        return Optional.empty();
    }

    /** The class loader the mod's classes are loaded by (the mod's main class loader once constructed). */
    ClassLoader classLoader();

    /** A class of the mod (or of a library it embeds) by its binary name; empty if absent. The class is not initialised. */
    Optional<Class<?>> classOf(String className);

    /** The instance of the mod's {@code @Mod} class; empty before construction. */
    Optional<Object> instance();

    /** The mod's jar as the user installed it. */
    Path file();
}
