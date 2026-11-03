package org.kylepitcock.legacyconnector.core;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User settings, read from {@code config/legacyconnector.properties}. The file is written with its defaults and comments on
 * first start; missing or malformed keys fall back to the defaults. It is a plain properties file (not a NeoForge config)
 * because the resource pack is generated before NeoForge configs are loaded.
 */
public final class LegacyConfig {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Config");
    private static final String FILE = "legacyconnector.properties";
    private static final String DEFAULTS = """
            # Legacy Connector settings. Delete a line to restore its default.

            # Replace the old stone / netherrack / end stone behind legacy ore textures with a modern block texture.
            oreBackgrounds.enabled = true
            # The vanilla block texture (textures/block/<name>.png) that replaces each old host rock, or "legacy" to keep it.
            # Any block texture works, e.g. deepslate, tuff, andesite, blackstone, basalt_side.
            oreBackgrounds.stone = stone
            oreBackgrounds.netherrack = netherrack
            oreBackgrounds.endStone = end_stone

            # Replace old vanilla art inside legacy mod textures with the modern vanilla textures: exact copies of old block, item
            # and GUI textures, and old backgrounds (smooth stone, cobblestone, planks, ...) that mods painted their art on.
            modernizeTextures = true
            # Rebuild legacy leaf textures that are recolours of the old vanilla leaves (most mod trees) from the modern vanilla
            # leaves, in the mod's colours (fancy and fast variants). Hand-drawn leaves and animated textures stay as they are.
            textures.modernLeaves = true

            # Legacy worlds ended at y=0. When enabled, legacy world generators (ores, caves, ...) run a second time shifted
            # down so that they also fill the modern world below y=0 (legacy y=0 is modern y=-64).
            deepLayer.enabled = true
            # Comma separated class name prefixes of legacy world generators that must not run below y=0.
            deepLayer.excludeGenerators =

            # What legacy world generators build in place (shrines, towers, huts) is recorded as a modern structure where it is
            # built, so /locate structure <mod>:legacy/<name> finds the ones in chunks that have generated.
            structures.locatable = true

            # Legacy block subtypes (metadata variants with an item of their own) become blocks of their own, as in the 1.13
            # flattening: the item mod:name_3 places and names the block mod:name_3, so modern mods that use an item's block
            # (display stands, framed and copycat blocks) get the right subtype. false: one block per legacy block, as before
            # (the subtype blocks stay registered, and their blocks in saved worlds turn back into the base block on chunk load).
            blocks.flattenSubtypes = true

            # Library bridges replace an outdated legacy library by its modern equivalent when that mod is installed.
            # Set one to false to load the legacy library itself instead (read by the loader at startup).
            bridges.baubles.enabled = true
            bridges.waila.enabled = true
            bridges.nei.enabled = true
            bridges.cofhEnergy.enabled = true

            # Performance. perf.* switches of Legacy Connector's own work are on by default (they change nothing but its speed);
            # see docs/wiki/Performance.md.
            # The generated legacy data and asset packs are not built when the game only reads their metadata (once per pack scan).
            perf.lazyPackMetadata = true
            # The vanilla biome templates and biome tags that legacy biomes are generated from are read from the game's jars once per
            # session instead of on every data pack build.
            perf.biomeJarCache = true
            # The diagnostic sample of legacy biomes around spawn ("Biome sample around spawn" in the log) runs in the background
            # instead of on the server thread while the player joins.
            perf.asyncBiomeSample = true

            # Pack maker switches, read by the loader at startup.
            # Legacy mods to leave out entirely (their legacy ids, comma separated; the whole jar is skipped when all its mods are listed).
            # mods.skip = examplemod, othermod
            # Treat a dependency on one legacy id as satisfied by another (ordering and the generated mod metadata), e.g. when a mod was
            # renamed or a library is provided under another id. One line per id.
            # mods.alias.oldid = newid

            # Energy conversion for legacy tiles and items exposed as NeoForge energy (FE). RF is 1:1 with FE.
            # FE per IC2 EU (IC2 machines accept energy from modern cables at this rate).
            energy.fePerEu = 4
            # FE per BuildCraft MJ (BuildCraft's own rule: 1 MJ = 10 RF).
            energy.fePerMj = 10
            # Per direction (each defaults to the single key above; 0 turns that direction off). "in" = FE received by a legacy tile
            # or item (modern cables/generators powering legacy machines), "out" = FE extracted from one (legacy generators feeding
            # modern cables). Stored/capacity readouts keep using the single key.
            # energy.fePerEu.in = 4
            # energy.fePerEu.out = 4
            # energy.fePerMj.in = 10
            # energy.fePerMj.out = 10

            # Memory optimizations (see the Performance page of the wiki). Each can be switched off on its own.
            # Release the legacy views of unloaded levels and stopped servers (old client levels after a dimension change).
            perf.releaseUnloadedLevels = true
            # Release the legacy views of players that died or left (after two sweeps, so a respawn in progress keeps its view).
            perf.sweepRemovedPlayers = true
            # Release cached legacy views of closed connections and of inventories whose block was removed or unloaded.
            perf.sweepDeadViews = true
            # 1.12.2 item stack views without capabilities are held weakly (off: a tag kept without its stack could lose changes).
            perf.weakStackViews1122 = false
            # The loader drops its translation tables after mod discovery (read by the loader at startup).
            perf.releaseLoaderData = true
            # One loading warning listing translated mods with unresolved references or heavy stub use, shown once per set of mods.
            warnings.riskyMods = true
            # Tick performance of the legacy layer. Each switch turns one optimization off (false) if it ever misbehaves;
            # all of them are on by default because they keep 1.7.10/1.12.2 behaviour. See the Performance wiki page.
            # Legacy tile entities that never tick (1.7.10 canUpdate() false or no updateEntity, 1.12.2 not ITickable) get no ticker.
            perf.staticTileEntities = true
            # The legacy block (and vanilla metadata) of a modern block state is remembered on the block and the state.
            perf.blockLookupCache = true
            # The legacy item of a modern item (every legacy view of a vanilla or modern-mod stack) is remembered on the item.
            perf.itemLookupCache = true
            # The legacy world view of a level is remembered on the level.
            perf.worldViewCache = true
            # The legacy entity view of a modern entity is remembered on the entity.
            perf.entityViewCache = true
            # Legacy block reads (getBlock, getBlockMetadata) reuse one position object per thread instead of allocating one.
            perf.reuseBlockPos = true
            # "Does this legacy entity class override hook X" answers are cached per class without allocating per call.
            perf.hookOverrideCache = true
            # Client performance switches (see the Performance page of the wiki). Each can be turned off on its own.
            # Legacy particles drawn by the built-in particle classes are skipped outside the view (they look the same).
            perf.cullLegacyParticles = true
            # The same for particles that mods draw themselves (beams can reach into the view from outside: off by default).
            perf.cullModParticles = false
            # Legacy code reads the vanilla particle setting (All / Decreased / Minimal) as 1.7.10's particleSetting.
            perf.particleSetting = true
            # Metadata values of a legacy block whose models are identical share one model (baked once, not per value).
            perf.shareBlockModels = true
            # Legacy items with identical item models keep one baked model (skipped with ModernFix, which bakes on demand).
            perf.shareItemModels = true
            # Generated resource and data pack files are kept on disk (.legacyconnector/pack-spill) once read, not in memory.
            perf.packSpill = true
            # A legacy mod texture location remembers the modern one it binds (no new location per bind and frame).
            perf.cacheTextureLocations = true
            """;
    private static Properties values;

