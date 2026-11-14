package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Legacy libraries that were never updated, replaced by their modern equivalent instead of being loaded: when the
 * modern mod is installed, the legacy library jar is skipped and every reference to its API package is relocated
 * into the shim, where the API is re-implemented on top of the modern mod (like Connector Extras bridges Trinkets
 * to Curios). Without the modern mod, the legacy library is translated and loaded as usual.
 */
public final class LibraryBridges {
    /**
     * @param legacyModId  the legacy library mod this bridge replaces, or null when the API is only shaded into other mods
     * @param apiRoot      internal-name package root of the legacy API, re-implemented under the shim prefix
     * @param modernModId  modern mod the bridge needs, or null when it targets NeoForge itself (always active)
     * @param configKey    {@code bridges.<configKey>.enabled} in config/legacyconnector.properties switches the bridge off
     */
    public record Bridge(String legacyModId, String apiRoot, String modernModId, String configKey) {}

    public static final List<Bridge> KNOWN = List.of(
            new Bridge("Baubles", "baubles/api/", "curios", "baubles"),
            // Mods also call Baubles' PlayerHandler / InventoryBaubles directly (Botania's Flight Tiara and Travel Belt).
            new Bridge("Baubles", "baubles/common/", "curios", "baubles"),
            // Waila tooltip providers are shown through Jade.
            new Bridge("Waila", "mcp/mobius/waila/", "jade", "waila"),
            // NEI recipe handlers and hidden items are shown through JEI.
            new Bridge("NotEnoughItems", "codechicken/nei/", "jei", "nei"),
            // Patchouli's API (1.12.2: books, multiblocks, custom components) runs on the modern Patchouli; books legacy mods ship as
            // resources are converted into its data/<mod>/patchouli_books format by the runtime.
            new Bridge("patchouli", "vazkii/patchouli/api/", "patchouli", "patchouli"),
            // NEI always came with CodeChickenLib, so NEI integrations use it too (DragonAPI's tooltip handlers: GuiDraw, Rectangle4i).
            // Without a legacy CodeChickenLib jar its API is synthesized with the bridged NEI; a real CodeChickenLib is loaded instead.
            new Bridge(null, "codechicken/lib/", "jei", "nei"),
            // CoFH's RF energy API (shaded by most 1.7.10 tech mods) is backed by NeoForge's Energy capability.
            new Bridge(null, "cofh/api/energy/", null, "cofhEnergy"),
            // The same for 1.12.2's Redstone Flux library (cofh.redstoneflux.api / .impl, a separate mod "redstoneflux" that Thermal Expansion,
            // Thermal Dynamics, Mekanism, ... need): re-implemented in the 1.12.2 shim, so the library jar itself is not loaded.
            new Bridge("redstoneflux", "cofh/redstoneflux/", null, "cofhEnergy"));

