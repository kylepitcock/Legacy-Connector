package org.kylepitcock.legacyconnector.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minecraft 1.2.5 mods came as a client build and a server build (the game was obfuscated separately per side, ModLoaderMP carried the rest) that both contain the
 * same {@code mod_X} class: two mods with one id stop the game. Of each such group the build for the running side is kept (the first jar when none or several are),
 * as {@link BetaJars#selectBuilds} does for Beta 1.7.3, independent of it.
 *
 * <p>Only a jar all of whose mod ids have a better build elsewhere is left out. A jar that repackages a few mods of another one (LogisticsPipes' client jar carries
 * BuildCraft's {@code mod_BuildCraft*} next to its own {@code mod_LogisticsPipes}) loads without the ids it shares: the preferred build registers those.
 */
public final class Mc125Builds {
    private Mc125Builds() {}

    /** Jars to leave out of the set, with the reason. {@code runningSide} is "CLIENT" or "SERVER". */
    public static Map<LegacyJar, String> select(List<LegacyJar> jars, String runningSide) {
        char running = "SERVER".equals(runningSide) ? 's' : 'c';
        Map<String, List<LegacyJar>> byId = new LinkedHashMap<>();
        for (LegacyJar jar : jars) {
            if (jar.sourceVersion != LegacyVersion.V1_2_5) continue;
            for (LegacyModDecl mod : jar.mods) byId.computeIfAbsent(mod.legacyId, k -> new ArrayList<>()).add(jar);
        }
        Map<String, LegacyJar> preferred = new LinkedHashMap<>();
        for (var e : byId.entrySet()) {
            List<LegacyJar> group = e.getValue();
            if (group.size() < 2) continue;
            preferred.put(e.getKey(), group.stream().filter(j -> j.notchSide == running).findFirst().orElse(group.get(0)));
        }
        Map<LegacyJar, String> dropped = new LinkedHashMap<>();
        for (LegacyJar jar : jars) {
            if (jar.sourceVersion != LegacyVersion.V1_2_5 || jar.mods.isEmpty()) continue;
            LegacyJar other = null;
            boolean covered = true, any = false;
            for (LegacyModDecl mod : jar.mods) {
                LegacyJar keep = preferred.get(mod.legacyId);
                if (keep == null) {
                    covered = false;
                } else if (keep != jar) {
                    any = true;
                    other = keep;
                } else {
                    covered = false;
                }
            }
            if (any && covered) {
                dropped.put(jar, "another build of " + String.join(", ", jar.mods.stream().map(m -> m.legacyId).toList()) + " (" + other.fileName() + ", "
                        + (other.notchSide == 's' ? "server" : "client") + " names) is loaded on this " + (running == 's' ? "server" : "client"));
            } else if (any) {
                // keeps its own mods, gives up the ids another jar of the set carries (the preferred build registers them)
                List<String> gave = jar.mods.stream().filter(m -> preferred.get(m.legacyId) != null && preferred.get(m.legacyId) != jar).map(m -> m.legacyId + " (" + preferred.get(m.legacyId).fileName() + ")").toList();
                jar.mods.removeIf(m -> preferred.get(m.legacyId) != null && preferred.get(m.legacyId) != jar);
                jar.notes.add("Mod class(es) shared with other jars of the set are loaded from those: " + String.join(", ", gave));
            }
        }
        return dropped;
    }
}
