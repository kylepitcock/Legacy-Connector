package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Libraries of Forge 1.20.1 mods that also exist as native NeoForge 1.21.1 mods under the same mod id (Architectury, Curios, GeckoLib, Patchouli,
 * Cloth Config, ...; table {@code forward/1.20.1/libraries.tsv}). With the 1.21.1 build in the mods folder the mod's 1.20.1 copy of the library is not
 * translated or loaded (two mods with one id cannot load; the translated mods link against the installed build, with the API changes handled by the
 * {@code lib-*.tsv} tables). Without it the 1.20.1 jar is translated as a fallback when the pack has it; when the pack has neither, the log says which
 * build to install.
 */
public final class NativeLibraries1201 {
    /** A library with a native 1.21.1 build. */
    public record Library(String modId, String name, String url, boolean preferSideBySide) {}

    private static volatile Map<String, Library> table;
    /** Library jars the planner loads next to the native build instead of skipping them. */
    private static volatile Set<LegacyJar> isolated = Set.of();

    static void setIsolated(Set<LegacyJar> jars) {
        isolated = jars;
    }

    private NativeLibraries1201() {}

    public static Map<String, Library> table() {
        Map<String, Library> t = table;
        if (t == null) {
            Map<String, Library> m = new LinkedHashMap<>();
            try (InputStream in = NativeLibraries1201.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/libraries.tsv")) {
                if (in != null) {
                    for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                        if (l.isBlank() || l.startsWith("#")) continue;
                        String[] a = l.strip().split("\t");
                        if (a.length >= 3) m.put(a[0], new Library(a[0], a[1], a[2], a.length >= 4 && a[3].equalsIgnoreCase("sideBySide")));
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Corrupt libraries.tsv", e);
            }
            table = t = Map.copyOf(m);
        }
        return t;
    }

    /**
     * The bridge a 1.20.1 library jar is skipped for: all of its mods are libraries of the table whose 1.21.1 build the pack has installed;
     * null for any other jar. Called with the modern mod ids of the mods folder.
     */
    public static LibraryBridges.Bridge replacing(LegacyJar jar, Set<String> modernIds) {
        if (isolated.contains(jar)) return null; // loaded side by side with the native build (SideBySidePlanner)
        if (jar.version != LegacyVersion.V1_20_1 || jar.mods.isEmpty()) return null;
        for (LegacyModDecl m : jar.mods) {
            String id = m.legacyId.toLowerCase(Locale.ROOT);
            if (!table().containsKey(id) || !modernIds.contains(id)) return null;
        }
        String id = jar.mods.get(0).legacyId;
        return new LibraryBridges.Bridge(id, "", id.toLowerCase(Locale.ROOT), "native1201");
    }

    private static final Pattern MOD_ID = Pattern.compile("modId\\s*=\\s*\"([^\"]+)\"");

    /**
     * Messages for 1.20.1 mods that require a library of the table which the pack has neither as a 1.21.1 build nor as a 1.20.1 jar: FML would only say
     * "missing mod architectury".
     */
    public static List<String> missing(List<LegacyJar> jars, Set<String> modernIds) {
        Set<String> legacyIds = new java.util.HashSet<>();
        for (LegacyJar j : jars) if (j.version == LegacyVersion.V1_20_1) for (LegacyModDecl m : j.mods) legacyIds.add(m.legacyId.toLowerCase(Locale.ROOT));
        List<String> out = new ArrayList<>();
        Set<String> reported = new java.util.HashSet<>();
        for (LegacyJar j : jars) {
            if (j.version != LegacyVersion.V1_20_1 || j.mods.isEmpty()) continue;
            for (String dep : requiredDependencies(j)) {
                Library lib = table().get(dep);
                if (lib == null || modernIds.contains(dep) || legacyIds.contains(dep) || !reported.add(j.fileName() + ":" + dep)) continue;
                out.add(j.fileName() + " needs " + lib.name() + ", which is not in the mods folder: install the NeoForge 1.21.1 build (" + lib.url() + ")");
            }
        }
        return out;
    }

    /** The mod ids a 1.20.1 jar's mods.toml marks as mandatory dependencies. */
    static List<String> requiredDependencies(LegacyJar jar) {
        List<String> out = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jar.existing().toFile())) {
            ZipEntry e = zip.getEntry("META-INF/mods.toml");
            if (e == null) return out;
            String toml = new String(zip.getInputStream(e).readAllBytes(), StandardCharsets.UTF_8);
            for (String block : toml.split("(?m)^\\s*\\[\\[dependencies\\.")) {
                int end = block.indexOf("[[");
                if (end >= 0) block = block.substring(0, end);
                Matcher m = MOD_ID.matcher(block);
                if (!m.find()) continue;
                String id = m.group(1).toLowerCase(Locale.ROOT);
                boolean mandatory = block.matches("(?s).*mandatory\\s*=\\s*true.*");
                if (mandatory && !id.equals("forge") && !id.equals("minecraft") && !id.equals("neoforge")) out.add(id);
            }
        } catch (IOException | RuntimeException ignored) {
            // unreadable: the translator reports it
        }
        return out;
    }
}
