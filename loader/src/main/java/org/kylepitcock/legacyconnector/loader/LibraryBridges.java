package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
     */
    public record Bridge(String legacyModId, String apiRoot, String modernModId) {}

    public static final List<Bridge> KNOWN = List.of(
            new Bridge("Baubles", "baubles/api/", "curios"),
            // Waila tooltip providers are shown through Jade.
            new Bridge("Waila", "mcp/mobius/waila/api/", "jade"),
            // NEI recipe handlers and hidden items are shown through JEI.
            new Bridge("NotEnoughItems", "codechicken/nei/", "jei"),
            // CoFH's RF energy API (shaded by most 1.7.10 tech mods) is backed by NeoForge's Energy capability.
            new Bridge(null, "cofh/api/energy/", null));

    /**
     * Mods that only make sense on the 1.7.10 engine or launcher (performance/bug-fix patches, ID extenders, mixin
     * loaders) or are client conveniences with established modern equivalents. They are not translated; the report says why.
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
            java.util.Map.entry("notfine", "1.7.10 graphics options; use a modern renderer/options mod"),
            java.util.Map.entry("neid", "1.7.10 ID limit extender; 1.21.1 has no ID limits"),
            java.util.Map.entry("fastleafdecay", "use a modern fast leaf decay mod"),
            java.util.Map.entry("betterloadingscreen", "1.7.10 loading screen; NeoForge has its own"),
            java.util.Map.entry("bettercrashes", "1.7.10 crash handling; not applicable"),
            java.util.Map.entry("letsnecryptcraft", "Java 8 certificate fix; modern Java includes it"),
            java.util.Map.entry("helpfixer", "1.7.10 /help fix; not needed on 1.21.1"),
            java.util.Map.entry("netherportalfix", "1.7.10 portal fix; use a modern equivalent if wanted"),
            java.util.Map.entry("resourceloader", "1.7.10 resource pack loader; 1.21.1 loads resource packs natively"),
            java.util.Map.entry("oauth", "1.7.10 Microsoft login helper; not needed"),
            java.util.Map.entry("inventorytweaks", "client mod with modern equivalents (Inventory Profiles Next, Mouse Tweaks)"),
            java.util.Map.entry("bogosorter", "client mod with modern equivalents (Inventory Profiles Next)"),
            java.util.Map.entry("craftingtweaks", "client mod with a modern version (Crafting Tweaks)"),
            java.util.Map.entry("journeymap", "client mod with a modern version (JourneyMap for 1.21.1)"),
            java.util.Map.entry("navigator", "JourneyMap add-on; use the modern JourneyMap"),
            java.util.Map.entry("custommainmenu", "client mod with modern equivalents (FancyMenu)"),
            java.util.Map.entry("controlling", "client mod with a modern version (Controlling for 1.21.1)"),
            java.util.Map.entry("defaultkeys", "client mod with modern equivalents (Default Options)"),
            java.util.Map.entry("defaultserverlist", "client mod with modern equivalents"),
            java.util.Map.entry("duradisplay", "client mod with modern equivalents"),
            java.util.Map.entry("overloadedarmorbar", "client mod with modern equivalents"),
            java.util.Map.entry("torohealthmod", "client mod with modern equivalents (ToroHealth)"),
            java.util.Map.entry("findit", "client mod with modern equivalents"),
            java.util.Map.entry("darkerer", "client mod with modern equivalents"),
            java.util.Map.entry("betterachievements", "1.21 replaced achievements with advancements"),
            java.util.Map.entry("craftpresence", "client mod with a modern version (CraftPresence)"),
            java.util.Map.entry("ingameinfoxml", "client mod with modern equivalents"),
            java.util.Map.entry("opis", "1.7.10 profiler; use Spark"));

    /** Why a legacy jar is obsolete on 1.21.1 (all its mods are on the list), or null. */
    public static String obsoleteReason(LegacyJar jar) {
        if (!jar.hasMods()) return null;
        String reason = null;
        for (LegacyModDecl m : jar.mods) {
            reason = OBSOLETE.get(m.legacyId.toLowerCase(Locale.ROOT));
            if (reason == null) return null;
        }
        return reason;
    }

    private static final Pattern MOD_ID = Pattern.compile("modId\\s*=\\s*\"([a-z0-9_.-]+)\"");

    private LibraryBridges() {}

    /** Bridges whose modern mod is present in {@code modsDir} (or forced with -Dlegacyconnector.bridges=baubles,...). */
    public static List<Bridge> active(Path modsDir) {
        Set<String> modern = modernModIds(modsDir);
        String forced = System.getProperty("legacyconnector.bridges", "").toLowerCase(Locale.ROOT);
        List<Bridge> out = new ArrayList<>();
        for (Bridge b : KNOWN) {
            if (b.modernModId == null || modern.contains(b.modernModId)
                    || b.legacyModId != null && forced.contains(b.legacyModId.toLowerCase(Locale.ROOT))) out.add(b);
        }
        return out;
    }

    private static Set<String> modernModIds(Path modsDir) {
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
        for (Bridge b : active) {
            for (LegacyModDecl m : jar.mods) if (b.legacyModId != null && m.legacyId.equalsIgnoreCase(b.legacyModId)) return b;
        }
        return null;
    }
}
