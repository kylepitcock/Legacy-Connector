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

            # Legacy worlds ended at y=0. When enabled, legacy world generators (ores, caves, ...) run a second time shifted
            # down so that they also fill the modern world below y=0 (legacy y=0 is modern y=-64).
            deepLayer.enabled = true
            # Comma separated class name prefixes of legacy world generators that must not run below y=0.
            deepLayer.excludeGenerators =

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

            # Energy conversion for legacy tiles and items exposed as NeoForge energy (FE). RF is 1:1 with FE.
            # FE per IC2 EU (IC2 machines accept energy from modern cables at this rate).
            energy.fePerEu = 4
            # FE per BuildCraft MJ (BuildCraft's own rule: 1 MJ = 10 RF).
            energy.fePerMj = 10

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

    /** FE per IC2 EU for the energy capability of IC2 tiles and items. */
    public static int fePerEu() {
        return positiveInt("energy.fePerEu", 4);
    }

    /** FE per BuildCraft MJ for the energy capability of BuildCraft power receptors. */
    public static int fePerMj() {
        return positiveInt("energy.fePerMj", 10);
    }

    public static boolean modernizeTexturesEnabled() {
        return bool("modernizeTextures", true);
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
}
