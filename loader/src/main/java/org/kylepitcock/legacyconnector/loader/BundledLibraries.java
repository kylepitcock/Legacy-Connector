package org.kylepitcock.legacyconnector.loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The libraries Minecraft 1.7.10 / 1.12.2 shipped that legacy mods use directly (Scala 2.11, trove, vecmath, typesafe config, akka,
 * javax.activation). They are NOT jar-in-jar entries of the runtime: a jar-in-jar library always enters the game module layer, and
 * a modern mod bundling the same packages (ScalableCatsForce ships Scala 2.13) then breaks the whole launch with a
 * {@code ResolutionException: Modules X and Y export package ... to module Z}. Instead they are plain resources of the loader jar
 * ({@code META-INF/legacyconnector/libs/}); this class hands each library group to FML only when
 * <ol>
 *   <li>a legacy jar that will be loaded references it (or the runtime needs it), and</li>
 *   <li>no other jar of the mods folder (nor a jar nested in one) already contains a package of it.</li>
 * </ol>
 * A clash with a compatible library (another trove, activation, config) simply means the pack's copy serves the legacy mods. A clash
 * with Scala is different: legacy mods are compiled for Scala 2.11, whose binary interface differs from 2.12/2.13, so the mods that
 * use Scala cannot run next to another Scala and are skipped with a clear message (relocating Scala into a private namespace would
 * need the pickled signatures, the reflection universe and string class names rewritten as well).
 */
final class BundledLibraries {
    private static final Logger LOG = LoggerFactory.getLogger("Legacy Connector");
    static final String RESOURCE_DIR = "/META-INF/legacyconnector/libs/";
    static final String INDEX = RESOURCE_DIR + "index.txt";
    /** Development / testing: a folder with the same layout (index.txt + jars) instead of the embedded resources. */
    static final String DIR_PROPERTY = "legacyconnector.libsDir";

    /** A set of library jars that belong together. */
    private record Group(String id, String description, List<String> filePrefixes, List<String> usage, boolean always, boolean compatibleWithOthers) {}

    private static final List<Group> GROUPS = List.of(
            // Scala 2.10 is what Forge 1.6.4 / 1.5.2 mods (bdew's Gendustry and BD Lib) were compiled for; it is not binary compatible with 2.11 (trait
            // implementation classes differ), so a jar of those versions that uses Scala gets this group instead of "scala" (see usage mapping below).
            // Listed first: the "scala" prefixes would match its files. No class pattern: it is only ever assigned, never scanned for.
            new Group("scala210", "Scala 2.10", List.of("scala-library-2.10", "scala-reflect-2.10"), List.of("scala210-is-assigned-not-scanned/"), false, false),
            new Group("scala", "Scala 2.11 (+ akka)", List.of("scala-", "akka-actor"), List.of("scala/", "akka/"), false, false),
            new Group("config", "typesafe config", List.of("config-"), List.of("com/typesafe/config/"), false, true),
            new Group("trove", "trove4j", List.of("trove4j-"), List.of("gnu/trove/"), false, true),
            // The 1.12.2 shim itself is written against vecmath.
            new Group("vecmath", "vecmath", List.of("vecmath-"), List.of("javax/vecmath/"), true, true),
            new Group("activation", "javax.activation", List.of("jakarta.activation-"), List.of("javax/activation/"), false, true),
            new Group("lzma", "lzma (LZMA.LzmaInputStream, a Forge 1.7.10 library)", List.of("lzma-"), List.of("LZMA/"), false, true),
            new Group("argo", "argo JSON parser (a Minecraft 1.6.4 launcher library)", List.of("argo-"), List.of("argo/"), false, true));

    /** What was decided, for the log and the report. */
    record Result(Map<LegacyJar, String> unsupported, List<String> notes) {
        static final Result NONE = new Result(Map.of(), List.of());
    }

    private BundledLibraries() {}

