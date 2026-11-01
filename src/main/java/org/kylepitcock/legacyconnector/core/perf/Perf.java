package org.kylepitcock.legacyconnector.core.perf;

import org.kylepitcock.legacyconnector.core.LegacyConfig;

/**
 * The {@code perf.*} switches of {@code config/legacyconnector.properties}, read once: they are checked on paths that run for every
 * block, entity or tile entity every tick, where {@link LegacyConfig}'s synchronized property lookup would cost more than it saves.
 * A change needs a restart. Each switch guards one optimization of the legacy layer; see the Performance wiki page for what each
 * does and why it keeps legacy behaviour. Lives outside the mixin package (Mixin refuses to load other classes from it).
 */
public final class Perf {
    private Perf() {}

    /** Legacy tile entities that never tick get no modern ticker (LevelChunk drops it). */
    public static final boolean STATIC_TILE_ENTITIES = read("staticTileEntities");
    /** BlockBridge.legacyBlock / legacyMeta answered from a field on the modern block / state, without the bridge's lock. */
    public static final boolean BLOCK_LOOKUP_CACHE = read("blockLookupCache");
    /** ItemBridge.legacyOfModern answered from a field on the modern item (invalidated by a generation counter). */
    public static final boolean ITEM_LOOKUP_CACHE = read("itemLookupCache");
    /** World.mcc$wrap answered from a field on the level. */
    public static final boolean WORLD_VIEW_CACHE = read("worldViewCache");
    /** EntityBridge.wrap answered from a field on the entity. */
    public static final boolean ENTITY_VIEW_CACHE = read("entityViewCache");
    /** Legacy block reads use a per-thread mutable position. */
    public static final boolean REUSE_BLOCK_POS = read("reuseBlockPos");
    /** LegacyHosts.overrides answered from a per-class cache without building a key or a lambda per call. */
    public static final boolean HOOK_OVERRIDE_CACHE = read("hookOverrideCache");

    private static boolean read(String name) {
        try {
            return LegacyConfig.perf(name, true);
        } catch (RuntimeException | LinkageError e) {
            // no FML (offline tools): the default
            return true;
        }
    }
}
