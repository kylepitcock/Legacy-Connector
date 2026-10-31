package org.kylepitcock.legacyconnector.api;

/**
 * How far the legacy mods have come. The phases follow each other in this order, each reached once per game launch.
 *
 * <pre>
 * NOT_STARTED -> DISCOVERED -> REGISTERED -> LOAD_COMPLETE
 * </pre>
 */
public enum LegacyPhase {
    /** Before the last registry event; no legacy mod is constructed. */
    NOT_STARTED,
    /**
     * All legacy mods are known ({@link LegacyApi#mods()}) but none is constructed yet (no legacy preInit has run). Classes can be
     * looked up, but static initialisers of mod classes may not have run.
     */
    DISCOVERED,
    /**
     * Every legacy mod ran preInit, init and postInit and the deferred registrations (block items, recipes) were flushed.
     * NeoForge registries are still open here (this runs inside the last {@code RegisterEvent}); legacy mods' own data (aspects, ore
     * dictionary entries, recipes) is complete, the modern registries of other mods are filled.
     */
    REGISTERED,
    /**
     * The legacy {@code loadComplete} ran (main thread, {@code FMLCommonSetupEvent} work queue). Registries are frozen. This is the
     * best moment to feed modern content into a legacy mod's API.
     */
    LOAD_COMPLETE
}