    /**
     * Mods that only make sense on the 1.7.10 engine or launcher (performance/bug-fix patches, ID extenders, mixin
     * loaders, profilers). They are not translated; the report says why. Client-side conveniences (Inventory Tweaks,
     * JourneyMap, Custom Main Menu, ...) are loaded like any other legacy mod.
     * Keys are lower-case legacy mod ids.
     */
    private static final java.util.Map<String, String> OBSOLETE = java.util.Map.ofEntries(
            java.util.Map.entry("fastcraft", "1.7.10 performance patches; not applicable to 1.21.1"),
            java.util.Map.entry("archaicfix", "1.7.10 bug-fix/performance patches; not applicable to 1.21.1"),
            java.util.Map.entry("hodgepodge", "1.7.10 bug-fix patches; not applicable to 1.21.1"),
            java.util.Map.entry("bugtorch", "1.7.10 bug-fix patches; not applicable to 1.21.1"),
            java.util.Map.entry("coretweaks", "1.7.10 bug-fix/performance patches; not applicable to 1.21.1"),
            java.util.Map.entry("lwjgl3ify", "runs 1.7.10 on LWJGL 3 / modern Java; 1.21.1 already does"),
            java.util.Map.entry("angelica", "1.7.10 renderer replacement; use a modern renderer mod (e.g. Sodium/Embeddium)"),
            java.util.Map.entry("mcpatcherforge", "1.7.10 MCPatcher port patching the old renderer (bundled in Angelica); the layer converts connected textures itself"),
            java.util.Map.entry("beddium", "1.7.10 renderer replacement (Sodium port); use a modern renderer mod (e.g. Sodium/Embeddium)"),
            java.util.Map.entry("falsetweaks", "1.7.10 renderer performance patches; not applicable to 1.21.1"),
            java.util.Map.entry("infiview", "1.7.10 render distance patch; 1.21.1 has no such limit"),
            java.util.Map.entry("angellib", "library of the 1.7.10 renderer replacement Angelica; not applicable to 1.21.1"),
            java.util.Map.entry("euphoriacompanion", "companion of the 1.7.10 shader renderer Angelica; use a modern shader mod (Iris)"),
            java.util.Map.entry("crash_assistant", "1.7.10 JVM crash window (AWT); not applicable to 1.21.1"),
            java.util.Map.entry("notfine","1.7.10 graphics options; use a modern renderer/options mod"),
            java.util.Map.entry("neid", "1.7.10 ID limit extender; 1.21.1 has no ID limits"),
            java.util.Map.entry("fastleafdecay", "use a modern fast leaf decay mod"),
            java.util.Map.entry("betterloadingscreen", "1.7.10 loading screen; NeoForge has its own"),
            java.util.Map.entry("bettercrashes", "1.7.10 crash handling; not applicable"),
            java.util.Map.entry("letsnecryptcraft", "Java 8 certificate fix; modern Java includes it"),
            java.util.Map.entry("helpfixer", "1.7.10 /help fix; not needed on 1.21.1"),
            java.util.Map.entry("netherportalfix", "1.7.10 portal fix; use a modern equivalent if wanted"),
            java.util.Map.entry("resourceloader", "1.7.10 resource pack loader; 1.21.1 loads resource packs natively"),
            java.util.Map.entry("oauth", "1.7.10 Microsoft login helper; not needed"),
            java.util.Map.entry("betterachievements", "1.21 replaced achievements with advancements"),
            java.util.Map.entry("unimixins", "1.7.10 mixin loader; NeoForge 1.21.1 ships its own Mixin"),
            java.util.Map.entry("gtnhmixins", "1.7.10 mixin loader; NeoForge 1.21.1 ships its own Mixin"),
            java.util.Map.entry("spongemixins", "1.7.10 mixin loader; NeoForge 1.21.1 ships its own Mixin"),
            java.util.Map.entry("mixinbooterlegacy", "1.7.10 mixin loader; NeoForge 1.21.1 ships its own Mixin"),
            java.util.Map.entry("unimixins-mixin", "1.7.10 mixin loader component; NeoForge 1.21.1 ships its own Mixin"),
            java.util.Map.entry("unimixins-compat", "1.7.10 mixin loader component; NeoForge 1.21.1 ships its own Mixin"),
            java.util.Map.entry("mixingasm", "1.7.10 mixin loader component; NeoForge 1.21.1 ships its own Mixin"),
            java.util.Map.entry("gasstation", "1.7.10 mixin loader component; NeoForge 1.21.1 ships its own Mixin"),
            java.util.Map.entry("mixinextras", "1.7.10 mixin loader component; NeoForge 1.21.1 ships its own Mixin"),
            java.util.Map.entry("opis", "1.7.10 profiler; use Spark"),
            // 1.12.2
            java.util.Map.entry("betterfps", "1.12.2 performance patches; not applicable to 1.21.1"),
            java.util.Map.entry("foamfix", "1.12.2 performance/memory patches; not applicable to 1.21.1"),
            java.util.Map.entry("phosphor-lighting", "1.12.2 lighting engine rewrite; 1.21.1 has its own"),
            java.util.Map.entry("vanillafix", "1.12.2 bug-fix patches; not applicable to 1.21.1"),
            java.util.Map.entry("randompatches", "1.12.2 bug-fix patches; not applicable to 1.21.1"),
            java.util.Map.entry("aireducer", "1.12.2 AI performance patch; not applicable to 1.21.1"),
            java.util.Map.entry("mixinbooter", "1.12.2 mixin loader; NeoForge 1.21.1 ships its own Mixin"),
            java.util.Map.entry("loliasm", "1.12.2 performance patches; not applicable to 1.21.1"),
            java.util.Map.entry("ifpatcher", "1.12.2 class patcher; not applicable to 1.21.1"),
            java.util.Map.entry("oldjavawarning", "warns about old Java versions; not applicable"),
            java.util.Map.entry("serializationisbad", "1.12.2 deserialization security patch; not applicable to 1.21.1"),
            java.util.Map.entry("tidychunk", "1.12.2 chunk unloading tweak; not applicable to 1.21.1"),
            java.util.Map.entry("reauth", "1.12.2 login helper; not needed"),
            java.util.Map.entry("laggoggles", "1.12.2 profiler; use Spark"));

    /** Legacy mod ids (lower case) the pack maker switched off with {@code mods.skip} in config/legacyconnector.properties. */
    private static volatile Set<String> userSkips = Set.of();
    /** Modern mod ids of the mods folder, kept for the 1.20.1 library jars that a native 1.21.1 build replaces ({@link NativeLibraries1201}); empty when 1.20.1 support is off. */
    private static volatile Set<String> installedModern = Set.of();

