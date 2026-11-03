package org.kylepitcock.legacyconnector.core;

import java.nio.file.Path;
import java.util.List;
import net.neoforged.fml.CrashReportCallables;

/**
 * Puts the layer into crash reports: a header that says translated legacy mods are running (so a crash in one is reported to
 * Legacy Connector first, not to the legacy mod's author, who never saw 1.21.1), and a "System Details" section listing them
 * (id, version, Minecraft version it was built for, jar). Large packs have hundreds, so the list is capped.
 */
public final class LegacyCrashInfo {
    private static final int MAX_LISTED = 300;
    private static final String ISSUES = "https://github.com/kylepitcock/Legacy-Connector/issues";

    private LegacyCrashInfo() {}

    public static void register() {
        CrashReportCallables.registerHeader(LegacyCrashInfo::header);
        CrashReportCallables.registerCrashCallable("Legacy Connector legacy mods", LegacyCrashInfo::describe, () -> !LegacyMods.isEmpty());
    }

    static String header() {
        List<LegacyModRecord> mods = LegacyMods.all();
        if (mods.isEmpty()) return "Legacy Connector is installed (no legacy mods loaded).";
        return "Legacy Connector is running " + mods.size() + " translated legacy mod(s). If this crash involves one of them, report it at "
                + ISSUES + " (not to the mod's author: it was never built for this Minecraft version). Mod list below, under 'Legacy Connector legacy mods'.";
    }

    static String describe() {
        List<LegacyModRecord> mods = LegacyMods.ordered();
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (LegacyModRecord m : mods) {
            if (shown++ >= MAX_LISTED) {
                sb.append("\n\t... and ").append(mods.size() - MAX_LISTED).append(" more");
                break;
            }
            sb.append("\n\t").append(m.legacyId).append(' ').append(m.modVersion()).append(" (built for ").append(m.version).append(')');
            String jar = jarName(m);
            if (jar != null) sb.append(" from ").append(jar);
            if (!m.legacyId.equalsIgnoreCase(m.modernId)) sb.append(" as ").append(m.modernId);
        }
        return sb.toString();
    }

    private static String jarName(LegacyModRecord m) {
        try {
            if (m.metadata != null && m.metadata.has("sourcePath")) return Path.of(m.metadata.get("sourcePath").getAsString()).getFileName().toString();
        } catch (RuntimeException ignored) {
            // a crash report must never fail because of its own extras
        }
        return null;
    }
}
