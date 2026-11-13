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

    /**
     * The platform family a legacy version runs on: 1.4.7, 1.5.2 and 1.6.4 mods are linked against the 1.7.10 shim and run on its platform
     * (see the loader's {@code LegacyVersion.forward}); every other version is its own family.
     */
    public static String family(String version) {
        return "1.6.4".equals(version) || "1.5.2".equals(version) || "1.4.7".equals(version) ? "1.7.10" : version;
    }

    public static synchronized LegacyPlatform platform(String version) {
        LegacyPlatform p = PLATFORMS.get(family(version));
        if (p == null) throw new IllegalStateException("No legacy platform for Minecraft " + version);
        return p;
    }

    public static synchronized Iterable<LegacyPlatform> platforms() {
        return List.copyOf(PLATFORMS.values());
    }

    static synchronized void add(LegacyModRecord mod) {
        if (mod.flag("clientSideOnly") && net.neoforged.fml.loading.FMLEnvironment.dist.isDedicatedServer()) {
            org.apache.logging.log4j.LogManager.getLogger("LegacyConnector").info("Skipping the client-only legacy mod {} on a dedicated server", mod.legacyId);
            return;
        }
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
                .forEach(m -> byId.put(key(m.version, m.legacyId), m));
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
            target = LegacyConfig.modAlias(target);
            LegacyModRecord dep = byId.get(key(m.version, target));
            if (dep == null) dep = compatible(byId.values(), target, m.version);
            if (dep != null) visit(dep, byId, state, out);
        }
        state.put(m, 2);
        out.add(m);
    }

    private static String key(String version, String id) {
        return version + ":" + id.toLowerCase(Locale.ROOT);
    }

    /** Match within the caller's runtime family, preferring the exact Minecraft version. */
    private static LegacyModRecord compatible(Iterable<LegacyModRecord> mods, String id, String version) {
        if (id == null) return null;
        LegacyModRecord fallback = null;
        for (LegacyModRecord mod : mods) {
            if (!mod.legacyId.equalsIgnoreCase(id) && !mod.modernId.equalsIgnoreCase(id)) continue;
            if (mod.version.equals(version)) return mod;
            if (family(mod.version).equals(family(version))) fallback = mod;
        }
        return fallback;
    }

    public static synchronized LegacyModRecord byLegacyId(String id, String version) {
        return compatible(MODS, id, version);
    }

    public static synchronized List<LegacyModRecord> all() {
        return Collections.unmodifiableList(new ArrayList<>(MODS));
    }

    public static synchronized LegacyModRecord byLegacyId(String id) {
        return id == null ? null : BY_LEGACY_ID.get(id.toLowerCase(Locale.ROOT));
    }

    /** Resolves either a legacy id ({@code BuildCraft|Core}) or a modern id ({@code buildcraft_core}). */
    public static synchronized LegacyModRecord find(String id) {
        if (id == null) return null;
        // The isolated FML id identifies one copy even when registry namespaces remain shared.
        for (LegacyModRecord m : MODS) if (m.container != null && m.container.getModId().equals(id)) return m;
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
            if (family(m.version).equals(family(version))) return false;
            other = true;
        }
        return other;
    }

    /** True when at least one loaded legacy mod runs on the platform family of {@code version} (see {@link #family}). */
    public static synchronized boolean hasFamily(String version) {
        String wanted = family(version);
        for (LegacyModRecord mod : MODS) if (family(mod.version).equals(wanted)) return true;
        return false;
    }

    public static synchronized boolean isEmpty() {
        return MODS.isEmpty();
    }
}
