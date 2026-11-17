package org.kylepitcock.legacyconnector.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * File names of legacy saved data ({@code WorldSavedData}, 1.7.10 and 1.12.2). Mods find their data by listing the world's data
 * folder (Mystcraft lists {@code agedata_<id>.dat} at server start to register its ages), so the file keeps the mod's own name,
 * {@code data/<name>.dat} as before 1.12. Names the modern game keeps for its own data and names with characters a file cannot
 * hold keep the {@code legacy_} prefix. Earlier builds prefixed every file; those are renamed once per data folder.
 */
public final class SavedDataFiles {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/SavedData");
    private static final Set<Path> MIGRATED = ConcurrentHashMap.newKeySet();

    private SavedDataFiles() {}

    /** The name (without {@code .dat}) the modern storage uses for the legacy saved data {@code name}. */
    public static String file(String name) {
        String clean = name.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return reserved(clean) || !clean.equals(name) ? "legacy_" + clean : clean;
    }

    static boolean reserved(String name) {
        return name.isEmpty() || name.startsWith("legacy_") || name.equals("idcounts") || name.equals("scoreboard") || name.equals("chunks")
                || name.startsWith("raids") || name.equals("random_sequences") || name.startsWith("command_storage_") || name.matches("map_-?\\d+")
                || name.startsWith("minecraft") || name.startsWith("neoforge");
    }

    /**
     * Renames {@code legacy_<name>.dat} to {@code <name>.dat} in the data folder of {@code storage} (once per folder), for every
     * name {@link #file} would not prefix. A file that exists under the new name wins and the old one is left alone; a file that
     * cannot be renamed is skipped (logged once), and never fails the world load.
     */
    public static void migrate(DimensionDataStorage storage) {
        if (storage == null) return;
        try {
            Path folder = null;
            for (var f : DimensionDataStorage.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType() == java.io.File.class) {
                    f.setAccessible(true);
                    folder = ((java.io.File) f.get(storage)).toPath();
                } else if (f.getType() == Path.class) {
                    f.setAccessible(true);
                    folder = (Path) f.get(storage);
                }
                if (folder != null) break;
            }
            if (folder == null || !Files.isDirectory(folder) || !MIGRATED.add(folder.toAbsolutePath().normalize())) return;
            java.util.List<Path> candidates;
            try (var files = Files.list(folder)) {
                candidates = files.toList();
            }
            for (Path p : candidates) {
                String n = p.getFileName().toString();
                if (!n.startsWith("legacy_") || !n.endsWith(".dat")) continue;
                String name = n.substring("legacy_".length(), n.length() - 4);
                if (!file(name).equals(name)) continue; // would be prefixed anyway
                Path target = folder.resolve(name + ".dat");
                try {
                    if (Files.exists(target)) {
                        LOG.warn("Both {} and {}.dat exist in {}; the newer name is used, the old file is left as it is", n, name, folder);
                    } else {
                        Files.move(p, target);
                        LOG.info("Renamed legacy saved data {} to {}.dat", n, name);
                    }
                } catch (Exception e) {
                    LOG.warn("Could not rename legacy saved data {} to {}.dat ({}); the mod will not find it", n, name, e.toString());
                }
            }
        } catch (Exception | LinkageError e) {
            LOG.warn("Could not look for legacy saved data files to rename ({})", e.toString());
        }
    }
}
