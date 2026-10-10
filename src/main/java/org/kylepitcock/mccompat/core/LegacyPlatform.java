package org.kylepitcock.mccompat.core;

/**
 * One legacy Forge version's lifecycle semantics. Implemented once per shim family; the core decides *when*
 * phases happen, the platform decides *what* a phase means for that version's API.
 */
public interface LegacyPlatform {
    String version();

    /** Load and instantiate the {@code @Mod} class, inject {@code @Instance}/{@code @SidedProxy}/metadata. */
    void construct(LegacyModRecord mod) throws Exception;

    /** Called once before any legacy preInit runs, with all mods of this version constructed. */
    default void beforePreInit() {}

    void preInit(LegacyModRecord mod) throws Exception;

    /** Between preInit and init. 1.12 posts its {@code RegistryEvent.Register<T>} events here. */
    default void registryEvents() {}

    void init(LegacyModRecord mod) throws Exception;

    void postInit(LegacyModRecord mod) throws Exception;

    /** After every mod's postInit: flush deferred registrations (block items, recipes, ...). */
    default void afterPostInit() {}

    default void loadComplete(LegacyModRecord mod) throws Exception {}
}
