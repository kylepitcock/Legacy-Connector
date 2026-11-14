package org.kylepitcock.legacyconnector.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The dependencies a legacy mod declares ({@code @Mod(dependencies = "...")}, or the lists of its mcmod.info), checked against what the pack
 * has. FML refused to start when a {@code required-*} mod was absent; here such a mod is not run and the report says which mod is missing,
 * instead of the mod dying in preInit on a {@code NoClassDefFoundError} of the absent library.
 */
public final class LegacyDependencies {
    private LegacyDependencies() {}

    /** One entry of a dependency string: {@code kind} is {@code required-after}, {@code required-before}, {@code after}, ...; {@code range} may be empty. */
    public record Dep(String kind, String id, String range) {
        /** {@code required-after}, {@code required-before}, {@code required-client}, {@code required-server}, {@code required}. */
        public boolean required() {
            return kind.startsWith("required");
        }
    }

    /** A required dependency that is not there ({@code fatal}), or that is older than asked (a warning only). */
    public record Problem(LegacyModDecl mod, Dep dep, String message, boolean fatal) {
        /** The modern mod that stands in for the missing dependency on 1.21.1 (Curios for Baubles, ...), or null. */
        public String modernMod() {
            for (LibraryBridges.Bridge b : LibraryBridges.KNOWN) {
                if (b.legacyModId() != null && b.legacyModId().equalsIgnoreCase(dep.id()) && b.modernModId() != null) return b.modernModId();
            }
            return null;
        }
    }

    /** Ids that are always there: the loader itself, not mods. */
    private static final Set<String> BUILTIN = Set.of("forge", "fml", "minecraft", "minecraftforge", "mcp", "*");

    /** Parses {@code required-after:forge@[14.23.5.2768,);required-after:cofhcore@[4.6.0,4.7.0);after:jei}; malformed pieces are skipped. */
    public static List<Dep> parse(String dependencies) {
        List<Dep> out = new ArrayList<>();
        if (dependencies == null) return out;
        for (String part : splitTop(dependencies)) {
            part = part.trim();
            int colon = part.indexOf(':');
            if (colon <= 0) continue;
            String kind = part.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String target = part.substring(colon + 1).trim();
            String range = "";
            int at = target.indexOf('@');
            if (at >= 0) {
                range = target.substring(at + 1).trim();
                target = target.substring(0, at).trim();
            }
            if (target.isEmpty() || !(kind.contains("after") || kind.contains("before") || kind.startsWith("required"))) continue;
            out.add(new Dep(kind, target, range));
        }
        return out;
    }

