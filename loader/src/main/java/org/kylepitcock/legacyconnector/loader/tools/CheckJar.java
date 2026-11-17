package org.kylepitcock.legacyconnector.loader.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.kylepitcock.legacyconnector.loader.LegacyDependencies;
import org.kylepitcock.legacyconnector.loader.LegacyModDecl;
import org.kylepitcock.legacyconnector.loader.LibraryBridges;
import org.kylepitcock.legacyconnector.loader.transform.LinkReport;
import org.kylepitcock.legacyconnector.loader.BetaJars;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.LegacyJarInspector;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.synth.SynthDemand;
import org.kylepitcock.legacyconnector.loader.synth.SynthJarBuilder;
import org.kylepitcock.legacyconnector.loader.transform.LegacyJarTransformer;

/**
 * Developer tool: {@code CheckJar <shim-index.txt> <legacy mod jar>...}. Translates jars offline, as one modset (so
 * mods that extend each other link), prints which references cannot be linked, then generates the synthesized
 * legacy API and verifies every generated class against the real shim.
 *
 * <p>With {@code -Dlegacyconnector.check.json=<file>} it also writes one machine-readable record per jar (the probe results
 * behind the compatibility site, see {@code tools/compat/probe.py}).</p>
 */
public final class CheckJar {
    private CheckJar() {}