    private static List<String> index() throws IOException {
        String dir = System.getProperty(DIR_PROPERTY);
        byte[] data;
        if (dir != null) {
            Path p = Path.of(dir).resolve("index.txt");
            if (!Files.exists(p)) return List.of();
            data = Files.readAllBytes(p);
        } else {
            try (InputStream in = BundledLibraries.class.getResourceAsStream(INDEX)) {
                if (in == null) return List.of(); // development: the libraries are on the class path
                data = in.readAllBytes();
            }
        }
        List<String> out = new ArrayList<>();
        for (String line : new String(data, StandardCharsets.UTF_8).split("\\R")) if (!line.isBlank()) out.add(line.trim());
        return out;
    }

    private static byte[] read(String file) throws IOException {
        String dir = System.getProperty(DIR_PROPERTY);
        if (dir != null) return Files.readAllBytes(Path.of(dir).resolve(file));
        try (InputStream in = BundledLibraries.class.getResourceAsStream(RESOURCE_DIR + file)) {
            if (in == null) throw new IOException("missing embedded library " + file);
            return in.readAllBytes();
        }
    }

    private static Group groupOf(String file) {
        for (Group g : GROUPS) for (String p : g.filePrefixes) if (file.startsWith(p)) return g;
        return null;
    }

    /**
     * Decides and registers the libraries.
     *
     * @param legacy the legacy jars that will be translated (skipped ones excluded)
     * @param scanDirs the folders whose jars may already provide a library
     */
    static Result provide(IDiscoveryPipeline pipeline, Path cacheDir, List<LegacyJar> legacy, List<Path> scanDirs, byte[] loaderDigest, Set<Path> keep) {
        try {
            List<String> files = index();
            if (files.isEmpty()) return Result.NONE;

            Map<Group, List<String>> byGroup = new LinkedHashMap<>();
            for (String f : files) {
                Group g = groupOf(f);
                if (g == null) {
                    LOG.warn("Legacy Connector: bundled library {} belongs to no group; ignored", f);
                    continue;
                }
                byGroup.computeIfAbsent(g, k -> new ArrayList<>()).add(f);
            }

            // Which groups does each legacy jar reference?
            Map<LegacyJar, Set<String>> usage = new LinkedHashMap<>();
            Properties cache = loadUsageCache(cacheDir);
            boolean cacheChanged = false;
            for (LegacyJar jar : legacy) {
                Path p = jar.existing();
                String key = p.toAbsolutePath() + "|" + safeSize(p) + "|" + safeMtime(p) + "|g" + GROUPS.size();
                String cached = cache.getProperty(key);
                Set<String> used;
                if (cached != null) {
                    used = new TreeSet<>();
                    for (String s : cached.split(",")) if (!s.isBlank()) used.add(s);
                } else {
                    used = scanUsage(p);
                    cache.setProperty(key, String.join(",", used));
                    cacheChanged = true;
                }
                // 1.6.4 / 1.5.2 jars link against Scala 2.10
                used = new TreeSet<>(used);
                if (jar.version.numericIds() && used.remove("scala")) used.add("scala210");
                usage.put(jar, used);
            }
            if (cacheChanged) saveUsageCache(cacheDir, cache, legacy);

            Set<String> needed = new TreeSet<>();
            for (Group g : byGroup.keySet()) if (g.always) needed.add(g.id);
            usage.values().forEach(needed::addAll);
            if (needed.isEmpty()) {
                LOG.info("Legacy Connector: no legacy mod uses the bundled libraries; none are loaded");
                return new Result(Map.of(), List.of("No legacy mod uses the bundled libraries (Scala, trove, ...); none were loaded."));
            }

            // What the rest of the pack already provides.
            Set<Path> legacyPaths = new HashSet<>();
            for (LegacyJar j : legacy) {
                legacyPaths.add(j.path.toAbsolutePath().normalize());
                legacyPaths.add(j.existing().toAbsolutePath().normalize());
            }
            Map<String, String> providers = scanProviders(scanDirs, legacyPaths);

            Map<LegacyJar, String> unsupported = new LinkedHashMap<>();
            List<String> notes = new ArrayList<>();
            Path libDir = cacheDir.resolve("libs-" + java.util.HexFormat.of().formatHex(loaderDigest.length > 8 ? java.util.Arrays.copyOf(loaderDigest, 8) : loaderDigest));
            Set<String> provided = new HashSet<>();
            for (Group g : byGroup.keySet()) {
                // akka reads typesafe config: it comes along with Scala
                if (g.id.equals("config") && provided.contains("scala")) needed.add("config");
                if (!needed.contains(g.id)) {
                    notes.add(g.description + ": not loaded (no legacy mod uses it)");
                    continue;
                }
                if (g.id.equals("scala210") && needed.contains("scala")) {
                    String reason = "needs Scala 2.10, but other legacy mods of this pack need Scala 2.11: two Scala versions cannot share the game";
                    notes.add(g.description + ": NOT loaded, " + reason + "; the " + "1.6.4 / 1.5.2 mods that need it are skipped");
                    LOG.warn("Legacy Connector: {} conflicts with Scala 2.11 needed by other legacy mods; skipped: {}", g.description,
                            usage.entrySet().stream().filter(e -> e.getValue().contains(g.id)).map(e -> e.getKey().fileName()).toList());
                    for (var e : usage.entrySet()) if (e.getValue().contains(g.id)) unsupported.put(e.getKey(), reason);
                    continue;
                }
                // packages of all files of the group, and who else provides them
                Map<String, Set<String>> clash = new LinkedHashMap<>(); // provider -> packages
                for (String f : byGroup.get(g)) {
                    for (String pkg : packages(read(f))) {
                        String other = providers.get(pkg);
                        if (other != null) clash.computeIfAbsent(other, k -> new TreeSet<>()).add(pkg.replace('/', '.'));
                    }
                }
                if (clash.isEmpty()) {
                    Files.createDirectories(libDir);
                    deleteStaleLibDirs(cacheDir, libDir);
                    for (String f : byGroup.get(g)) {
                        Path target = libDir.resolve(f);
                        if (!Files.exists(target)) writeLibrary(read(f), target);
                        keep.add(target);
                        pipeline.addPath(target, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
                    }
                    provided.add(g.id);
                    notes.add(g.description + ": loaded from Legacy Connector (" + String.join(", ", byGroup.get(g)) + ")");
                    continue;
                }
                String who = String.join(", ", clash.keySet());
                if (g.compatibleWithOthers) {
                    notes.add(g.description + ": not loaded, " + who + " already provides it and serves the legacy mods");
                    LOG.info("Legacy Connector: {} is provided by {}; Legacy Connector's copy is not loaded", g.description, who);
                    continue;
                }
                // Scala: usable only if the other copy is the same Scala version family.
                String version = null;
                for (String provider : clash.keySet()) version = providerScalaVersion(provider, scanDirs);
                String wanted = g.id.equals("scala210") ? "2.10" : "2.11";
                if (version != null && version.startsWith(wanted)) {
                    notes.add(g.description + ": not loaded, " + who + " provides Scala " + version + " which serves the legacy mods");
                    continue;
                }
                String reason = "needs Scala " + wanted + " (or akka), but this pack already contains " + (version == null ? "another Scala" : "Scala " + version)
                        + " (" + who + "): two Scala versions cannot share the game, and Scala 2.11 code does not run on it";
                notes.add(g.description + ": NOT loaded because of a conflict with " + who + " (" + clash.values().stream().mapToInt(Set::size).sum()
                        + " shared packages); the legacy mods that need it are skipped");
                LOG.warn("Legacy Connector: {} conflicts with {}; legacy Scala mods cannot be loaded in this pack: {}", g.description, who,
                        usage.entrySet().stream().filter(e -> e.getValue().contains(g.id)).map(e -> e.getKey().fileName()).toList());
                for (var e : usage.entrySet()) if (e.getValue().contains(g.id)) unsupported.put(e.getKey(), reason);
            }
            return new Result(unsupported, notes);
        } catch (IOException | RuntimeException e) {
            LOG.error("Legacy Connector: could not set up the bundled libraries", e);
            return Result.NONE;
        }
    }

    /** Library folders of older loader builds. */
    private static void deleteStaleLibDirs(Path cacheDir, Path current) {
        try (var dirs = Files.list(cacheDir)) {
            for (Path d : dirs.filter(f -> Files.isDirectory(f) && f.getFileName().toString().startsWith("libs-") && !f.equals(current)).toList()) {
                // only the library jars this loader wrote there (flat folder), then the folder if it is empty
                try (var files = Files.list(d)) {
                    for (Path f : files.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().matches(".+\\.jar(\\.tmp)?")).toList()) {
                        Files.deleteIfExists(f);
                    }
                }
                try {
                    Files.deleteIfExists(d);
                } catch (java.nio.file.DirectoryNotEmptyException ignored) {
                    // something else lives there: left alone
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.debug("Legacy Connector: could not remove old library folders", e);
        }
    }

    /** The jar of this group is written with the marker FML needs to put it into the game layer; signatures are dropped. */
    private static void writeLibrary(byte[] jar, Path target) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(jar)); OutputStream os = Files.newOutputStream(tmp)) {
            Manifest m = new Manifest();
            Map<String, byte[]> entries = new LinkedHashMap<>();
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String n = e.getName();
                if (n.equals("META-INF/MANIFEST.MF")) {
                    m = new Manifest(new ByteArrayInputStream(in.readAllBytes()));
                    continue;
                }
                if (n.startsWith("META-INF/") && (n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA") || n.endsWith(".EC"))) continue;
                entries.put(n, in.readAllBytes());
            }
            if (m.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION) == null) m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            m.getMainAttributes().putValue("FMLModType", "GAMELIBRARY");
            m.getEntries().clear();
            try (JarOutputStream jos = new JarOutputStream(os, m)) {
                for (var en : entries.entrySet()) {
                    ZipEntry z = new ZipEntry(en.getKey());
                    z.setTime(0);
                    jos.putNextEntry(z);
                    jos.write(en.getValue());
                    jos.closeEntry();
                }
            }
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static Set<String> packages(byte[] jar) throws IOException {
        Set<String> out = new TreeSet<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(jar))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) addPackage(out, e.getName());
        }
        return out;
    }

    private static void addPackage(Set<String> out, String entry) {
        if (!entry.endsWith(".class") || entry.startsWith("META-INF/")) return;
        int slash = entry.lastIndexOf('/');
        if (slash > 0) out.add(entry.substring(0, slash));
    }

    /** package (slash form) to the name of a jar that contains classes of it; nested jar-in-jar libraries are looked into too. */
    static Map<String, String> scanProviders(List<Path> dirs, Set<Path> excluded) {
        Map<String, String> out = new HashMap<>();
        for (Path dir : dirs) {
            if (!Files.isDirectory(dir)) continue;
            try (var files = Files.list(dir)) {
                for (Path p : files.filter(f -> f.getFileName().toString().toLowerCase().endsWith(".jar")).sorted().toList()) {
                    String name = p.getFileName().toString();
                    if (name.toLowerCase().startsWith("legacyconnector") || excluded.contains(p.toAbsolutePath().normalize())) continue;
                    try (ZipFile zip = new ZipFile(p.toFile())) {
                        Set<String> pk = new HashSet<>();
                        Enumeration<? extends ZipEntry> en = zip.entries();
                        while (en.hasMoreElements()) {
                            ZipEntry e = en.nextElement();
                            String n = e.getName();
                            if (n.startsWith("META-INF/jarjar/") && n.endsWith(".jar")) {
                                try (InputStream in = zip.getInputStream(e)) {
                                    for (String s : packages(in.readAllBytes())) out.putIfAbsent(s, name + " (nested " + n.substring(n.lastIndexOf('/') + 1) + ")");
                                }
                            } else {
                                addPackage(pk, n);
                            }
                        }
                        for (String s : pk) out.putIfAbsent(s, name);
                    } catch (IOException | RuntimeException e) {
                        LOG.debug("Legacy Connector: could not scan {} for libraries", p, e);
                    }
                }
            } catch (IOException e) {
                LOG.debug("Legacy Connector: could not list {}", dir, e);
            }
        }
        return out;
    }

    /** The Scala version of the jar named {@code provider} (from its library.properties), or null. */
    private static String providerScalaVersion(String provider, List<Path> dirs) {
        String file = provider.contains(" (") ? provider.substring(0, provider.indexOf(" (")) : provider;
        for (Path dir : dirs) {
            Path p = dir.resolve(file);
            if (!Files.isRegularFile(p)) continue;
            try (ZipFile zip = new ZipFile(p.toFile())) {
                ZipEntry e = zip.getEntry("library.properties");
                if (e == null) return null;
                Properties props = new Properties();
                try (InputStream in = zip.getInputStream(e)) {
                    props.load(in);
                }
                return props.getProperty("version.number");
            } catch (IOException | RuntimeException ex) {
                return null;
            }
        }
        return null;
    }

    // ---- which libraries does a legacy jar reference? ----

    private static Set<String> scanUsage(Path jar) {
        Set<String> out = new TreeSet<>();
        List<Group> open = new ArrayList<>(GROUPS);
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements() && !open.isEmpty()) {
                ZipEntry e = en.nextElement();
                String n = e.getName();
                if (!n.endsWith(".class") || n.startsWith("META-INF/")) continue;
                byte[] data;
                try (InputStream in = zip.getInputStream(e)) {
                    data = in.readAllBytes();
                }
                for (var it = open.iterator(); it.hasNext(); ) {
                    Group g = it.next();
                    for (String pattern : g.usage) {
                        if (contains(data, pattern.getBytes(StandardCharsets.US_ASCII))) {
                            out.add(g.id);
                            it.remove();
                            break;
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            // unreadable: assume it may use everything rather than break it
            for (Group g : GROUPS) out.add(g.id);
        }
        return out;
    }

    private static boolean contains(byte[] hay, byte[] needle) {
        int last = hay.length - needle.length;
        byte first = needle[0];
        outer:
        for (int i = 0; i <= last; i++) {
            if (hay[i] != first) continue;
            for (int j = 1; j < needle.length; j++) if (hay[i + j] != needle[j]) continue outer;
            return true;
        }
        return false;
    }

    private static long safeSize(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return -1;
        }
    }

    private static long safeMtime(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }

    private static Properties loadUsageCache(Path cacheDir) {
        Properties p = new Properties();
        Path f = cacheDir.resolve("library-usage.properties");
        if (Files.exists(f)) {
            try (InputStream in = Files.newInputStream(f)) {
                p.load(in);
            } catch (IOException | RuntimeException e) {
                LOG.debug("Legacy Connector: could not read the library usage cache", e);
            }
        }
        return p;
    }

    private static void saveUsageCache(Path cacheDir, Properties all, List<LegacyJar> current) {
        // only entries of the current jars, so the file does not grow
        Properties out = new Properties();
        for (LegacyJar jar : current) {
            Path p = jar.existing();
            String key = p.toAbsolutePath() + "|" + safeSize(p) + "|" + safeMtime(p) + "|g" + GROUPS.size();
            String v = all.getProperty(key);
            if (v != null) out.setProperty(key, v);
        }
        try (OutputStream os = Files.newOutputStream(cacheDir.resolve("library-usage.properties"))) {
            out.store(os, "which bundled libraries each legacy jar references");
        } catch (IOException e) {
            LOG.debug("Legacy Connector: could not write the library usage cache", e);
        }
    }
}
