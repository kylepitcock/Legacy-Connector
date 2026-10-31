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

    /** The class loader the mod's classes are loaded by (the mod's main class loader once constructed). */
    ClassLoader classLoader();

    /** A class of the mod (or of a library it embeds) by its binary name; empty if absent. The class is not initialised. */
    Optional<Class<?>> classOf(String className);

    /** The instance of the mod's {@code @Mod} class; empty before construction. */
    Optional<Object> instance();

    /** The mod's jar as the user installed it. */
    Path file();
}