    /**
     * Forge 1.20.1 support is experimental but on by default (it does nothing unless a Forge 1.20.1 jar is installed):
     * {@code experimental.forge1201=false} (config) or {@code -Dlegacyconnector.forge1201=false} turns it off.
     */
    static boolean forge1201On(Path modsDir) {
        String prop = System.getProperty("legacyconnector.forge1201");
        if (prop != null && !prop.isBlank()) return Boolean.parseBoolean(prop.trim());
        return !"false".equalsIgnoreCase(readConfig(modsDir).getProperty("experimental.forge1201", "true").trim());
    }

    /** {@code experimental.forge1201.softlink} (default on): references to 1.21.1 symbols that do not exist become stand-ins that throw when executed. */
    static boolean forge1201SoftLinkOn(Path modsDir) {
        String prop = System.getProperty("legacyconnector.forge1201.softlink");
        if (prop != null && !prop.isBlank()) return Boolean.parseBoolean(prop.trim());
        return !"false".equalsIgnoreCase(readConfig(modsDir).getProperty("experimental.forge1201.softlink", "true").trim());
    }

    private static volatile Set<String> lastModern = Set.of();

    /** Modern mod ids of the mods folder seen by the last {@link #active} call, whatever the 1.20.1 setting. */
    public static Set<String> seenModernIds() {
        return lastModern;
    }

    /** Modern mod ids of the mods folder seen by the last {@link #active} call. */
    public static Set<String> installedModernIds() {
        return installedModern;
    }
    /** {@code mods.alias.<id> = <other id>}: a dependency on the first id is treated as one on the second (lower case keys). */
    private static volatile java.util.Map<String, String> userAliases = java.util.Map.of();

    /** Reads {@code mods.skip = id, id} and {@code mods.alias.<id> = <id>} (called when the config is read at startup). */
    static void applyUserConfig(Properties config) {
        Set<String> skips = new HashSet<>();
        for (String id : config.getProperty("mods.skip", "").split(",")) {
            if (!id.isBlank()) skips.add(id.trim().toLowerCase(Locale.ROOT));
        }
        java.util.Map<String, String> aliases = new java.util.HashMap<>();
        for (String key : config.stringPropertyNames()) {
            if (key.startsWith("mods.alias.") && key.length() > "mods.alias.".length() && !config.getProperty(key).isBlank()) {
                aliases.put(key.substring("mods.alias.".length()).trim().toLowerCase(Locale.ROOT), config.getProperty(key).trim());
            }
        }
        userSkips = Set.copyOf(skips);
        userAliases = java.util.Map.copyOf(aliases);
    }

    /** The id a dependency on {@code legacyId} is satisfied by, after the pack maker's {@code mods.alias.*} (the id itself when none). */
    public static String alias(String legacyId) {
        String other = userAliases.get(legacyId.toLowerCase(Locale.ROOT));
        return other != null ? other : legacyId;
    }

    /** Lower-case ids of the engine-only mods that are never loaded: a dependency on one of them counts as satisfied. */
    public static Set<String> obsoleteIds() {
        return OBSOLETE.keySet();
    }

    /** Why a legacy jar is obsolete on 1.21.1 (all its mods are on the list) or switched off by the pack maker, or null. */
    public static String obsoleteReason(LegacyJar jar) {
        if (!jar.hasMods()) return null;
        Set<String> skips = userSkips;
        if (!skips.isEmpty() && jar.mods.stream().allMatch(m -> skips.contains(m.legacyId.toLowerCase(Locale.ROOT)))) {
            return "switched off with mods.skip in config/legacyconnector.properties";
        }
        String reason = null;
        for (LegacyModDecl m : jar.mods) {
            reason = OBSOLETE.get(m.legacyId.toLowerCase(Locale.ROOT));
            if (reason == null) return null;
        }
        return reason;
    }

    private static final Pattern MOD_ID = Pattern.compile("modId\\s*=\\s*\"([a-z0-9_.-]+)\"");

    private LibraryBridges() {}

