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
    private static final Set<String> DROP = Set.of("javax/activation",
            // packages of the JDK's own java.xml module (Mo' Creatures 1.6.4 bundles javax.xml.namespace.QName): a second exporter fails module resolution
            "javax/xml/namespace", "javax/xml/parsers", "javax/xml/stream", "javax/xml/transform", "javax/xml/xpath", "javax/xml/datatype",
            "org/w3c/dom", "org/xml/sax");

    private static final java.util.regex.Pattern JDK_XML = java.util.regex.Pattern.compile("javax/xml/(catalog|crypto|datatype|namespace|parsers|stream|transform|validation|xpath)(/.*)?");

    /** Also the packages of the JDK's own java.xml module ({@code javax/xml/namespace} bundled by MoCreatures splits it against enderio's copy). */
    private static boolean dropped(String pkg) {
        return droppedWith(pkg, hostPackages());
    }

    /**
     * The game's own copy of Nashorn ({@code org.openjdk.nashorn}, NeoForge's coremod engine): HuajiAge bundles another one, and the
     * Scala/Akka library reads both modules ("Modules org.openjdk.nashorn and huajiage export package ... to module akka.actor").
     */
    private static final java.util.regex.Pattern HOST_PREFIXES = java.util.regex.Pattern.compile("org/openjdk/nashorn(/.*)?|jdk/internal/dynalink(/.*)?");

    /** Packages of every named module in the layers above this one (boot layer, the game's library layers): a legacy jar's copy would be a second exporter. */
    private static volatile Set<String> hostPackages;

    private static Set<String> hostPackages() {
        Set<String> r = hostPackages;
        if (r != null) return r;
        Set<String> out = new HashSet<>();
        try {
            Set<ModuleLayer> seen = new HashSet<>();
            java.util.ArrayDeque<ModuleLayer> todo = new java.util.ArrayDeque<>();
            ModuleLayer own = SplitPackageResolver.class.getModule().getLayer();
            if (own != null) todo.addAll(own.parents());
            todo.add(ModuleLayer.boot());
            while (!todo.isEmpty()) {
                ModuleLayer l = todo.poll();
                if (!seen.add(l)) continue;
                for (Module m : l.modules()) {
                    for (String p : m.getPackages()) out.add(p.replace('.', '/'));
                }
                todo.addAll(l.parents());
            }
        } catch (RuntimeException e) {
            // best effort: the static list still applies
        }
        return hostPackages = out;
    }

    /** Visible for tests: the decision for a package given the host's packages. */
    static boolean droppedWith(String pkg, Set<String> host) {
        if (host.contains(pkg) || HOST_PREFIXES.matcher(pkg).matches()) return true;
        return DROP.contains(pkg) || JDK_XML.matcher(pkg).matches() || pkg.equals("org/w3c/dom") || pkg.startsWith("org/w3c/dom/")
                || pkg.equals("org/xml/sax") || pkg.startsWith("org/xml/sax/");
    }

    record Outcome(List<Path> jars, Path sharedLibrary, Set<String> sharedPackages) {}

    static Outcome resolve(List<Path> jars, Path cacheDir) throws IOException {
        return resolve(jars, cacheDir, Set.of());
    }

    /**
     * @param ownerKeeping jars of a version whose mods look their own jar up by file (Forestry lists its jar for {@code forestry/plugins/Plugin*.class}): a
     *     package all of whose copies are in such jars stays in the jar of the mod that owns its root, which also receives the classes only the other copies had
     */
    static Outcome resolve(List<Path> jars, Path cacheDir, Set<Path> ownerKeeping) throws IOException {
        Map<String, List<Path>> owners = new TreeMap<>();
        Map<Path, Map<String, Integer>> packageSizes = new HashMap<>();
        Map<Path, Map<String, Set<String>>> classNames = new HashMap<>();
        for (Path jar : jars) {
            Map<String, Integer> sizes = new HashMap<>();
            Map<String, Set<String>> names = new HashMap<>();
            classNames.put(jar, names);
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                Enumeration<? extends ZipEntry> en = zip.entries();
                while (en.hasMoreElements()) {
                    String n = en.nextElement().getName();
                    if (!n.endsWith(".class") || n.startsWith("META-INF/")) continue;
                    int slash = n.lastIndexOf('/');
                    if (slash < 0) continue; // default package classes can never be exported anyway
                    sizes.merge(n.substring(0, slash), 1, Integer::sum);
                    names.computeIfAbsent(n.substring(0, slash), k -> new HashSet<>()).add(n.substring(slash + 1));
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

        // A package all of whose copies are in owner-keeping jars stays in the jar of the mod that owns its root (the mod whose own code lives under the same
        // top-level folder); the other jars lose their copy and the owner's jar is given the classes only the other copies had. Moving it to the shared library
        // would take it out of the owner's jar, and mods that list their own jar's entries (Forestry finds its plugins by scanning the jar for
        // forestry/plugins/Plugin*.class) would see none.
        Map<String, Path> keptBy = new TreeMap<>();
        Map<Path, Map<Path, List<String>>> extras = new HashMap<>(); // owner jar -> donor jar -> class entries to add
        for (String pkg : shared) {
            List<Path> candidates = owners.get(pkg);
            if (candidates == null || candidates.size() < 2 || dropped(pkg) || !ownerKeeping.containsAll(candidates)) continue;
            Path owner = ownerOf(candidates, packageSizes, pkg);
            if (owner == null) continue;
            keptBy.put(pkg, owner);
            Set<String> own = classNames.get(owner).get(pkg);
            for (Path other : candidates) {
                if (other.equals(owner)) continue;
                for (String name : classNames.get(other).get(pkg)) {
                    if (!own.contains(name)) extras.computeIfAbsent(owner, k -> new HashMap<>()).computeIfAbsent(other, k -> new ArrayList<>()).add(pkg + "/" + name);
                }
            }
        }

        // Content-address everything so repeated launches reuse the same files.
        MessageDigest digest = sha256();
        digest.update("services-v1".getBytes()); // stripped jars drop service providers that moved (format change)
        for (Path jar : jars) digest.update(jar.getFileName().toString().getBytes());
        for (String pkg : shared) digest.update(pkg.getBytes());
        for (var e : keptBy.entrySet()) digest.update((e.getKey() + "@" + e.getValue().getFileName()).getBytes());
        String key = HexFormat.of().formatHex(digest.digest()).substring(0, 16);

        Path sharedJar = cacheDir.resolve("shared-apis-" + key + ".jar");
        List<Path> out = new ArrayList<>();
        List<Path> toStrip = new ArrayList<>();
        Map<Path, Path> strippedOf = new HashMap<>();
        for (Path jar : jars) {
            Set<String> mine = strippedPackages(jar, packageSizes, shared, keptBy);
            if (mine.isEmpty() && !extras.containsKey(jar)) {
                out.add(jar);
                continue;
            }
            Path stripped = cacheDir.resolve(stripExt(jar.getFileName().toString()) + "-split-" + key + ".jar");
            out.add(stripped);
            strippedOf.put(jar, stripped);
            if (!Files.exists(stripped)) toStrip.add(jar);
        }

        // Warm launch: the merged library and every stripped jar exist, so no class has to be read at all (the key names every input).
        Set<String> sharedOnly = new TreeSet<>(shared);
        sharedOnly.removeAll(keptBy.keySet());
        Path sharedLibrary = sharedOnly.isEmpty() ? null : sharedJar;
        boolean needShared = sharedLibrary != null && !Files.exists(sharedJar);
        if (!needShared && toStrip.isEmpty()) return new Outcome(out, sharedLibrary, sharedOnly);

        // Cold: every jar is independent. The stripped copies and the shared classes are collected in parallel and merged in jar order.
        Set<Path> toRead = new HashSet<>(toStrip);
        if (needShared) toRead.addAll(strippedOf.keySet());
        Map<Path, Map<String, byte[]>> collected = new java.util.concurrent.ConcurrentHashMap<>();
        Map<Path, IOException> failure = new java.util.concurrent.ConcurrentHashMap<>();
        jars.parallelStream().filter(toRead::contains).forEach(jar -> {
            try {
                Set<String> mine = strippedPackages(jar, packageSizes, shared, keptBy);
                Set<String> sharedMine = new HashSet<>(mine);
                sharedMine.removeAll(keptBy.keySet());
                try (ZipFile zip = new ZipFile(jar.toFile())) {
                    if (needShared) {
                        Map<String, byte[]> mineClasses = new TreeMap<>();
                        Enumeration<? extends ZipEntry> en = zip.entries();
                        while (en.hasMoreElements()) {
                            ZipEntry e = en.nextElement();
                            String n = e.getName();
                            int slash = n.lastIndexOf('/');
                            if (!n.endsWith(".class") || slash < 0 || !sharedMine.contains(n.substring(0, slash)) || dropped(n.substring(0, slash))) continue;
                            try (InputStream in = zip.getInputStream(e)) {
                                mineClasses.put(n, in.readAllBytes());
                            }
                        }
                        collected.put(jar, mineClasses);
                    }
                    if (!Files.exists(strippedOf.get(jar))) {
                        copyWithout(jar, zip, strippedOf.get(jar), mine);
                        if (extras.containsKey(jar)) addExtras(strippedOf.get(jar), extras.get(jar));
                    }
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
                Set<String> mine = strippedPackages(jar, packageSizes, shared, keptBy);
                mine.removeAll(keptBy.keySet());
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
        return new Outcome(out, sharedLibrary, sharedOnly);
    }

    /** Adds the class entries of the donor jars to the jar (a copy made by {@link #copyWithout}). */
    private static void addExtras(Path target, Map<Path, List<String>> donors) throws IOException {
        Map<String, byte[]> add = new TreeMap<>();
        for (var d : donors.entrySet()) {
            try (ZipFile zip = new ZipFile(d.getKey().toFile())) {
                for (String n : d.getValue()) {
                    ZipEntry e = zip.getEntry(n);
                    if (e == null) continue;
                    try (InputStream in = zip.getInputStream(e)) {
                        add.putIfAbsent(n, in.readAllBytes());
                    }
                }
            }
        }
        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(target)) {
            for (var e : add.entrySet()) {
                Path entry = fs.getPath("/" + e.getKey());
                if (entry.getParent() != null) Files.createDirectories(entry.getParent());
                Files.write(entry, e.getValue());
            }
        }
    }

    /** The packages of {@code jar} that are shared and not kept by it: the ones its translated copy loses. */
    private static Set<String> strippedPackages(Path jar, Map<Path, Map<String, Integer>> packageSizes, Set<String> shared, Map<String, Path> keptBy) {
        Set<String> mine = new HashSet<>(packageSizes.get(jar).keySet());
        mine.retainAll(shared);
        mine.removeIf(pkg -> jar.equals(keptBy.get(pkg)));
        return mine;
    }

    /** The jar that owns the root of the package (most classes outside {@code api} packages under the same top-level folder), or null when none does. */
    private static Path ownerOf(List<Path> candidates, Map<Path, Map<String, Integer>> packageSizes, String pkg) {
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
        return owner;
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
        List<String> services = new ArrayList<>();
        Enumeration<? extends ZipEntry> names = zip.entries();
        while (names.hasMoreElements()) {
            String n = names.nextElement().getName();
            int slash = n.lastIndexOf('/');
            if (n.endsWith(".class") && slash > 0 && packages.contains(n.substring(0, slash))) doomed.add(n);
            else if (n.startsWith(SERVICES) && !n.endsWith("/")) services.add(n);
        }
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.copy(jar, tmp, StandardCopyOption.REPLACE_EXISTING);
            try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(tmp)) {
                for (String n : doomed) Files.deleteIfExists(fs.getPath("/" + n));
                for (String n : services) {
                    Path file = fs.getPath("/" + n);
                    byte[] kept = withoutProviders(Files.readAllBytes(file), packages);
                    if (kept == null) Files.delete(file);
                    else if (kept.length != Files.size(file)) Files.write(file, kept);
                }
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tmp);
            streamWithout(zip, target, packages);
        }
    }

    private static final String SERVICES = "META-INF/services/";

    /**
     * A service file without the providers whose classes moved to the shared library (a module's provider has to be in the module itself, so a
     * stripped jar that kept the line fails to resolve: "Provider class ... not in JAR file"); null when no provider is left.
     */
    static byte[] withoutProviders(byte[] file, Set<String> packages) {
        String text = new String(file, java.nio.charset.StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder();
        boolean any = false;
        for (String line : text.split("\\R", -1)) {
            String name = line;
            int hash = name.indexOf('#');
            if (hash >= 0) name = name.substring(0, hash);
            name = name.trim();
            if (!name.isEmpty()) {
                int dot = name.lastIndexOf('.');
                if (dot > 0 && packages.contains(name.substring(0, dot).replace('.', '/'))) continue;
                any = true;
            }
            out.append(line).append(System.lineSeparator());
        }
        if (!any) return null;
        return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
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
                if (n.equals(JarFile.MANIFEST_NAME) || !seen.add(n)) continue;
                if (e.isDirectory()) { // kept: a mod mounting a folder of its own jar looks the folder entry up
                    ZipEntry d = new ZipEntry(n);
                    d.setTime(0);
                    jos.putNextEntry(d);
                    jos.closeEntry();
                    continue;
                }
                int slash = n.lastIndexOf('/');
                if (n.endsWith(".class") && slash > 0 && packages.contains(n.substring(0, slash))) continue;
                byte[] replaced = null;
                if (n.startsWith(SERVICES)) {
                    try (InputStream in = zip.getInputStream(e)) {
                        replaced = withoutProviders(in.readAllBytes(), packages);
                    }
                    if (replaced == null) continue;
                }
                ZipEntry z = new ZipEntry(n);
                z.setTime(0);
                jos.putNextEntry(z);
                if (replaced != null) {
                    jos.write(replaced);
                } else {
                    try (InputStream in = zip.getInputStream(e)) {
                        in.transferTo(jos);
                    }
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