    private LegacyConfig() {}

    private static synchronized Properties values() {
        if (values != null) return values;
        Properties p = new Properties();
        try {
            Path file = FMLPaths.CONFIGDIR.get().resolve(FILE);
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    p.load(r);
                }
            } else {
                Files.writeString(file, DEFAULTS);
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not read {}; using the defaults: {}", FILE, e.toString());
        }
        return values = p;
    }

    private static String string(String key, String fallback) {
        String v = values().getProperty(key);
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    private static boolean bool(String key, boolean fallback) {
        String v = string(key, null);
        if (v == null) return fallback;
        if (v.equalsIgnoreCase("true")) return true;
        if (v.equalsIgnoreCase("false")) return false;
        LOG.warn("{}: '{}' is not true or false; using {}", key, v, fallback);
        return fallback;
    }

    private static int positiveInt(String key, int fallback) {
        String v = string(key, null);
        if (v == null) return fallback;
        try {
            int n = Integer.parseInt(v);
            if (n > 0) return n;
        } catch (NumberFormatException e) {
            // reported below
        }
        LOG.warn("{}: '{}' is not a positive whole number; using {}", key, v, fallback);
        return fallback;
    }

    /**
     * {@code perf.<name>}: one tick-performance optimization (see {@code core.perf.Perf}, which reads these once). Unknown or
     * malformed values keep the default.
     */
    public static boolean perf(String name, boolean fallback) {
        return bool("perf." + name, fallback);
    }

    /** {@code mods.alias.<id> = <other id>}: a legacy dependency on the first id is satisfied by the second (the id itself when none). */
    public static String modAlias(String legacyId) {
        String other = string("mods.alias." + legacyId.toLowerCase(java.util.Locale.ROOT), null);
        return other != null ? other : legacyId;
    }

    /** FE per IC2 EU for the energy capability of IC2 tiles and items. */
    public static int fePerEu() {
        return positiveInt("energy.fePerEu", 4);
    }

    /** FE per BuildCraft MJ for the energy capability of BuildCraft power receptors. */
    public static int fePerMj() {
        return positiveInt("energy.fePerMj", 10);
    }

    /** FE per EU for FE received by a legacy IC2 tile or item (modern -> legacy); 0 = off. Defaults to {@link #fePerEu()}. */
    public static int fePerEuIn() {
        return nonNegativeInt("energy.fePerEu.in", fePerEu());
    }

    /** FE per EU for FE extracted from a legacy IC2 tile or item (legacy -> modern); 0 = off. Defaults to {@link #fePerEu()}. */
    public static int fePerEuOut() {
        return nonNegativeInt("energy.fePerEu.out", fePerEu());
    }

    /** FE per MJ for FE received by a BuildCraft power receptor; 0 = off. Defaults to {@link #fePerMj()}. */
    public static int fePerMjIn() {
        return nonNegativeInt("energy.fePerMj.in", fePerMj());
    }

    /** FE per MJ for FE extracted from a BuildCraft tile; 0 = off. Defaults to {@link #fePerMj()}. */
    public static int fePerMjOut() {
        return nonNegativeInt("energy.fePerMj.out", fePerMj());
    }

    private static int nonNegativeInt(String key, int fallback) {
        String v = string(key, null);
        if (v == null) return fallback;
        try {
            int n = Integer.parseInt(v.trim());
            if (n >= 0) return n;
        } catch (NumberFormatException e) {
            // reported below
        }
        LOG.warn("{}: '{}' is not a whole number of 0 or more; using {}", key, v, fallback);
        return fallback;
    }

    public static boolean modernizeTexturesEnabled() {
        return bool("modernizeTextures", true);
    }

    /** Whether legacy leaf textures that recolour the old vanilla leaves are rebuilt in the modern leaf pattern. */
    public static boolean modernLeavesEnabled() {
        return bool("textures.modernLeaves", true);
    }

    public static boolean oreBackgroundsEnabled() {
        return bool("oreBackgrounds.enabled", true);
    }

    /** Vanilla block texture name (no folder or extension) replacing the given legacy host, or null to keep the old rock. */
    public static String oreBackground(String key, String fallback) {
        String v = string("oreBackgrounds." + key, fallback).toLowerCase(Locale.ROOT);
        if (v.equals("legacy")) return null;
        if (!v.matches("[a-z0-9_/]+")) {
            LOG.warn("oreBackgrounds.{}: '{}' is not a texture name; using {}", key, v, fallback);
            return fallback;
        }
        return v;
    }

    /**
     * {@code events.skip}: legacy event handlers that are not registered, comma separated, as {@code full.ClassName#method} or
     * {@code full.ClassName#*} (for finding which legacy handler draws or does something unwanted).
     */
    public static java.util.Set<String> skippedEventHandlers() {
        java.util.Set<String> out = new java.util.HashSet<>();
        for (String s : string("events.skip", "").split(",")) if (!s.isBlank()) out.add(s.trim());
        return out;
    }

    /** Whether legacy block subtypes are flattened into blocks of their own (see BlockFlattening1710). */
    public static boolean flattenBlockSubtypes() {
        return bool("blocks.flattenSubtypes", true);
    }

    /**
     * {@code structures.locatable}: what legacy world generators build in place is recorded as modern structure starts, so
     * {@code /locate structure <mod>:legacy/<name>} finds it (LegacyGeneratorStructures).
     */
    public static boolean structuresLocatable() {
        return bool("structures.locatable", true);
    }

    /** {@code villages.legacyBuildings}: legacy mods' village buildings join the vanilla village house pools. */
    public static boolean villageLegacyBuildings() {
        return bool("villages.legacyBuildings", true);
    }

    /** {@code compat.rocketnautics.plasmaEveryTicks}: how often Rocketnautics' star plasma texture is rebuilt (1 = every tick, as the mod does). */
    public static int rocketnauticsPlasmaEveryTicks() {
        try {
            return Math.max(1, Integer.parseInt(string("compat.rocketnautics.plasmaEveryTicks", "4").trim()));
        } catch (NumberFormatException e) {
            return 4;
        }
    }

    /** {@code compat.hbm.fastTagStacks}: HBM's TagStack lists a tag's items from the tag, not by scanning the whole item registry. */
    public static boolean hbmFastTagStacks() {
        return bool("compat.hbm.fastTagStacks", true);
    }

    /** {@code compat.auroras.irisPackCheck}: Auroras asks Iris whether a shader pack is in use instead of whether shaders are switched on. */
    public static boolean aurorasIrisPackCheck() {
        return bool("compat.auroras.irisPackCheck", true);
    }

    /**
     * {@code render.irisCompat}: legacy rendering adapts to an Iris shader pack in use (sky, particle and glow geometry through
     * the programs Iris has for them, overlays and glows left out of the shadow map, no particles spawned by the shadow pass).
     */
    public static boolean renderIrisCompat() {
        return bool("render.irisCompat", true);
    }

    public static boolean deepLayerEnabled() {
        return bool("deepLayer.enabled", true);
    }

    public static boolean deepLayerAllows(String generatorClass) {
        for (String prefix : excluded()) if (generatorClass.startsWith(prefix)) return false;
        return true;
    }

    private static List<String> excluded;

    private static synchronized List<String> excluded() {
        if (excluded == null) {
            List<String> out = new ArrayList<>();
            for (String s : string("deepLayer.excludeGenerators", "").split(",")) if (!s.isBlank()) out.add(s.trim());
            excluded = out;
        }
        return excluded;
    }

    // ------------------------------------------------------------------ perf.* (memory and speed switches)

    /** {@code perf.releaseUnloadedLevels}: legacy views of unloaded levels and stopped servers are released (see LevelReleases). */
    public static boolean perfReleaseUnloadedLevels() {
        return bool("perf.releaseUnloadedLevels", true);
    }

    /** {@code perf.sweepRemovedPlayers}: views of players removed for good (died and respawned, logged out) are swept. */
    public static boolean perfSweepRemovedPlayers() {
        return bool("perf.sweepRemovedPlayers", true);
    }

    /** {@code perf.sweepDeadViews}: views of closed connections and of removed or unloaded inventories are swept. */
    public static boolean perfSweepDeadViews() {
        return bool("perf.sweepDeadViews", true);
    }

    /** {@code perf.weakStackViews1122} (off): 1.12.2 item stack views without capabilities are held weakly (see Stacks1122). */
    public static boolean perfWeakStackViews1122() {
        return bool("perf.weakStackViews1122", false);
    }

    /** Legacy particles of the built-in particle classes are not drawn outside the view frustum. */
    public static boolean perfCullLegacyParticles() {
        return perf("cullLegacyParticles", true);
    }

    /** Mod particles that draw themselves are culled too (a beam from outside the view may disappear: off by default). */
    public static boolean perfCullModParticles() {
        return perf("cullModParticles", false);
    }

    /** 1.7.10's {@code GameSettings.particleSetting} follows the vanilla particle option. */
    public static boolean perfParticleSetting() {
        return perf("particleSetting", true);
    }

    /** Identical generated block models of a legacy block's metadata values are written once and shared. */
    public static boolean perfShareBlockModels() {
        return perf("shareBlockModels", true);
    }

    /** Legacy items with identical generated item models share one baked model. */
    public static boolean perfShareItemModels() {
        return perf("shareItemModels", true);
    }

    /** A legacy mod texture location keeps the modern location it binds. */
    public static boolean perfCacheTextureLocations() {
        return perf("cacheTextureLocations", true);
    }

    /** Generated pack files move to a disk file once read instead of staying in memory for the session. */
    public static boolean perfPackSpill() {
        return perf("packSpill", true);
    }

    // ------------------------------------------------------------------ perf.* (world load and other performance switches)

    /**
     * {@code perf.lazyPackMetadata}: the generated packs opened only for their metadata (once per pack repository scan) generate no
     * content (they built every file each time: about 0.2 to 0.7 s per scan for the data pack in ATM10).
     */
    public static boolean perfLazyPackMetadata() {
        return bool("perf.lazyPackMetadata", true);
    }

    /**
     * {@code perf.biomeJarCache}: the vanilla biome templates and the vanilla/NeoForge biome tags legacy biomes are generated from are
     * read from the jars once per session (every data pack build read them again: about 0.2 to 0.4 s per build in ATM10).
     */
    public static boolean perfBiomeJarCache() {
        return bool("perf.biomeJarCache", true);
    }

    /**
     * {@code perf.asyncBiomeSample}: the "Biome sample around spawn" diagnostic (1089 biome source lookups after the server started)
     * runs on a background worker instead of the server thread that the joining player waits for.
     */
    public static boolean perfAsyncBiomeSample() {
        return bool("perf.asyncBiomeSample", true);
    }
}
