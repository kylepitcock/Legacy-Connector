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
            if (list.size() > 1) shared.add(pkg);
        });
        if (shared.isEmpty()) return new Outcome(jars, null, shared);

        // Content-address everything so repeated launches reuse the same files.
        MessageDigest digest = sha256();
        for (Path jar : jars) digest.update(jar.getFileName().toString().getBytes());
        for (String pkg : shared) digest.update(pkg.getBytes());
        String key = HexFormat.of().formatHex(digest.digest()).substring(0, 16);

        Path sharedJar = cacheDir.resolve("shared-apis-" + key + ".jar");
        List<Path> out = new ArrayList<>();
        Map<String, byte[]> sharedClasses = new LinkedHashMap<>();

        for (Path jar : jars) {
            Set<String> mine = new HashSet<>(packageSizes.get(jar).keySet());
            mine.retainAll(shared);
            if (mine.isEmpty()) {
                out.add(jar);
                continue;
            }
            Path stripped = cacheDir.resolve(stripExt(jar.getFileName().toString()) + "-split-" + key + ".jar");
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                // Collect this jar's copies; a package's "best" copy is the jar with the most classes in it, and
                // classes only some copies have are unioned in.
                for (String pkg : mine) {
                    Path best = bestCopy(pkg, owners.get(pkg), packageSizes);
                    Enumeration<? extends ZipEntry> en = zip.entries();
                    while (en.hasMoreElements()) {
                        ZipEntry e = en.nextElement();
                        String n = e.getName();
                        if (!n.endsWith(".class") || n.lastIndexOf('/') < 0 || !n.substring(0, n.lastIndexOf('/')).equals(pkg)) continue;
                        try (InputStream in = zip.getInputStream(e)) {
                            byte[] data = in.readAllBytes();
                            if (best.equals(jar)) sharedClasses.put(n, data);
                            else sharedClasses.putIfAbsent(n, data);
                        }
                    }
                }
                if (!Files.exists(stripped)) copyWithout(zip, stripped, mine);
            }
            out.add(stripped);
        }

        if (!Files.exists(sharedJar)) {
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

    private static void copyWithout(ZipFile zip, Path target, Set<String> packages) throws IOException {
        Manifest manifest = null;
        Map<String, byte[]> entries = new LinkedHashMap<>();
        Enumeration<? extends ZipEntry> en = zip.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            if (e.isDirectory()) continue;
            String n = e.getName();
            try (InputStream in = zip.getInputStream(e)) {
                if (n.equals(JarFile.MANIFEST_NAME)) {
                    manifest = new Manifest(in);
                    continue;
                }
                int slash = n.lastIndexOf('/');
                if (n.endsWith(".class") && slash > 0 && packages.contains(n.substring(0, slash))) continue;
                entries.put(n, in.readAllBytes());
            }
        }
        writeJar(target, manifest == null ? new Manifest() : manifest, entries);
    }

    private static void writeJar(Path target, Manifest manifest, Map<String, byte[]> entries) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (OutputStream os = Files.newOutputStream(tmp); JarOutputStream jos = new JarOutputStream(os, manifest)) {
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
