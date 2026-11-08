package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Side-by-side libraries (docs/wiki/Bridges.md, "Library decision order"): a legacy library that clashes with a native 1.21.1 jar or with another legacy
 * version's copy of the same library is loaded as an isolated unit instead of being skipped. A unit has
 * <ul>
 *   <li>a distinct FML mod id ({@code <id>_l1201}; registry namespaces are not touched, they stay the legacy mod's own),</li>
 *   <li>its Java packages relocated under {@code legacyconnector_relocated/sbs_<version>/} in the library and in every mod of the SAME legacy
 *       version (bytecode, annotation and string constants, {@code META-INF/services}, access transformers, mixin config package/plugin),</li>
 *   <li>renamed mixin config files, so two copies never share a config name.</li>
 * </ul>
 * The mechanism is version agnostic; the units are keyed by {@link LegacyVersion}. Older versions relocate packages through
 * {@code CrossVersionPackages} already and only need the id isolation; the 1.20.1 pipeline has no such pass and uses the class relocation here.
 */
public final class SideBySide {
    public static final SideBySide NONE = new SideBySide(Map.of());
    private static volatile SideBySide current = NONE;

    /** One isolated library copy. {@code roots} are internal package roots ({@code com/github/alexthe666/citadel}). */
    public record Unit(LegacyVersion version, String modId, String newId, Set<String> roots, String reason) {}

    private final Map<LegacyVersion, Map<String, Unit>> units;

    public SideBySide(Map<LegacyVersion, Map<String, Unit>> units) {
        this.units = units;
    }

    public static SideBySide current() {
        return current;
    }

    public static void setCurrent(SideBySide plan) {
        current = plan == null ? NONE : plan;
    }

    public static SideBySide of(Collection<Unit> list) {
        Map<LegacyVersion, Map<String, Unit>> m = new LinkedHashMap<>();
        for (Unit u : list) m.computeIfAbsent(u.version(), k -> new LinkedHashMap<>()).put(u.modId(), u);
        return new SideBySide(m);
    }

    public boolean isEmpty() {
        return units.isEmpty();
    }

    public Collection<Unit> units() {
        return units.values().stream().flatMap(m -> m.values().stream()).toList();
    }

    /** The FML id a mod of {@code version} with this modern id loads as (the id itself when it is not an isolated unit). */
    public String newId(LegacyVersion version, String modId) {
        Unit u = unit(version, modId);
        return u == null ? modId : u.newId();
    }

    public Unit unit(LegacyVersion version, String modId) {
        Map<String, Unit> m = units.get(version);
        return m == null ? null : m.get(modId);
    }

    public static String newIdFor(String modId, LegacyVersion version) {
        return modId + "_l" + version.id().replaceAll("[^a-z0-9]", "");
    }

    /** Where the relocated copy of {@code version} goes. */
    public static String prefix(LegacyVersion version) {
        return "legacyconnector_relocated/sbs_" + version.id().replaceAll("[^A-Za-z0-9]", "_") + "/";
    }

    // ---- names ------------------------------------------------------------------------------------------------------------------

    /** The relocated name of a class (internal name) as seen by mods of {@code version}, or null when it is not in an isolated package. */
    public String mapClass(LegacyVersion version, String internalName) {
        Map<String, Unit> m = units.get(version);
        if (m == null) return null;
        for (Unit u : m.values()) if (inRoots(u.roots(), internalName)) return prefix(version) + internalName;
        return null;
    }

    private static boolean inRoots(Set<String> roots, String name) {
        for (String r : roots) {
            if (name.startsWith(r) && (name.length() == r.length() || name.charAt(r.length()) == '/' || name.charAt(r.length()) == '$')) return true;
        }
        return false;
    }

    /**
     * A string constant that names an isolated class or package, in dotted ({@code Class.forName}, mixin targets) or slashed form; anything else is
     * returned unchanged.
     */
    public String mapString(LegacyVersion version, String s) {
        if (s == null || s.length() < 4 || units.get(version) == null) return s;
        boolean dotted = s.indexOf('/') < 0;
        String internal = dotted ? s.replace('.', '/') : s;
        if (!dotted && s.indexOf('.') >= 0 && !s.endsWith(".class")) return s;
        String moved = mapClass(version, internal);
        if (moved == null) return s;
        return dotted ? moved.replace('/', '.') : moved;
    }

    /** Text files that name classes (services, access transformers): every occurrence of an isolated root, dotted or slashed. */
    public String rewriteText(LegacyVersion version, String text) {
        Map<String, Unit> m = units.get(version);
        if (m == null) return text;
        for (Unit u : m.values()) {
            for (String root : u.roots()) {
                String dotted = root.replace('/', '.');
                String target = prefix(version).replace('/', '.') + dotted;
                text = Pattern.compile("(?<![\\w.])" + Pattern.quote(dotted) + "(?![\\w])").matcher(text).replaceAll(Matcher.quoteReplacement(target));
            }
        }
        return text;
    }

    /** Resources that moved with the classes (services files, resources inside isolated package folders); ids go through {@link #rewriteText}. */
    public Map<String, byte[]> rewriteResources(LegacyVersion version, Map<String, byte[]> resources) {
        if (units.get(version) == null) return resources;
        Map<String, byte[]> out = new LinkedHashMap<>();
        for (var e : resources.entrySet()) {
            String name = e.getKey();
            byte[] data = e.getValue();
            if (name.startsWith("META-INF/services/") && name.length() > "META-INF/services/".length()) {
                String iface = name.substring("META-INF/services/".length());
                String moved = mapString(version, iface);
                name = "META-INF/services/" + moved;
                data = rewriteText(version, new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
            } else if (name.startsWith("META-INF/") && name.endsWith(".cfg")) {
                data = rewriteText(version, new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
            } else if (!name.startsWith("META-INF/") && !name.startsWith("assets/") && !name.startsWith("data/")) {
                String moved = mapClass(version, name.indexOf('.') > 0 ? name.substring(0, name.lastIndexOf('.')) : name);
                if (moved != null && name.indexOf('/') > 0) name = prefix(version) + name;
            }
            out.put(name, data);
        }
        return out;
    }

    /**
     * Mixin config file names an isolated library renames ({@code citadel.mixins.json} to {@code citadel_l1201.mixins.json}): two copies would
     * otherwise share one resource name. Only the configs of the unit's own jar are renamed.
     */
    public static String mixinConfigName(String name, String suffix) {
        return name.endsWith(".mixins.json") ? name.substring(0, name.length() - ".mixins.json".length()) + suffix + ".mixins.json"
                : name.endsWith(".json") ? name.substring(0, name.length() - 5) + suffix + ".json" : name + suffix;
    }

    /**
     * mods.toml text of a 1.20.1 jar (already in the NeoForge format): ids of isolated libraries are replaced in dependency entries of every jar
     * and, for the unit's own jar ({@code ownIds}), in the {@code [[mods]]} entry and the {@code [[dependencies.<id>]]} headers.
     */
    public String rewriteToml(LegacyVersion version, String toml, Set<String> ownIds) {
        Map<String, Unit> m = units.get(version);
        if (m == null) return toml;
        for (Unit u : m.values()) {
            Pattern id = Pattern.compile("(modId\\s*=\\s*)([\"'])" + Pattern.quote(u.modId()) + "\\2", Pattern.CASE_INSENSITIVE);
            toml = id.matcher(toml).replaceAll("$1$2" + Matcher.quoteReplacement(u.newId()) + "$2");
            if (ownIds.contains(u.modId())) {
                toml = toml.replace("[[dependencies." + u.modId() + "]]", "[[dependencies." + u.newId() + "]]");
            }
        }
        return toml;
    }

    // ---- planning ---------------------------------------------------------------------------------------------------------------

    private static final Pattern FORBIDDEN_ROOT = Pattern.compile("(java|javax|jdk|sun|net/minecraft|net/neoforged|net/minecraftforge|com/mojang|org/spongepowered|org/objectweb|legacyconnector.*)(/.*)?");

    /** The package roots of a set of class packages: packages without an ancestor package in the set, at least two segments deep. */
    public static Set<String> rootsOf(Collection<String> packages) {
        TreeSet<String> sorted = new TreeSet<>(packages);
        Set<String> roots = new TreeSet<>();
        for (String p : sorted) {
            if (p.indexOf('/') < 0 || FORBIDDEN_ROOT.matcher(p).matches()) continue;
            boolean covered = false;
            for (String r : roots) if (p.equals(r) || p.startsWith(r + "/")) { covered = true; break; }
            if (!covered) roots.add(p);
        }
        return roots;
    }

    /** The class packages of a jar (internal names), without {@code META-INF}. */
    public static Set<String> packagesOf(Path jar) throws IOException {
        Set<String> out = new TreeSet<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                String n = en.nextElement().getName();
                if (!n.endsWith(".class") || n.startsWith("META-INF/")) continue;
                int slash = n.lastIndexOf('/');
                if (slash > 0) out.add(n.substring(0, slash));
            }
        }
        return out;
    }

    /** Config: {@code experimental.sideBySideLibraries} (default true; the 1.20.1 key {@code experimental.forge1201.sideBySideLibraries} is an alias). */
    public static boolean enabled(java.util.Properties config) {
        String prop = System.getProperty("legacyconnector.sideBySideLibraries");
        if (prop != null && !prop.isBlank()) return Boolean.parseBoolean(prop.trim());
        String v = config.getProperty("experimental.sideBySideLibraries", config.getProperty("experimental.forge1201.sideBySideLibraries", "true"));
        return !"false".equalsIgnoreCase(v.trim());
    }

    /** {@code experimental.sideBySideLibraries.include / .exclude}: comma separated mod ids (lower case). */
    public static Set<String> idList(java.util.Properties config, String suffix) {
        Set<String> out = new TreeSet<>();
        String v = config.getProperty("experimental.sideBySideLibraries." + suffix, config.getProperty("experimental.forge1201.sideBySideLibraries." + suffix, ""));
        for (String s : v.split(",")) if (!s.isBlank()) out.add(s.trim().toLowerCase(Locale.ROOT));
        return out;
    }

    /** Everything translations depend on, for cache keys. */
    public String fingerprint() {
        Map<String, String> m = new TreeMap<>();
        for (Unit u : units()) m.put(u.version().id() + ":" + u.modId(), u.newId() + "=" + u.roots());
        return "sbs" + m;
    }
}
