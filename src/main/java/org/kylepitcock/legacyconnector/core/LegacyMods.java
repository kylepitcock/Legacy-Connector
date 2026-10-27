package org.kylepitcock.legacyconnector.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Registry of every legacy mod loaded through the compatibility layer, in dependency order. */
public final class LegacyMods {
    private static final List<LegacyModRecord> MODS = new ArrayList<>();
    private static final Map<String, LegacyModRecord> BY_LEGACY_ID = new HashMap<>();
    private static final Map<String, LegacyPlatform> PLATFORMS = new LinkedHashMap<>();

    private LegacyMods() {}

    public static synchronized void registerPlatform(LegacyPlatform platform) {
        PLATFORMS.put(platform.version(), platform);
    }

    public static synchronized LegacyPlatform platform(String version) {
        LegacyPlatform p = PLATFORMS.get(version);
        if (p == null) throw new IllegalStateException("No legacy platform for Minecraft " + version);
        return p;
    }

    public static synchronized Iterable<LegacyPlatform> platforms() {
        return List.copyOf(PLATFORMS.values());
    }

    static synchronized void add(LegacyModRecord mod) {
        MODS.add(mod);
        BY_LEGACY_ID.put(mod.legacyId.toLowerCase(Locale.ROOT), mod);
    }

    /**
     * All mods, sorted so that {@code after:X} / {@code required-after:X} dependencies come first. Otherwise mods in
     * coremod jars come first, as FML found them on the class path before it read the mods folder (ForgeMultipart
     * registers its tile traits before AE2's integration adds pass-through interfaces), then by id.
     */
    public static synchronized List<LegacyModRecord> ordered() {
        Map<String, LegacyModRecord> byId = new LinkedHashMap<>();
        MODS.stream().sorted(java.util.Comparator.comparing((LegacyModRecord m) -> !m.inCoreModJar())
                        .thenComparing(m -> m.legacyId, String.CASE_INSENSITIVE_ORDER))
                .forEach(m -> byId.put(m.legacyId.toLowerCase(Locale.ROOT), m));
        List<LegacyModRecord> out = new ArrayList<>();
        Map<LegacyModRecord, Integer> state = new HashMap<>();
        for (LegacyModRecord m : byId.values()) visit(m, byId, state, out);
        return out;
    }

    private static void visit(LegacyModRecord m, Map<String, LegacyModRecord> byId, Map<LegacyModRecord, Integer> state, List<LegacyModRecord> out) {
        Integer s = state.get(m);
        if (s != null) return; // visited or on stack (cycle: legacy FML also just ignored those)
        state.put(m, 1);
        for (String part : m.dependencies().split(";")) {
            int colon = part.indexOf(':');
            if (colon < 0) continue;
            String kind = part.substring(0, colon).trim();
            if (!kind.endsWith("after")) continue;
            String target = part.substring(colon + 1).trim();
            int at = target.indexOf('@');
            if (at >= 0) target = target.substring(0, at);
            LegacyModRecord dep = byId.get(target.toLowerCase(Locale.ROOT));
            if (dep != null) visit(dep, byId, state, out);
        }
        state.put(m, 2);
        out.add(m);
    }

    public static synchronized List<LegacyModRecord> all() {
        return Collections.unmodifiableList(new ArrayList<>(MODS));
    }

    public static synchronized LegacyModRecord byLegacyId(String id) {
        return id == null ? null : BY_LEGACY_ID.get(id.toLowerCase(Locale.ROOT));
    }

    /** Resolves either a legacy id ({@code BuildCraft|Core}) or a modern id ({@code buildcraft_core}). */
    public static synchronized LegacyModRecord find(String id) {
        LegacyModRecord r = byLegacyId(id);
        if (r != null) return r;
        for (LegacyModRecord m : MODS) if (m.modernId.equals(id)) return m;
        return null;
    }

    /**
     * Whether {@code id} (legacy or modern id) names only legacy mods of versions other than {@code version}. A mod's
     * integration with another version of a mod cannot work (1.12.2 Botania's Thaumcraft 6 support crashed on 1.7.10's
     * Thaumcraft 4), so for its isModLoaded such a mod is absent.
     */
    public static synchronized boolean onlyOtherVersions(String id, String version) {
        boolean other = false;
        for (LegacyModRecord m : MODS) {
            if (!m.legacyId.equalsIgnoreCase(id) && !m.modernId.equalsIgnoreCase(id)) continue;
            if (m.version.equals(version)) return false;
            other = true;
        }
        return other;
    }

    public static synchronized boolean isEmpty() {
        return MODS.isEmpty();
    }
}
