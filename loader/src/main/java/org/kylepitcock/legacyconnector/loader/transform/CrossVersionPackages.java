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
 */
public final class CrossVersionPackages {
    public static final CrossVersionPackages NONE = new CrossVersionPackages();

    private final Map<String, EnumSet<LegacyVersion>> packages = new HashMap<>();
    private final Map<String, EnumSet<LegacyVersion>> classes = new HashMap<>();

    /** Records that a jar of {@code version} defines {@code className} (internal name). */
    public void add(LegacyVersion version, String className) {
        int slash = className.lastIndexOf('/');
        if (slash <= 0) return;
        packages.computeIfAbsent(className.substring(0, slash), k -> EnumSet.noneOf(LegacyVersion.class)).add(version);
        classes.computeIfAbsent(className, k -> EnumSet.noneOf(LegacyVersion.class)).add(version);
    }

    /** Drops everything but the packages defined by several versions; call once all jars are added. */
    public CrossVersionPackages done() {
        packages.values().removeIf(v -> v.size() < 2);
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
            if (versions.contains(version) && versions.iterator().next() != version) out.add(pkg);
        });
        return out;
    }

    /** Everything the mapping depends on, for cache keys: a jar's translation changes when another version's jars do. */
    public String fingerprint() {
        StringBuilder sb = new StringBuilder();
        new java.util.TreeMap<>(classes).forEach((c, v) -> sb.append(c).append('=').append(v).append(';'));
        return sb.toString();
    }

    /** Where the copy of {@code version} goes. */
    public static String prefix(LegacyVersion version) {
        return "legacyconnector_relocated/" + version.shimPrefix().substring("legacyconnector/".length());
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
        LegacyVersion copy = owners == null || owners.contains(version) ? version : owners.iterator().next();
        return pkgVersions.iterator().next() == copy ? internalName : prefix(copy) + internalName;
    }
}
