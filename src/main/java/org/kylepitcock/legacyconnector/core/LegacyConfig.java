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

            # Legacy worlds ended at y=0. When enabled, legacy world generators (ores, caves, ...) run a second time shifted
            # down so that they also fill the modern world below y=0 (legacy y=0 is modern y=-64).
            deepLayer.enabled = true
            # Comma separated class name prefixes of legacy world generators that must not run below y=0.
            deepLayer.excludeGenerators =
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
}
