package org.kylepitcock.mccompat.loader.tools;

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
import org.kylepitcock.mccompat.loader.LegacyJar;
import org.kylepitcock.mccompat.loader.LegacyJarInspector;
import org.kylepitcock.mccompat.loader.LegacyVersion;
import org.kylepitcock.mccompat.loader.shim.ClassInfo;
import org.kylepitcock.mccompat.loader.shim.ShimIndex;
import org.kylepitcock.mccompat.loader.synth.SynthDemand;
import org.kylepitcock.mccompat.loader.synth.SynthJarBuilder;
import org.kylepitcock.mccompat.loader.transform.LegacyJarTransformer;

/**
 * Developer tool: {@code CheckJar <shim-index.txt> <legacy mod jar>...}. Translates jars offline, as one modset (so
 * mods that extend each other link), prints which references cannot be linked, then generates the synthesized
 * legacy API and verifies every generated class against the real shim.
 */
public final class CheckJar {
    private CheckJar() {}

    public static void main(String[] args) throws Exception {
        ShimIndex index;
        try (InputStream in = Files.newInputStream(Path.of(args[0]))) {
            index = ShimIndex.read(in);
        }
        Path out = Files.createTempDirectory("mccompat-check");
        LegacyJarTransformer transformer = new LegacyJarTransformer(index, CheckJar.class.getClassLoader());
        List<LegacyJar> jars = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            Path jarPath = Path.of(args[i]);
            LegacyJar jar = LegacyJarInspector.inspect(jarPath);
            if (jar == null) System.out.println(jarPath.getFileName() + ": not a legacy Forge mod");
            else jars.add(jar);
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
            var result = transformer.transform(jar, out.resolve("out-" + (n++) + ".jar"), peers.get(jar.version));
            demand.computeIfAbsent(jar.version, v -> new SynthDemand()).merge(result.demand);
            System.out.println(jar.fileName() + " [" + jar.version.id() + ", " + jar.versionEvidence + "]: " + result.classes + " classes, "
                    + result.links.errorCount() + " unresolved, " + result.links.synthesizedClasses.size() + " synthesized classes, "
                    + result.links.stubbedMembers.size() + " synthesized members");
            if (jar.coreModClass != null) System.out.println("  coremod: " + jar.coreModClass + " (not executed)");
            if (!"-q".equals(System.getProperty("mccompat.check.quiet", ""))) System.out.print(result.links.render());
        }
        for (var e : demand.entrySet()) {
            Map<String, byte[]> classes = new SynthJarBuilder(e.getKey(), index, CheckJar.class.getClassLoader(), e.getValue()).build();
            System.out.println("Synthesized " + classes.size() + " " + e.getKey().id() + " classes (" + e.getValue().helperCount() + " helpers)");
            verify(classes);
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