    /** The archive itself, or the mod archives it wraps, or one jar per install root when its classes sit under several folders. */
    static List<Path> expand(Path archive, Path dir) {
        String low = archive.getFileName().toString().toLowerCase();
        if (!low.endsWith(".zip") && !low.endsWith(".jar")) return List.of(archive);
        try {
            List<Path> bundled = org.kylepitcock.legacyconnector.loader.LegacyModLocator.bundledMods(archive, dir);
            if (!bundled.isEmpty()) return bundled;
            java.util.Map<String, List<String>> byRoot = new java.util.LinkedHashMap<>();
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive.toFile())) {
                var en = zip.entries();
                while (en.hasMoreElements()) {
                    java.util.zip.ZipEntry e = en.nextElement();
                    if (e.isDirectory() || !e.getName().endsWith(".class") || e.getName().startsWith("__MACOSX/")) continue;
                    String cn;
                    try (InputStream in = zip.getInputStream(e)) {
                        cn = new org.objectweb.asm.ClassReader(in).getClassName();
                    }
                    String suffix = cn + ".class";
                    if (!e.getName().endsWith(suffix)) continue;
                    byRoot.computeIfAbsent(e.getName().substring(0, e.getName().length() - suffix.length()), k -> new ArrayList<>()).add(e.getName());
                }
                if (byRoot.isEmpty() || (byRoot.size() == 1 && byRoot.keySet().iterator().next().isEmpty())) return List.of(archive); // one root: a single folder is stripped (SDKs Mods: minecraft/mod_SdkFps.class)
                List<Path> out = new ArrayList<>();
                int n = 0;
                for (String r : byRoot.keySet()) {
                    // only roots that hold a ModLoader entry class count as installs; helper folders of a mod stay with their root
                    boolean mod = byRoot.get(r).stream().anyMatch(c -> c.substring(r.length()).matches("(.*/)?mod_[^/]*\\.class"));
                    if (!mod) continue;
                    Path target = dir.resolve(low.replaceAll("[.](zip|jar)$", "") + "-root" + (n++) + ".jar");
                    try (java.util.zip.ZipOutputStream zo = new java.util.zip.ZipOutputStream(Files.newOutputStream(target))) {
                        var en2 = zip.entries();
                        while (en2.hasMoreElements()) {
                            java.util.zip.ZipEntry e = en2.nextElement();
                            if (e.isDirectory() || !e.getName().startsWith(r) || e.getName().startsWith("__MACOSX/")) continue;
                            zo.putNextEntry(new java.util.zip.ZipEntry(e.getName().substring(r.length())));
                            try (InputStream in = zip.getInputStream(e)) {
                                in.transferTo(zo);
                            }
                            zo.closeEntry();
                        }
                    }
                    out.add(target);
                }
                return out.isEmpty() ? List.of(archive) : out;
            }
        } catch (IOException | RuntimeException e) {
            return List.of(archive);
        }
    }

    public static void main(String[] args) throws Exception {
        // the bridges that need no modern mod (CoFH RF) are always active in the game: the shim replaces the shaded copies of their API
        LegacyVersion.setBridgedRoots(org.kylepitcock.legacyconnector.loader.LibraryBridges.KNOWN.stream().filter(b -> b.modernModId() == null)
                .map(org.kylepitcock.legacyconnector.loader.LibraryBridges.Bridge::apiRoot).toList());
        ShimIndex index;
        try (InputStream in = Files.newInputStream(Path.of(args[0]))) {
            index = ShimIndex.read(in);
        }
        String jsonPath = System.getProperty("legacyconnector.check.json");
        JsonArray records = new JsonArray();
        Path out = Files.createTempDirectory("legacyconnector-check");
        LegacyJarTransformer transformer = new LegacyJarTransformer(index, CheckJar.class.getClassLoader());
        List<LegacyJar> jars = new ArrayList<>();
        // "@list.txt" names a file with one jar path per line (a few hundred jars overflow the Windows command line)
        List<String> jarArgs = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("@")) jarArgs.addAll(Files.readAllLines(Path.of(args[i].substring(1))).stream().filter(l -> !l.isBlank()).toList());
            else jarArgs.add(args[i]);
        }
        // an archive that only wraps mod archives (Smart Moving, MineFactory Reloaded) or holds several install roots (an SMP and an SSP folder) is several mods, as the game would load it / a user would install it
        Path expanded = Files.createTempDirectory("legacyconnector-check-expanded");
        List<String> flat = new ArrayList<>();
        for (String jarArg : jarArgs) {
            for (Path p : expand(Path.of(jarArg), expanded)) flat.add(p.toString());
        }
        for (String jarArg : flat) {
            Path jarPath = Path.of(jarArg);
            LegacyJar jar;
            try {
                jar = LegacyJarInspector.inspect(jarPath);
            } catch (IOException | RuntimeException e) {
                // a truncated or corrupt archive (ZipException: zip END header not found) must not stop the check of the other jars
                String reason = "unreadable archive, skipped: " + e;
                System.out.println(jarPath.getFileName() + ": " + reason);
                JsonObject r = new JsonObject();
                r.addProperty("jar", jarPath.getFileName().toString());
                r.addProperty("result", "skipped");
                r.addProperty("reason", reason);
                records.add(r);
                continue;
            }
            if (jar == null) {
                // a modern library of the set (Curios, GeckoLib...): legacy mods link against it, so the verifier needs its type relations
                readModernLibrary(jarPath);
                System.out.println(jarPath.getFileName() + ": not a legacy Forge mod");
                JsonObject r = new JsonObject();
                r.addProperty("jar", jarPath.getFileName().toString());
                r.addProperty("result", "not-legacy");
                records.add(r);
            } else jars.add(jar);
        }
        // b1.7.3 client + server builds of one mod: offline the client build is checked (as on a client)
        Map<LegacyJar, String> otherBuilds = BetaJars.selectBuilds(jars, "CLIENT");
        for (var e : otherBuilds.entrySet()) {
            System.out.println(e.getKey().fileName() + " [" + e.getKey().version.id() + "]: not loaded with this set: " + e.getValue());
            records.add(record(e.getKey(), "skipped", e.getValue(), null, 0));
        }
        jars.removeAll(otherBuilds.keySet());
        // Minecraft 1.2.5 client + server builds likewise
        Map<LegacyJar, String> otherBuilds125 = org.kylepitcock.legacyconnector.loader.Mc125Builds.select(jars, "CLIENT");
        for (var e : otherBuilds125.entrySet()) {
            System.out.println(e.getKey().fileName() + " [1.2.5]: not loaded with this set: " + e.getValue());
            records.add(record(e.getKey(), "skipped", e.getValue(), null, 0));
        }
        jars.removeAll(otherBuilds125.keySet());
        org.kylepitcock.legacyconnector.loader.transform.Notch125To147.registerSet(jars);
        // As in a pack with Jade, JEI, Curios and Patchouli installed: bridged libraries are not translated, their API is relocated into the shim
        // (-Pbridges=false checks the legacy jars as plain mods instead).
        if (!"false".equalsIgnoreCase(System.getProperty("legacyconnector.check.bridges"))) {
            boolean realCcl = jars.stream().anyMatch(j -> j.fileName().toLowerCase(java.util.Locale.ROOT).startsWith("codechickenlib"));
            List<LibraryBridges.Bridge> bridges = LibraryBridges.KNOWN.stream().filter(b -> !(realCcl && "codechicken/lib/".equals(b.apiRoot()))).toList();
            LegacyVersion.setBridgedRoots(bridges.stream().map(LibraryBridges.Bridge::apiRoot).toList());
            for (LegacyJar jar : List.copyOf(jars)) {
                LibraryBridges.Bridge replaced = LibraryBridges.replacing(jar, bridges);
                if (replaced != null) {
                    System.out.println(jar.fileName() + ": replaced by the " + replaced.configKey() + " bridge");
                    jars.remove(jar);
                }
            }
        }
        // required dependencies that are not in the set (a jar that FML would not have started)
        Map<LegacyJar, List<String>> missingDeps = new java.util.LinkedHashMap<>();
        {
            List<LibraryBridges.Bridge> inUse = "false".equalsIgnoreCase(System.getProperty("legacyconnector.check.bridges")) ? List.of()
                    : LibraryBridges.KNOWN.stream().filter(b -> !(jars.stream().anyMatch(j -> j.fileName().toLowerCase(java.util.Locale.ROOT).startsWith("codechickenlib"))
                            && "codechicken/lib/".equals(b.apiRoot()))).toList();
            List<String> apiIds = jars.stream().flatMap(j -> j.apis.stream()).map(a -> a[0]).toList();
            java.util.Set<String> have = LegacyDependencies.presentIds(jars, inUse, java.util.Set.of(), apiIds);
            for (var p : LegacyDependencies.check(jars, have, inUse, true)) {
                LegacyJar owner = jars.stream().filter(j -> j.mods.contains(p.mod())).findFirst().orElse(null);
                if (!p.fatal() || owner == null) continue;
                missingDeps.computeIfAbsent(owner, k -> new ArrayList<>()).add(p.message());
                System.out.println(owner.fileName() + ": " + p.message());
            }
        }
        transformer.presentMods(jars.stream().flatMap(j -> j.mods.stream()).map(m -> m.legacyId).toList());
        Map<LegacyVersion, org.kylepitcock.legacyconnector.loader.transform.PeerClasses> peerSets = new EnumMap<>(LegacyVersion.class);
        for (LegacyJar jar : jars) {
            peerSets.computeIfAbsent(jar.version, v -> new org.kylepitcock.legacyconnector.loader.transform.PeerClasses()).add(transformer.scan(jar));
        }
        Map<LegacyVersion, Map<String, ClassInfo>> peers = new EnumMap<>(LegacyVersion.class);
        peerSets.forEach((v, set) -> peers.put(v, set.classes()));
        Map<LegacyVersion, SynthDemand> demand = new EnumMap<>(LegacyVersion.class);
        Map<Path, JsonObject> recordsByOutJar = new java.util.LinkedHashMap<>();
        List<Path> originals = new ArrayList<>();
        for (LegacyJar j : jars) ALL_ORIGINALS.add(j.existing());
        int n = 0;
        for (LegacyJar jar : jars) {
            String obsolete = LibraryBridges.obsoleteReason(jar);
            if (obsolete != null) {
                records.add(record(jar, "obsolete", obsolete, null, 0));
                System.out.println(jar.fileName() + ": obsolete on 1.21.1 (" + obsolete + ")");
                continue;
            }
            Path outJar = out.resolve("out-" + (n++) + ".jar");
            var result = transformer.transform(jar, outJar, peers.get(jar.version));
            JsonObject rec = record(jar, result.links.errorCount() == 0 ? "links" : "unresolved", null, result.links, result.classes);
            if (missingDeps.containsKey(jar)) {
                // not a defect of the layer: the pack lacks a library the mod declares as required
                // the mod still loads in the game (FML would not have started it); true when everything else it uses links
                if (result.links.errorCount() == 0) rec.addProperty("linksWithoutDependency", true);
                rec.addProperty("result", "missing-dependency");
                JsonArray deps = new JsonArray();
                missingDeps.get(jar).forEach(deps::add);
                rec.add("missingDependencies", deps);
                rec.addProperty("reason", missingDeps.get(jar).get(0));
            }
            records.add(rec);
            recordsByOutJar.put(outJar, rec);
            // -Dlegacyconnector.check.keep=<dir>: keep the translated jars (for ./gradlew textureReport)
            String keep = System.getProperty("legacyconnector.check.keep");
            if (keep != null && !keep.isBlank() && Files.isRegularFile(outJar)) {
                Files.createDirectories(Path.of(keep));
                Files.copy(outJar, Path.of(keep).resolve(jar.fileName().replaceAll("[^A-Za-z0-9._-]", "_") + ".translated.jar"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            verifyBytecode(index, jar, outJar, peers.get(jar.version), rec);
            originals.add(jar.existing());
            demand.computeIfAbsent(jar.version, v -> new SynthDemand()).merge(result.demand);
            System.out.println(jar.fileName() + " [" + jar.version.id() + ", " + jar.versionEvidence + "]: " + result.classes + " classes, "
                    + result.links.errorCount() + " unresolved, " + result.links.synthesizedClasses.size() + " synthesized classes, "
                    + result.links.stubbedMembers.size() + " synthesized members");
            if (jar.coreModClass != null) System.out.println("  coremod: " + jar.coreModClass + " (not executed)");
            for (String note : result.notes) System.out.println("  note: " + note);
            if (!"-q".equals(System.getProperty("legacyconnector.check.quiet", ""))) System.out.print(result.links.render());
        }
        checkAssets(recordsByOutJar, originals);
        if (!"false".equalsIgnoreCase(System.getProperty("legacyconnector.check.verify"))) {
            System.out.println("verify: " + VERIFY_TOTALS[0] + " classes, " + VERIFY_TOTALS[1] + " methods, " + VERIFY_TOTALS[2] + " findings in " + VERIFY_TOTALS[3] + " groups (all jars)");
            if (!GLOBAL_UNKNOWN.isEmpty()) {
                // by package prefix (first three segments): mostly optional integrations with mods that are not in the set
                Map<String, Integer> byPrefix = new java.util.TreeMap<>();
                for (String u : GLOBAL_UNKNOWN) {
                    String[] seg = u.split("/");
                    byPrefix.merge(String.join("/", java.util.Arrays.copyOf(seg, Math.min(3, seg.length - 1 < 1 ? 1 : seg.length - 1))), 1, Integer::sum);
                }
                System.out.println("  " + GLOBAL_UNKNOWN.size() + " distinct unknown types treated as assignable (the verifier is blind there), by package: "
                        + byPrefix.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(12).map(e -> e.getKey() + " " + e.getValue()).toList());
            }
            GLOBAL_GROUPS.forEach((k, g) -> {
                System.out.println("  " + g.sites + " site(s) in " + g.jars.size() + " jar(s): " + k);
                if (g.example != null) System.out.println("    e.g. " + g.example);
                System.out.println("    jars: " + String.join(", ", g.jars.stream().limit(8).toList()) + (g.jars.size() > 8 ? ", ..." : ""));
            });
        }
        if (jsonPath != null) {
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            Files.writeString(Path.of(jsonPath), gson.toJson(records) + "\n");
        }
        for (var e : demand.entrySet()) {
            Map<String, byte[]> classes = new SynthJarBuilder(e.getKey(), index, CheckJar.class.getClassLoader(), e.getValue()).build();
            System.out.println("Synthesized " + classes.size() + " " + e.getKey().id() + " classes (" + e.getValue().helperCount() + " helpers)");
            verify(classes);
        }
        // -Dlegacyconnector.check.verify.fail=true: a non-zero exit code when the bytecode verification found anything (CI use)
        if ("true".equalsIgnoreCase(System.getProperty("legacyconnector.check.verify.fail")) && VERIFY_TOTALS[2].get() > 0) System.exit(1);
    }

    /**
     * Rendering smoke test of every translated jar ({@link ResourceCheck}): missing textures, models and blockstate models, added to each record
     * as "assets". Needs the vanilla 1.21.1 client jar ({@code -Dlegacyconnector.check.vanillaJar=...}, found in the Gradle cache otherwise);
     * without it the check is skipped. {@code -Dlegacyconnector.check.assets=false} switches it off.
     */
    private static void checkAssets(Map<Path, JsonObject> records, List<Path> originals) {
        if ("false".equalsIgnoreCase(System.getProperty("legacyconnector.check.assets")) || records.isEmpty()) return;
        Path vanilla = vanillaJar();
        if (vanilla == null) {
            System.out.println("Asset check skipped: the vanilla 1.21.1 client jar was not found (set -Dlegacyconnector.check.vanillaJar)");
            return;
        }
        try (ResourceCheck.Index index = new ResourceCheck.Index()) {
            index.add(vanilla);
            for (Path jar : records.keySet()) index.add(jar);
            for (Path jar : originals) index.addOriginal(jar);
            int withProblems = 0, layer = 0, mod = 0;
            for (var e : records.entrySet()) {
                JsonObject assets = ResourceCheck.check(e.getKey(), index);
                e.getValue().add("assets", assets);
                int l = assets.get("layer").getAsInt();
                if (l > 0) withProblems++;
                layer += l;
                mod += assets.get("mod").getAsInt();
            }
            System.out.println("Asset check: " + withProblems + " of " + records.size() + " jars have asset references the translation lost ("
                    + layer + " in total); " + mod + " more are missing from the mods' own jars too");
        } catch (IOException | RuntimeException e) {
            System.out.println("Asset check failed: " + e);
        }
    }

    private static Path vanillaJar() {
        String configured = System.getProperty("legacyconnector.check.vanillaJar");
        if (configured != null && !configured.isBlank()) return Files.isRegularFile(Path.of(configured)) ? Path.of(configured) : null;
        Path cached = Path.of(System.getProperty("user.home"), ".gradle", "caches", "neoformruntime", "artifacts", "minecraft_1.21.1_client.jar");
        if (Files.isRegularFile(cached)) return cached;
        // a fresh checkout (CI): the dev environment's own copy of the client resources, created by the build
        try (var jars = Files.list(Path.of("build", "moddev", "artifacts"))) {
            return jars.filter(j -> j.getFileName().toString().contains("minecraft-resources") && j.toString().endsWith(".jar")).findFirst().orElse(null);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static JsonObject record(LegacyJar jar, String result, String reason, LinkReport links, int classes) {
        JsonObject r = new JsonObject();
        r.addProperty("jar", jar.fileName());
        r.addProperty("mc", jar.version.id());
        r.addProperty("result", result);
        JsonArray mods = new JsonArray();
        for (LegacyModDecl m : jar.mods) {
            JsonObject o = new JsonObject();
            o.addProperty("id", m.legacyId);
            o.addProperty("name", m.name);
            o.addProperty("version", m.version);
            mods.add(o);
        }
        r.add("mods", mods);
        if (reason != null) r.addProperty("reason", reason);
        if (jar.coreModClass != null) r.addProperty("coremod", jar.coreModClass);
        if (links != null) {
            r.addProperty("classes", classes);
            r.addProperty("unresolved", links.errorCount());
            r.addProperty("synthesizedClasses", links.synthesizedClasses.size());
            r.addProperty("synthesizedMembers", links.stubbedMembers.size());
            r.add("missing", members(links.missingClasses, links.missingShimMembers, links.missingLibraryMembers));
            // legacy API the layer generated as empty stubs: it links but does nothing, so it is a gap in behaviour (tools/compat/missing_report.py)
            r.add("stubbed", members(links.stubbedMembers));
            // calls to @Optional.Method members removed for an absent mod: guarded optional integrations, not failures
            r.add("optional", members(links.optionalMembers));
        }
        return r;
    }

    /** Every reference of the groups (the site's notes only show the first few; tools/compat/missing_report.py ranks all of them). */
    @SafeVarargs
    private static JsonArray members(Map<String, TreeSet<String>>... groups) {
        JsonArray a = new JsonArray();
        for (var g : groups) {
            for (var e : g.entrySet()) a.add(e.getKey().replace('/', '.'));
        }
        return a;
    }

    /** Game classes whose {@code final} an access transformer removes: the layer's own, the loaded jars' (the set), and the checked jar's. */
    private static final java.util.Set<String> UNFINAL = new java.util.TreeSet<>();
    private static boolean unfinalLoaded;

    private static java.util.Set<String> unfinalClasses(Path translatedJar) {
        synchronized (UNFINAL) {
            if (!unfinalLoaded) {
                unfinalLoaded = true;
                try {
                    for (var urls = CheckJar.class.getClassLoader().getResources("META-INF/accesstransformer.cfg"); urls.hasMoreElements(); ) {
                        try (InputStream in = urls.nextElement().openStream()) {
                            BytecodeCheck.parseFinalRemovals(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), UNFINAL);
                        }
                    }
                } catch (IOException ignored) {
                    // classpath resources only
                }
                for (Path jar : ALL_ORIGINALS) addAccessTransformer(jar, UNFINAL);
            }
        }
        java.util.Set<String> all = new java.util.TreeSet<>(UNFINAL);
        addAccessTransformer(translatedJar, all);
        return all;
    }

    private static void addAccessTransformer(Path jar, java.util.Set<String> into) {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.toFile())) {
            var e = zip.getEntry("META-INF/accesstransformer.cfg");
            if (e != null) {
                try (InputStream in = zip.getInputStream(e)) {
                    BytecodeCheck.parseFinalRemovals(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), into);
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // not a zip or unreadable: the other checks report it
        }
    }

    /** The original jars of the set, read once for their access transformers. */
    private static final List<Path> ALL_ORIGINALS = new ArrayList<>();

    /** Class headers of the non-legacy jars of the set (modern libraries), for the verifier's type relations. */
    private static final Map<String, ClassInfo> MODERN_LIBS = new java.util.HashMap<>();

    private static void readModernLibrary(Path jar) {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.toFile())) {
            for (var en = zip.entries(); en.hasMoreElements(); ) {
                var entry = en.nextElement();
                if (!entry.getName().endsWith(".class") || entry.getName().startsWith("META-INF/")) continue;
                try (InputStream in = zip.getInputStream(entry)) {
                    var node = new org.objectweb.asm.tree.ClassNode();
                    new org.objectweb.asm.ClassReader(in.readAllBytes()).accept(node, org.objectweb.asm.ClassReader.SKIP_CODE | org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
                    ClassInfo info = new ClassInfo(node.name, node.superName, node.interfaces == null ? List.of() : node.interfaces, node.access);
                    for (var m : node.methods) info.methods.put(m.name + m.desc, m.access);
                    MODERN_LIBS.put(node.name, info);
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // not a zip: reported as not-legacy anyway
        }
    }

    /** One finding group over the whole set (the same type pair in several jars is one group), for the closing report. */
    private static final class GlobalGroup {
        int sites;
        final java.util.Set<String> jars = new java.util.TreeSet<>();
        String example;
    }

    /** Types no jar, shim or classpath entry could resolve (assumed assignable): the verifier is blind there. */
    private static final java.util.Set<String> GLOBAL_UNKNOWN = new java.util.TreeSet<>();

    private static final Map<String, GlobalGroup> GLOBAL_GROUPS = new java.util.LinkedHashMap<>();

    /** Totals over all jars, for the closing "verify:" line. */
    private static final java.util.concurrent.atomic.AtomicInteger[] VERIFY_TOTALS = {new java.util.concurrent.atomic.AtomicInteger(), new java.util.concurrent.atomic.AtomicInteger(),
            new java.util.concurrent.atomic.AtomicInteger(), new java.util.concurrent.atomic.AtomicInteger()};

    /**
     * Data-flow verification of the translated classes ({@link BytecodeCheck}); on by default, {@code -Dlegacyconnector.check.verify=false}
     * (Gradle {@code -Pverify=false}) skips it. Failures are listed in the record as {@code verifyErrors} and grouped in {@code verify}.
     */
    private static void verifyBytecode(ShimIndex index, LegacyJar jar, Path outJar, Map<String, ClassInfo> peers, JsonObject rec) {
        if ("false".equalsIgnoreCase(System.getProperty("legacyconnector.check.verify"))) return;
        try {
            Map<String, ClassInfo> known = new java.util.HashMap<>(MODERN_LIBS);
            if (peers != null) known.putAll(peers);
            var hierarchy = new org.kylepitcock.legacyconnector.loader.transform.Hierarchy(index, CheckJar.class.getClassLoader(), known, jar.version);
            BytecodeCheck check = BytecodeCheck.run(outJar, hierarchy, unfinalClasses(outJar));
            VERIFY_TOTALS[0].addAndGet(check.classesChecked);
            VERIFY_TOTALS[1].addAndGet(check.methodsChecked);
            VERIFY_TOTALS[2].addAndGet(check.findingCount);
            for (BytecodeCheck.Group g : check.groups.values()) {
                String key = g.expected.isEmpty() ? g.message : g.found + " is not assignable to " + g.expected;
                GlobalGroup gg = GLOBAL_GROUPS.computeIfAbsent(key, k -> new GlobalGroup());
                gg.sites += g.sites;
                gg.jars.add(jar.fileName());
                if (gg.example == null && !g.examples.isEmpty()) gg.example = g.examples.get(0).site();
            }
            VERIFY_TOTALS[3].set(GLOBAL_GROUPS.size());
            GLOBAL_UNKNOWN.addAll(check.unknownTypes);
            rec.addProperty("verifyFailures", check.failureCount);
            rec.add("verify", check.toJson());
            System.out.println("  " + check.summary());
            if (check.failureCount > 0) {
                JsonArray list = new JsonArray();
                check.failures.forEach(list::add);
                rec.add("verifyErrors", list);
                for (BytecodeCheck.Group g : check.groups.values()) {
                    System.out.println("    " + g.sites + " site(s): " + g.describe());
                    for (BytecodeCheck.Finding f : g.examples) System.out.println("      " + f.site());
                }
            }
        } catch (Exception | LinkageError e) {
            System.out.println("  bytecode verification skipped: " + e);
        }
    }

    /** Defines and links every generated class; VerifyError / IncompatibleClassChangeError mean a generator bug. */
    private static void verify(Map<String, byte[]> classes) {
        ClassLoader loader = new URLClassLoader(new URL[0], CheckJar.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] b = classes.get(name.replace('.', '/'));
                if (b == null) throw new ClassNotFoundException(name);
                return defineClass(name, b, 0, b.length);
            }
        };
        int ok = 0;
        List<String> bad = new ArrayList<>();
        for (String name : classes.keySet()) {
            try {
                Class<?> c = Class.forName(name.replace('/', '.'), false, loader);
                // getDeclaredMethods links the class (verifies it) without running static initializers.
                c.getDeclaredMethods();
                ok++;
            } catch (VerifyError | ClassFormatError | IncompatibleClassChangeError e) {
                bad.add(name + ": " + e);
            } catch (Throwable t) {
                ok++; // missing client classes on this classpath etc. - not a generator problem
            }
        }
        System.out.println("  verified " + ok + "/" + classes.size() + " generated classes" + (bad.isEmpty() ? "" : ", FAILURES:"));
        for (String b : bad) System.out.println("    " + b);
    }
}
