package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Decides which legacy libraries run as isolated units next to another copy ({@link SideBySide}).
 *
 * <p>Decision order for a legacy library whose mod id a native NeoForge 1.21.1 jar also uses: native bridge (the library is in
 * {@code libraries.tsv} without the {@code sideBySide} marker, i.e. the translated mods can link against the native API) &gt; side by side &gt; skipped
 * with a report line. For two legacy versions of one library the copy of the newer version keeps the id and the other becomes a unit.
 */
public final class SideBySidePlanner {
    private SideBySidePlanner() {}

    /** The plan and the report line of every jar that became a unit. */
    public record Result(SideBySide plan, Map<LegacyJar, String> notes) {}

    /**
     * @param modernIds     mod ids of the NeoForge jars of the mods folder
     * @param crossVersion  units for mod ids that two legacy versions share (see {@code LegacyModLocator.dropDuplicateMods}), id isolation only
     */
    public static Result plan(List<LegacyJar> legacy, Set<String> modernIds, Properties config, List<SideBySide.Unit> crossVersion) {
        Map<LegacyJar, String> notes = new IdentityHashMap<>();
        if (!SideBySide.enabled(config)) return new Result(SideBySide.NONE, notes);
        Set<String> include = SideBySide.idList(config, "include");
        Set<String> exclude = SideBySide.idList(config, "exclude");
        List<SideBySide.Unit> units = new ArrayList<>(crossVersion);
        Set<LegacyJar> isolated = Collections.newSetFromMap(new IdentityHashMap<>());
        for (LegacyJar jar : legacy) {
            if (jar.version != LegacyVersion.V1_20_1 || jar.mods.isEmpty()) continue;
            String id = jar.mods.get(0).modernId();
            if (exclude.contains(id) || !modernIds.contains(id)) continue;
            NativeLibraries1201.Library lib = NativeLibraries1201.table().get(id);
            if (lib != null && !lib.preferSideBySide() && !include.contains(id)) continue; // bridged to the native build
            Set<String> roots;
            try {
                roots = SideBySide.rootsOf(SideBySide.packagesOf(jar.existing()));
            } catch (IOException | RuntimeException e) {
                continue;
            }
            if (roots.isEmpty()) continue;
            for (LegacyModDecl m : jar.mods) {
                String newId = SideBySide.newIdFor(m.modernId(), jar.version);
                units.add(new SideBySide.Unit(jar.version, m.modernId(), newId, roots, "native " + id + " (NeoForge 1.21.1) is installed"));
            }
            isolated.add(jar);
        }
        SideBySide plan = SideBySide.of(units);
        Map<String, SideBySide.Unit> byKey = new LinkedHashMap<>();
        for (SideBySide.Unit u : units) byKey.put(u.version().id() + ":" + u.modId(), u);
        for (LegacyJar jar : legacy) {
            for (LegacyModDecl m : jar.mods) {
                SideBySide.Unit u = byKey.get(jar.version.id() + ":" + m.modernId());
                if (u == null || !u.roots().isEmpty() && !isolated.contains(jar) || u.roots().isEmpty() && !crossVersion.contains(u)) continue;
                m.fmlIdOverride = u.newId();
                notes.put(jar, "  side-by-side: " + m.modernId() + " loads as " + u.newId() + " next to its other copy (" + u.reason() + ")"
                        + (u.roots().isEmpty() ? "" : ", packages " + u.roots() + " relocated under " + SideBySide.prefix(u.version())) + "\n");
            }
        }
        NativeLibraries1201.setIsolated(isolated);
        return new Result(plan, notes);
    }

    /** For tests and the locator: lower-case id of a library mod. */
    static String key(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}
