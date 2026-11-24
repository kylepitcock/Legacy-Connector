package org.kylepitcock.legacyconnector.loader.transform;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;

/**
 * Class packages that jars of more than one legacy version define (a library shipped for both, e.g. CodeChickenLib for
 * 1.7.10 and 1.12.2; an API shaded by a 1.7.10 mod and shipped by its 1.12.2 owner). Same names, different code: the
 * oldest version keeps the names and every other version's copy is relocated, so each version's mods link to their own
 * copy. A class only another version's jars have (a shaded API missing a class) links to that version's copy, as the one
 * merged copy of a package did before.
 *
 * <p>Packages a modern mod of the pack also defines are relocated in every legacy version's jars: two modules exporting
 * one package stop the launch (DragonAPI ships the 1.7.10 AE2 API, {@code appeng.api.*}, which modern AE2 also has). A
 * class only the modern mod has keeps its name, so such references still link to the modern mod.
 */
public final class CrossVersionPackages {
    public static final CrossVersionPackages NONE = new CrossVersionPackages();

    private final Map<String, EnumSet<LegacyVersion>> packages = new HashMap<>();
    private final Map<String, EnumSet<LegacyVersion>> classes = new HashMap<>();
    private final Set<String> modern = new TreeSet<>();

    /** Records that a jar of {@code version} defines {@code className} (internal name). */
    public void add(LegacyVersion version, String className) {
        int slash = className.lastIndexOf('/');
        if (slash <= 0) return;
        packages.computeIfAbsent(className.substring(0, slash), k -> EnumSet.noneOf(LegacyVersion.class)).add(version);
        classes.computeIfAbsent(className, k -> EnumSet.noneOf(LegacyVersion.class)).add(version);
    }

    /**
     * Records the packages modern jars define (internal names, {@code a/b}); legacy copies of them are relocated.
     * {@code keep} are packages never relocated (those of {@code @Mod} classes). Call before {@link #done()}.
     */
    public CrossVersionPackages modern(Set<String> modernPackages, Set<String> keep) {
        for (String pkg : packages.keySet()) if ((modernPackages.contains(pkg) || hiddenRoot(pkg)) && !keep.contains(pkg)) modern.add(pkg);
        return this;
    }

    /**
     * A package under the root folder {@code assets} or {@code data}: FML leaves those roots out of a mod's module (they are resource
     * roots), so a class there (Rival Rebels' {@code assets.rivalrebels.RivalRebels}) is not found by the game's class loader. Moved
     * like the packages of a modern mod.
     */
    static boolean hiddenRoot(String pkg) {
        return pkg.equals("assets") || pkg.equals("data") || pkg.startsWith("assets/") || pkg.startsWith("data/");
    }

    /** Packages of legacy jars a modern jar also defines. */
    public Set<String> modernClashes() {
        return modern;
    }

    /** Drops everything but the packages defined by several versions or by a modern jar; call once all jars are added. */
    public CrossVersionPackages done() {
        packages.entrySet().removeIf(e -> e.getValue().size() < 2 && !modern.contains(e.getKey()));
        classes.keySet().removeIf(c -> !packages.containsKey(c.substring(0, c.lastIndexOf('/'))));
        return this;
    }

    public boolean isEmpty() {
        return packages.isEmpty();
    }

    /** Packages whose copy in {@code version}'s jars is relocated. */
    public Set<String> relocated(LegacyVersion version) {
        Set<String> out = new TreeSet<>();
        packages.forEach((pkg, versions) -> {
            if (versions.contains(version) && (versions.iterator().next() != version || modern.contains(pkg))) out.add(pkg);
        });
        return out;
    }

    /** Everything the mapping depends on, for cache keys: a jar's translation changes when another version's jars do. */
    public String fingerprint() {
        StringBuilder sb = new StringBuilder();
        new java.util.TreeMap<>(classes).forEach((c, v) -> sb.append(c).append('=').append(v).append(';'));
        modern.forEach(p -> sb.append("modern:").append(p).append(';'));
        return sb.toString();
    }

    /** Where the copy of {@code version} goes. */
    public static String prefix(LegacyVersion version) {
        // per source version (not per shim: 1.4.7, 1.5.2 and 1.6.4 share one shim, but their copies of a package differ and must not merge)
        return "legacyconnector_relocated/v" + version.id().replace('.', '_') + "/";
    }

    /**
     * The name a reference from a {@code version} class to {@code internalName} links to, or null when the class is not
     * in a package shared between versions. Nested class names are passed whole ({@code a/b/C$D}).
     */
    public String map(LegacyVersion version, String internalName) {
        if (packages.isEmpty()) return null;
        int slash = internalName.lastIndexOf('/');
        if (slash <= 0) return null;
        EnumSet<LegacyVersion> pkgVersions = packages.get(internalName.substring(0, slash));
        if (pkgVersions == null) return null;
        EnumSet<LegacyVersion> owners = classes.get(internalName);
        if (modern.contains(internalName.substring(0, slash))) {
            if (owners == null) return internalName; // only the modern mod has it
            return prefix(owners.contains(version) ? version : owners.iterator().next()) + internalName;
        }
        LegacyVersion copy = owners == null || owners.contains(version) ? version : owners.iterator().next();
        return pkgVersions.iterator().next() == copy ? internalName : prefix(copy) + internalName;
    }
}