    /**
     * Bridges whose modern mod is present in {@code modsDir} (or forced with -Dlegacyconnector.bridges=baubles,...),
     * minus those switched off with {@code bridges.<name>.enabled = false} in config/legacyconnector.properties; the
     * legacy library of a disabled bridge is then translated and loaded as usual.
     */
    public static List<Bridge> active(Path modsDir) {
        Set<String> modern = modernModIds(modsDir);
        installedModern = forge1201On(modsDir) ? modern : Set.of();
        lastModern = modern;
        Properties config = readConfig(modsDir);
        applyUserConfig(config);
        String forced = System.getProperty("legacyconnector.bridges", "").toLowerCase(Locale.ROOT);
        List<Bridge> out = new ArrayList<>();
        boolean realBaubles = hasJarNamed(modsDir, "baubles");
        // only a legacy CodeChickenLib counts: packs like ATM10 ship the modern one (CodeChickenLib-1.21.1-...), which legacy code cannot
        // use. A 1.12.2 one counts too (CodeChickenLib-1.12.2-3.2.3.358-universal.jar): without it, mods built on the real library
        // (a BakingVertexBuffer passed where a BufferBuilder is expected) linked against the synthesized stand-in and failed to verify
        boolean realCcl = hasLegacyJarNamed(modsDir, "codechickenlib") || hasJarNamed(modsDir.resolve("1.7.10"), "codechickenlib")
                || hasJarNamed(modsDir.resolve("1.12.2"), "codechickenlib")
                // 1.6.4 / 1.5.2: CodeChickenCore's DepLoader downloaded CodeChickenLib into mods/<minecraft version>/
                || hasJarNamed(modsDir.resolve("1.6.4"), "codechickenlib") || hasJarNamed(modsDir.resolve("1.5.2"), "codechickenlib");
        for (Bridge b : KNOWN) {
            if (!enabled(config, b)) continue;
            if ("codechicken/lib/".equals(b.apiRoot) && realCcl) continue;
            // Baubles API shaded into other mods (Extra Utilities) with neither Baubles nor Curios: the shim's own API still
            // gives mods an inventory instead of the shaded one's null.
            if ("baubles".equals(b.configKey) && !realBaubles && !modern.contains("curios")) {
                out.add(b);
                continue;
            }
            if (b.modernModId == null || modern.contains(b.modernModId)
                    || b.legacyModId != null && forced.contains(b.legacyModId.toLowerCase(Locale.ROOT))) out.add(b);
        }
        return out;
    }

    /** True if a jar in the mods folder starts with {@code prefix} and names a legacy Minecraft version (case-insensitive). */
    private static boolean hasLegacyJarNamed(Path modsDir, String prefix) {
        if (!Files.isDirectory(modsDir)) return false;
        try (Stream<Path> files = Files.list(modsDir)) {
            return files.map(f -> f.getFileName().toString().toLowerCase(Locale.ROOT))
                    .anyMatch(n -> n.startsWith(prefix) && (n.contains("1.7.10") || n.contains("1.12.2") || n.contains("1.6.4")
                            || n.contains("1.5.2") || n.contains("1.4.7")) && n.endsWith(".jar"));
        } catch (IOException e) {
            return false;
        }
    }

    /** True if a jar in the mods folder has a file name starting with {@code prefix} (case-insensitive). */
    private static boolean hasJarNamed(Path modsDir, String prefix) {
        if (!Files.isDirectory(modsDir)) return false;
        try (Stream<Path> files = Files.list(modsDir)) {
            return files.anyMatch(f -> f.getFileName().toString().toLowerCase(Locale.ROOT).startsWith(prefix)
                    && f.toString().endsWith(".jar"));
        } catch (IOException e) {
            return false;
        }
    }

    static Properties readConfig(Path modsDir) {
        Properties p = new Properties();
        Path file = modsDir.resolveSibling("config").resolve("legacyconnector.properties");
        if (Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                p.load(r);
            } catch (IOException | RuntimeException ignored) {
                // unreadable config: every bridge stays enabled
            }
        }
        return p;
    }

    private static boolean enabled(Properties config, Bridge b) {
        return !"false".equalsIgnoreCase(config.getProperty("bridges." + b.configKey + ".enabled", "true").trim());
    }

    static Set<String> modernModIds(Path modsDir) {
        Set<String> ids = new HashSet<>();
        if (!Files.isDirectory(modsDir)) return ids;
        try (Stream<Path> files = Files.list(modsDir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".jar")).toList()) {
                try (ZipFile zip = new ZipFile(p.toFile())) {
                    ZipEntry e = zip.getEntry("META-INF/neoforge.mods.toml");
                    if (e == null) continue;
                    try (InputStream in = zip.getInputStream(e)) {
                        Matcher m = MOD_ID.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                        while (m.find()) ids.add(m.group(1));
                    }
                } catch (IOException | RuntimeException ignored) {
                    // not a readable jar; FML reports it
                }
            }
        } catch (IOException ignored) {
            // no mods folder access
        }
        return ids;
    }

    /** True if this legacy jar is the library a bridge replaces (it declares the bridged mod id). */
    public static Bridge replacing(LegacyJar jar, List<Bridge> active) {
        Bridge lib = NativeLibraries1201.replacing(jar, installedModern);
        if (lib != null) return lib;
        for (Bridge b : active) {
            for (LegacyModDecl m : jar.mods) if (b.legacyModId != null && m.legacyId.equalsIgnoreCase(b.legacyModId)) return b;
            if (b.legacyModId != null && jar.mods.isEmpty() && jar.containerModIds.stream().anyMatch(id -> id.equalsIgnoreCase(b.legacyModId))) return b;
        }
        return null;
    }
}
