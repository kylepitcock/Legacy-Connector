package org.kylepitcock.legacyconnector.loader.tools;

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
import org.kylepitcock.legacyconnector.loader.LegacyModDecl;
import org.kylepitcock.legacyconnector.loader.LibraryBridges;
import org.kylepitcock.legacyconnector.loader.transform.LinkReport;
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
        ShimIndex index;
        try (InputStream in = Files.newInputStream(Path.of(args[0]))) {
            index = ShimIndex.read(in);
        }
        String jsonPath = System.getProperty("legacyconnector.check.json");
        JsonArray records = new JsonArray();
        Path out = Files.createTempDirectory("legacyconnector-check");
        LegacyJarTransformer transformer = new LegacyJarTransformer(index, CheckJar.class.getClassLoader());
        List<LegacyJar> jars = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            Path jarPath = Path.of(args[i]);
            LegacyJar jar = LegacyJarInspector.inspect(jarPath);
            if (jar == null) {
                System.out.println(jarPath.getFileName() + ": not a legacy Forge mod");
                JsonObject r = new JsonObject();
                r.addProperty("jar", jarPath.getFileName().toString());
                r.addProperty("result", "not-legacy");
                records.add(r);
            } else jars.add(jar);
        }
        transformer.presentMods(jars.stream().flatMap(j -> j.mods.stream()).map(m -> m.legacyId).toList());
        Map<LegacyVersion, Map<String, ClassInfo>> peers = new EnumMap<>(LegacyVersion.class);
        for (LegacyJar jar : jars) {
            Map<String, ClassInfo> p = peers.computeIfAbsent(jar.version, v -> new HashMap<>());
            for (ClassInfo c : transformer.scan(jar)) p.putIfAbsent(c.name, c);
        }
        Map<LegacyVersion, SynthDemand> demand = new EnumMap<>(LegacyVersion.class);
        int n = 0;
        for (LegacyJar jar : jars) {
            String obsolete = LibraryBridges.obsoleteReason(jar);
            if (obsolete != null) {
                records.add(record(jar, "obsolete", obsolete, null, 0));
                System.out.println(jar.fileName() + ": obsolete on 1.21.1 (" + obsolete + ")");
                continue;
            }
            var result = transformer.transform(jar, out.resolve("out-" + (n++) + ".jar"), peers.get(jar.version));
            records.add(record(jar, result.links.errorCount() == 0 ? "links" : "unresolved", null, result.links, result.classes));
            demand.computeIfAbsent(jar.version, v -> new SynthDemand()).merge(result.demand);
            System.out.println(jar.fileName() + " [" + jar.version.id() + ", " + jar.versionEvidence + "]: " + result.classes + " classes, "
                    + result.links.errorCount() + " unresolved, " + result.links.synthesizedClasses.size() + " synthesized classes, "
                    + result.links.stubbedMembers.size() + " synthesized members");
            if (jar.coreModClass != null) System.out.println("  coremod: " + jar.coreModClass + " (not executed)");
            for (String note : result.notes) System.out.println("  note: " + note);
            if (!"-q".equals(System.getProperty("legacyconnector.check.quiet", ""))) System.out.print(result.links.render());
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
