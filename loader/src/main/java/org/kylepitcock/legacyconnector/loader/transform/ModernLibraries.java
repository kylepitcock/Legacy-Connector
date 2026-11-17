package org.kylepitcock.legacyconnector.loader.transform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Modern mods (jars of the pack's mods folder) whose mod id a legacy mod lists as a REQUIRED dependency: CodeChickenLib for 1.12.2 Thermal Expansion
 * ({@code required-after:codechickenlib}) is satisfied by id when only CodeChickenLib 1.21.1 is installed, but that is another API generation with
 * the same package names. A legacy reference into such a package links only when that exact class (and member) exists in the modern jar; the link check
 * reads the modern jar's classes through this index and reports what is missing, instead of treating the shared package as a match.
 */
public final class ModernLibraries {
    public static final ModernLibraries NONE = new ModernLibraries(Map.of());

    /** One modern mod: its (lower-case) id, the jar and the packages (internal names) that jar defines. */
    public record Lib(String id, Path jar, Set<String> packages) {}

    private static final Map<Path, ZipFile> OPEN = new ConcurrentHashMap<>();

    private final Map<String, Lib> libs;
    private final Map<String, Lib> byPackage = new HashMap<>();

    public ModernLibraries(Map<String, Lib> libsById) {
        this.libs = libsById;
        for (Lib l : libsById.values()) {
            for (String p : l.packages()) byPackage.putIfAbsent(p, l);
            String root = commonRoot(l.packages());
            if (root != null) roots.putIfAbsent(root, l);
        }
    }

    /** Package roots ({@code codechicken/lib}): a class under one belongs to that mod's API even where the modern jar has no such package (the legacy one had more). */
    private final Map<String, Lib> roots = new HashMap<>();

    /** The longest package prefix, at least two segments, that all of {@code packages} share; null when there is none. */
    static String commonRoot(Set<String> packages) {
        String[] common = null;
        for (String p : packages) {
            String[] seg = p.split("/");
            if (common == null) {
                common = seg;
                continue;
            }
            int n = 0;
            while (n < common.length && n < seg.length && common[n].equals(seg[n])) n++;
            common = java.util.Arrays.copyOf(common, n);
        }
        return common == null || common.length < 2 ? null : String.join("/", common);
    }

    public boolean isEmpty() {
        return libs.isEmpty();
    }

    /** The libraries among {@code ids} (lower-case mod ids), as a view of only those. */
    public ModernLibraries restrictedTo(Set<String> ids) {
        if (libs.isEmpty() || ids.isEmpty()) return NONE;
        Map<String, Lib> sub = new HashMap<>();
        for (String id : ids) {
            Lib l = libs.get(id);
            if (l != null) sub.put(id, l);
        }
        return sub.isEmpty() ? NONE : new ModernLibraries(sub);
    }

    /** The library whose jar defines the package of {@code internalName}, or null. */
    public Lib ownerOfPackage(String internalName) {
        int slash = internalName.lastIndexOf('/');
        if (slash <= 0) return null;
        String pkg = internalName.substring(0, slash);
        Lib l = byPackage.get(pkg);
        if (l != null) return l;
        for (var e : roots.entrySet()) if (pkg.startsWith(e.getKey() + "/")) return e.getValue();
        return null;
    }

    /** The bytes of {@code internalName.class} from the library that defines its package, or null when that jar lacks the class. */
    public byte[] read(String internalName) {
        Lib l = ownerOfPackage(internalName);
        if (l == null) return null;
        try {
            ZipFile zip = OPEN.computeIfAbsent(l.jar(), p -> {
                try {
                    return new ZipFile(p.toFile());
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
            ZipEntry e = zip.getEntry(internalName + ".class");
            if (e == null) return null;
            try (InputStream in = zip.getInputStream(e)) {
                return in.readAllBytes();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** What decides translations: library ids with their jar names, sizes and package sets (a cache key part). */
    public String fingerprint() {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(libs).forEach((id, l) -> {
            long size = 0;
            try {
                size = Files.size(l.jar());
            } catch (IOException ignored) {
                // the name alone still changes the key
            }
            sb.append(id).append('=').append(l.jar().getFileName()).append(':').append(size).append(':').append(new HashSet<>(l.packages()).hashCode()).append(';');
        });
        return sb.toString();
    }
}