    /** Splits at ';' outside brackets. */
    private static List<String> splitTop(String s) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[' || c == '(') depth++;
            else if ((c == ']' || c == ')') && depth > 0) depth--;
            else if (c == ';' && depth == 0) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }

    /**
     * Every required dependency of the mods of {@code jars} that is absent (fatal), or older than the range asks (a warning).
     *
     * @param present lower-case ids of everything that counts as installed: legacy mods and APIs, bridged libraries whose modern mod is there,
     *                the modern mods of the pack, the obsolete (not loaded, satisfied) ones
     * @param active  the bridges in use; a dependency on a bridged library whose modern mod is missing says which one to install
     * @param client  true on the client: {@code required-client} counts and {@code required-server} does not (and the other way round)
     */
    public static List<Problem> check(List<LegacyJar> jars, Set<String> present, List<LibraryBridges.Bridge> active, boolean client) {
        Map<String, String> versions = new LinkedHashMap<>();
        for (LegacyJar j : jars) for (LegacyModDecl m : j.mods) versions.putIfAbsent(m.legacyId.toLowerCase(Locale.ROOT), m.version);
        List<Problem> out = new ArrayList<>();
        for (LegacyJar jar : jars) {
            for (LegacyModDecl mod : jar.mods) {
                for (Dep dep : parse(mod.dependencies)) {
                    if (!dep.required()) continue;
                    if (dep.kind().endsWith("client") && !client || dep.kind().endsWith("server") && client) continue;
                    String id = dep.id().toLowerCase(Locale.ROOT);
                    if (BUILTIN.contains(id) || present.contains(id) || present.contains(LibraryBridges.alias(dep.id()).toLowerCase(Locale.ROOT))) {
                        String have = versions.get(id);
                        if (have != null && tooOld(have, dep.range())) {
                            out.add(new Problem(mod, dep, displayName(mod) + " wants " + dep.id() + " " + dep.range() + " but " + have + " is installed", false));
                        }
                        continue;
                    }
                    out.add(new Problem(mod, dep, message(mod, dep), true));
                }
            }
        }
        return out;
    }

    /** Everything that counts as installed for {@link #check}, as lower-case ids. */
    public static Set<String> presentIds(List<LegacyJar> jars, List<LibraryBridges.Bridge> active, Set<String> modernIds, Iterable<String> apiIds) {
        Set<String> out = new java.util.HashSet<>();
        for (LegacyJar j : jars) {
            for (LegacyModDecl m : j.mods) {
                out.add(m.legacyId.toLowerCase(Locale.ROOT));
                out.add(m.modernId().toLowerCase(Locale.ROOT));
            }
            for (String id : j.containerModIds) out.add(id.toLowerCase(Locale.ROOT));
        }
        for (LibraryBridges.Bridge b : active) if (b.legacyModId() != null) out.add(b.legacyModId().toLowerCase(Locale.ROOT));
        for (String id : modernIds) out.add(id.toLowerCase(Locale.ROOT));
        for (String id : apiIds) out.add(id.toLowerCase(Locale.ROOT));
        out.addAll(LibraryBridges.obsoleteIds());
        return out;
    }

    /**
     * The text of the loading-warning screen, as a MessageFormat pattern. FML resolves the keys of loading issues from its own bundled
     * language file only (mod language files are not readable at this phase), so the pattern itself is the key: an unknown key is
     * shown as is, with the arguments filled in. No apostrophes or braces other than the placeholders.
     */
    public static String issueKey(Problem p) {
        return p.modernMod() == null
                ? "{0} ({1}) requires {2}{3}, which is not installed. Put its jar for Minecraft {1} into mods/{1}/. {0} may fail to load or miss features without it."
                : "{0} ({1}) requires {2}{3} (provided on 1.21.1 by {4}): install {4}. {0} may fail to load or miss features without it.";
    }

    /** Arguments of {@link #issueKey}: mod name, Minecraft version, dependency, " <version range>" or empty, modern mod. */
    public static Object[] issueArgs(Problem p, String minecraftVersion) {
        return new Object[] {displayName(p.mod()), minecraftVersion, p.dep().id(), p.dep().range().isEmpty() ? "" : " " + p.dep().range(),
                p.modernMod() == null ? "" : p.modernMod()};
    }

    private static String displayName(LegacyModDecl mod) {
        return mod.name != null && !mod.name.isBlank() ? mod.name : mod.legacyId;
    }

    /** {@code <Mod name> requires <dependency id> (<version range if given>), which is not installed}, plus what to install when a bridge covers it. */
    static String message(LegacyModDecl mod, Dep dep) {
        StringBuilder sb = new StringBuilder(displayName(mod)).append(" requires ").append(dep.id());
        if (!dep.range().isEmpty()) sb.append(" (").append(dep.range()).append(')');
        sb.append(", which is not installed");
        for (LibraryBridges.Bridge b : LibraryBridges.KNOWN) {
            if (b.legacyModId() == null || !b.legacyModId().equalsIgnoreCase(dep.id())) continue;
            if (b.modernModId() != null) sb.append(" (install the modern ").append(b.modernModId()).append(" mod, which Legacy Connector uses in its place, or the legacy jar)");
            break;
        }
        return sb.toString();
    }

    /** True when {@code have} is older than the lower bound of {@code range}; false when either is not a plain dotted number. */
    static boolean tooOld(String have, String range) {
        if (range == null || range.isBlank() || have == null) return false;
        String r = range.trim();
        String lower = r;
        if (r.startsWith("[") || r.startsWith("(")) {
            int comma = r.indexOf(',');
            lower = r.substring(1, comma < 0 ? r.length() - 1 : comma).trim();
        }
        if (lower.isEmpty()) return false;
        int[] a = numbers(have), b = numbers(lower);
        if (a == null || b == null) return false;
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0, y = i < b.length ? b[i] : 0;
            if (x != y) return x < y;
        }
        return false;
    }

    private static int[] numbers(String v) {
        List<Integer> out = new ArrayList<>();
        for (String p : v.trim().split("[.\\-]")) {
            if (!p.matches("\\d{1,9}")) break;
            out.add(Integer.parseInt(p));
        }
        if (out.isEmpty()) return null;
        return out.stream().mapToInt(Integer::intValue).toArray();
    }
}
