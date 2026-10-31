package org.kylepitcock.legacyconnector.api;

import java.util.List;

/**
 * Entry point of an addon for legacy mods. Implement it in a class with a public no-argument constructor and declare it in the
 * addon's {@code META-INF/neoforge.mods.toml}:
 *
 * <pre>
 * [modproperties.my_addon_modid]
 * legacyconnector-addon = "com.example.MyAddon"        # or a list: ["com.example.A", "com.example.B"]
 * </pre>
 *
 * (A {@code META-INF/services/org.kylepitcock.legacyconnector.api.LegacyAddon} file works as well.) The addon is a normal NeoForge mod;
 * Legacy Connector instantiates this class once and calls it from the phases below, always on the main thread. An exception in a
 * callback is logged and does not stop the game or the other addons.
 *
 * <p>All callbacks have empty defaults. See {@link LegacyPhase} for what each phase means.
 */
public interface LegacyAddon {
    /** Lower values run first; ties are broken by class name. */
    default int priority() {
        return 0;
    }

    /**
     * Legacy mod ids (see {@link LegacyMod#id()}) that must be present and not failed for this addon to be active. While one is
     * missing the addon gets no callback at all (and its capability adapters are not registered).
     */
    default List<String> requiredLegacyMods() {
        return List.of();
    }

    /** Phase {@link LegacyPhase#DISCOVERED}. */
    default void onLegacyModsDiscovered() {}

    /** Phase {@link LegacyPhase#REGISTERED}. */
    default void onLegacyRegistered() {}

    /** Phase {@link LegacyPhase#LOAD_COMPLETE}. */
    default void onLegacyLoadComplete() {}

    /**
     * Called while NeoForge's {@code RegisterCapabilitiesEvent} is fired: declare which legacy tile entities provide which
     * NeoForge capabilities. Legacy mods are fully loaded then.
     */
    default void registerCapabilities(LegacyCapabilities capabilities) {}
}
