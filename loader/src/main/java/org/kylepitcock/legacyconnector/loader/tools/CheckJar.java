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
        for (String jarArg : jarArgs) {
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
        if (jsonPath != null) {
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            Files.writeString(Path.of(jsonPath), gson.toJson(records) + "\n");
        }
        for (var e : demand.entrySet()) {
            Map<String, byte[]> classes = new SynthJarBuilder(e.getKey(), index, CheckJar.class.getClassLoader(), e.getValue()).build();
            System.out.println("Synthesized " + classes.size() + " " + e.getKey().id() + " classes (" + e.getValue().helperCount() + " helpers)");
            verify(classes);
        }
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

    /** Data-flow verification of the translated classes ({@link BytecodeCheck}); failures are listed in the record as {@code verifyErrors}. */
    private static void verifyBytecode(ShimIndex index, LegacyJar jar, Path outJar, Map<String, ClassInfo> peers, JsonObject rec) {
        try {
            var hierarchy = new org.kylepitcock.legacyconnector.loader.transform.Hierarchy(index, CheckJar.class.getClassLoader(),
                    peers == null ? Map.of() : peers, jar.version);
            BytecodeCheck check = BytecodeCheck.run(outJar, hierarchy);
            rec.addProperty("verifyFailures", check.failureCount);
            System.out.println("  bytecode verification: " + check.methodsChecked + " method(s) analysed, " + check.failureCount + " failing");
            if (check.failureCount > 0) {
                JsonArray list = new JsonArray();
                check.failures.forEach(list::add);
                rec.add("verifyErrors", list);
                System.out.println("  " + check.failureCount + " of " + check.methodsChecked + " method(s) fail bytecode verification:");
                for (String f : check.failures) System.out.println("    " + f);
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
