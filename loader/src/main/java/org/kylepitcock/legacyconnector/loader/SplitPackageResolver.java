package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Legacy mods routinely shaded other mods' APIs ({@code cofh/api/energy}, {@code ic2/api}, {@code baubles/api}).
 * On a 1.7.10 class path the first copy simply won; under JPMS two modules exporting the same package is a
 * fatal boot error. This resolver moves every package that appears in more than one translated jar into a
 * single shared {@code GAMELIBRARY} jar and strips it from the individual jars.
 */
final class SplitPackageResolver {
    private SplitPackageResolver() {}

    /**
     * Packages the game itself provides as modules (NeoForge ships {@code jakarta.activation}, which exports {@code javax.activation}): a legacy jar
     * bundling a copy is stripped of it and the copy is not shared, or the module layer fails to resolve.
     */
    private static final Set<String> DROP = Set.of("javax/activation");

    /** DROP, plus the JDK's own XML packages (Mo' Creatures bundles javax.xml.namespace.QName, which java.xml exports). */
    private static boolean dropped(String pkg) {
        return DROP.contains(pkg) || pkg.equals("javax/xml") || pkg.startsWith("javax/xml/") || pkg.startsWith("org/w3c/dom") || pkg.startsWith("org/xml/sax");
    }

    record Outcome(List<Path> jars, Path sharedLibrary, Set<String> sharedPackages) {}

    static Outcome resolve(List<Path> jars, Path cacheDir) throws IOException {
        Map<String, List<Path>> owners = new TreeMap<>();
        Map<Path, Map<String, Integer>> packageSizes = new HashMap<>();
        for (Path jar : jars) {
            Map<String, Integer> sizes = new HashMap<>();
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                Enumeration<? extends ZipEntry> en = zip.entries();
                while (en.hasMoreElements()) {
                    String n = en.nextElement().getName();
                    if (!n.endsWith(".class") || n.startsWith("META-INF/")) continue;
                    int slash = n.lastIndexOf('/');
                    if (slash < 0) continue; // default package classes can never be exported anyway
                    sizes.merge(n.substring(0, slash), 1, Integer::sum);
                }
            }
            packageSizes.put(jar, sizes);
            for (String pkg : sizes.keySet()) owners.computeIfAbsent(pkg, k -> new ArrayList<>()).add(jar);
        }

        Set<String> shared = new TreeSet<>();
        owners.forEach((pkg, list) -> {
            if (list.size() > 1 || dropped(pkg)) shared.add(pkg);
        });
        if (shared.isEmpty()) return new Outcome(jars, null, shared);

        // Content-address everything so repeated launches reuse the same files.
        MessageDigest digest = sha256();
        for (Path jar : jars) digest.update(jar.getFileName().toString().getBytes());
        for (String pkg : shared) digest.update(pkg.getBytes());
        String key = HexFormat.of().formatHex(digest.digest()).substring(0, 16);

        Path sharedJar = cacheDir.resolve("shared-apis-" + key + ".jar");
        List<Path> out = new ArrayList<>();
        List<Path> toStrip = new ArrayList<>();
        Map<Path, Path> strippedOf = new HashMap<>();
        for (Path jar : jars) {
            Set<String> mine = new HashSet<>(packageSizes.get(jar).keySet());
            mine.retainAll(shared);
            if (mine.isEmpty()) {
                out.add(jar);
                continue;
            }
            Path stripped = cacheDir.resolve(stripExt(jar.getFileName().toString()) + "-split-" + key + ".jar");
            out.add(stripped);
            strippedOf.put(jar, stripped);
            if (!Files.exists(stripped)) toStrip.add(jar);
        }

        // Warm launch: the merged library and every stripped jar exist, so no class has to be read at all (the key names every input).
        boolean needShared = !Files.exists(sharedJar);
        if (!needShared && toStrip.isEmpty()) return new Outcome(out, sharedJar, shared);

        // Cold: every jar is independent. The stripped copies and the shared classes are collected in parallel and merged in jar order.
        Set<Path> toRead = new HashSet<>(toStrip);
        if (needShared) toRead.addAll(strippedOf.keySet());
        Map<Path, Map<String, byte[]>> collected = new java.util.concurrent.ConcurrentHashMap<>();
        Map<Path, IOException> failure = new java.util.concurrent.ConcurrentHashMap<>();
        jars.parallelStream().filter(toRead::contains).forEach(jar -> {
            try {
                Set<String> mine = new HashSet<>(packageSizes.get(jar).keySet());
                mine.retainAll(shared);
                try (ZipFile zip = new ZipFile(jar.toFile())) {
                    if (needShared) {
                        Map<String, byte[]> mineClasses = new TreeMap<>();
                        Enumeration<? extends ZipEntry> en = zip.entries();
                        while (en.hasMoreElements()) {
                            ZipEntry e = en.nextElement();
                            String n = e.getName();
                            int slash = n.lastIndexOf('/');
                            if (!n.endsWith(".class") || slash < 0 || !mine.contains(n.substring(0, slash)) || dropped(n.substring(0, slash))) continue;
                            try (InputStream in = zip.getInputStream(e)) {
                                mineClasses.put(n, in.readAllBytes());
                            }
                        }
                        collected.put(jar, mineClasses);
                    }
                    if (!Files.exists(strippedOf.get(jar))) copyWithout(jar, zip, strippedOf.get(jar), mine);
                }
            } catch (IOException e) {
                failure.put(jar, e);
            }
        });
        if (!failure.isEmpty()) throw failure.values().iterator().next();

        if (needShared) {
            Map<String, byte[]> sharedClasses = new TreeMap<>();
            for (Path jar : jars) {
                Map<String, byte[]> classes = collected.get(jar);
                if (classes == null) continue;
                Set<String> mine = new HashSet<>(packageSizes.get(jar).keySet());
                mine.retainAll(shared);
                Map<String, Boolean> best = new HashMap<>();
                for (String pkg : mine) best.put(pkg, bestCopy(pkg, owners.get(pkg), packageSizes).equals(jar));
                for (var e : classes.entrySet()) {
                    String n = e.getKey();
                    if (best.get(n.substring(0, n.lastIndexOf('/')))) sharedClasses.put(n, e.getValue());
                    else sharedClasses.putIfAbsent(n, e.getValue());
                }
            }
            Manifest m = new Manifest();
            m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            m.getMainAttributes().putValue("FMLModType", "GAMELIBRARY");
            m.getMainAttributes().putValue("Automatic-Module-Name", "legacyconnector.shared.apis");
            writeJar(sharedJar, m, sharedClasses);
        }
        return new Outcome(out, sharedJar, shared);
    }

    /**
     * The authoritative copy of a shared package: the jar of the mod that owns the API (it also has non-shared code
     * under the same root, e.g. {@code forestry/core} next to {@code forestry/api}), else the largest copy. Shaded
     * copies in other mods are often outdated versions of the API.
     */
    private static Path bestCopy(String pkg, List<Path> candidates, Map<Path, Map<String, Integer>> packageSizes) {
        String root = pkg.indexOf('/') > 0 ? pkg.substring(0, pkg.indexOf('/') + 1) : pkg + "/";
        Path owner = null;
        int ownerClasses = 0;
        for (Path jar : candidates) {
            int own = 0;
            for (var e : packageSizes.get(jar).entrySet()) {
                if (e.getKey().startsWith(root) && !e.getKey().contains("/api")) own += e.getValue();
            }
            if (own > ownerClasses) {
                owner = jar;
                ownerClasses = own;
            }
        }
        if (owner != null) return owner;
        return candidates.stream().max((a, b) -> Integer.compare(packageSizes.get(a).get(pkg), packageSizes.get(b).get(pkg))).orElseThrow();
    }

    /**
     * The jar without the classes of the packages: a byte copy of the file whose entries are then deleted in place, which keeps every
     * remaining entry as it is (no inflating and deflating of the 100 MB of textures and sounds a big mod jar holds); streaming a new jar
     * is the fallback.
     */
    private static void copyWithout(Path jar, ZipFile zip, Path target, Set<String> packages) throws IOException {
        List<String> doomed = new ArrayList<>();
        Enumeration<? extends ZipEntry> names = zip.entries();
        while (names.hasMoreElements()) {
            String n = names.nextElement().getName();
            int slash = n.lastIndexOf('/');
            if (n.endsWith(".class") && slash > 0 && packages.contains(n.substring(0, slash))) doomed.add(n);
        }
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.copy(jar, tmp, StandardCopyOption.REPLACE_EXISTING);
            try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(tmp)) {
                for (String n : doomed) Files.deleteIfExists(fs.getPath("/" + n));
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tmp);
            streamWithout(zip, target, packages);
        }
    }

    private static void streamWithout(ZipFile zip, Path target, Set<String> packages) throws IOException {
        Manifest manifest = null;
        ZipEntry manifestEntry = zip.getEntry(JarFile.MANIFEST_NAME);
        if (manifestEntry != null) {
            try (InputStream in = zip.getInputStream(manifestEntry)) {
                manifest = new Manifest(in);
            }
        }
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (OutputStream os = new java.io.BufferedOutputStream(Files.newOutputStream(tmp), 1 << 16);
                JarOutputStream jos = new JarOutputStream(os, manifest == null ? new Manifest() : manifest)) {
            jos.setLevel(java.util.zip.Deflater.BEST_SPEED); // a cache file: size does not matter
            Set<String> seen = new HashSet<>();
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String n = e.getName();
                if (e.isDirectory() || n.equals(JarFile.MANIFEST_NAME) || !seen.add(n)) continue;
                int slash = n.lastIndexOf('/');
                if (n.endsWith(".class") && slash > 0 && packages.contains(n.substring(0, slash))) continue;
                ZipEntry z = new ZipEntry(n);
                z.setTime(0);
                jos.putNextEntry(z);
                try (InputStream in = zip.getInputStream(e)) {
                    in.transferTo(jos);
                }
                jos.closeEntry();
            }
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static void writeJar(Path target, Manifest manifest, Map<String, byte[]> entries) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (OutputStream os = new java.io.BufferedOutputStream(Files.newOutputStream(tmp), 1 << 16); JarOutputStream jos = new JarOutputStream(os, manifest)) {
            jos.setLevel(java.util.zip.Deflater.BEST_SPEED); // a cache file read once per class: size does not matter
            for (var e : entries.entrySet()) {
                ZipEntry z = new ZipEntry(e.getKey());
                z.setTime(0);
                jos.putNextEntry(z);
                jos.write(e.getValue());
                jos.closeEntry();
            }
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String stripExt(String s) {
        return s.endsWith(".jar") ? s.substring(0, s.length() - 4) : s;
    }

    static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
