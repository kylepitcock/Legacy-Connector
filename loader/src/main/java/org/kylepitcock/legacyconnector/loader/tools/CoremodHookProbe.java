package org.kylepitcock.legacyconnector.loader.tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.LegacyJarInspector;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.transform.CoremodHookDiscovery;
import org.kylepitcock.legacyconnector.loader.transform.LegacyCoreModTransformers;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Development tool: what the coremods of some 1.7.10 jars do to Minecraft's own classes. Every class of the 1.7.10 client jar is shown to each class
 * transformer (first under its obfuscated name, as LaunchWrapper did before FML's deobfuscation; when a transformer leaves that alone, in SRG names, as a
 * transformer sorted after the deobfuscation saw it) and the difference is printed per method in SRG names: instructions added and removed, with the
 * instruction before them, and members and interfaces added.
 *
 * <pre>CoremodHookProbe &lt;client-1.7.10.jar&gt; &lt;mcp-1.7.10-srg.zip&gt; &lt;work dir&gt; &lt;mod.jar&gt;...</pre>
 */
public final class CoremodHookProbe {
    /** -Dlegacyconnector.probe.sites=true: only the hook sites, one line each. */
    private static final boolean SITES_ONLY = Boolean.getBoolean("legacyconnector.probe.sites");

    private CoremodHookProbe() {}

    /** joined.srg: obfuscated names to SRG names. */
    private static final class Srg extends Remapper {
        final Map<String, String> classes = new HashMap<>();
        final Map<String, String> fields = new HashMap<>();
        final Map<String, String> methods = new HashMap<>();
        final Map<String, String[]> supers = new HashMap<>();

        @Override
        public String map(String name) {
            return classes.getOrDefault(name, name);
        }

        @Override
        public String mapFieldName(String owner, String name, String desc) {
            for (String o : lineage(owner)) {
                String s = fields.get(o + "/" + name);
                if (s != null) return s;
            }
            return name;
        }

        @Override
        public String mapMethodName(String owner, String name, String desc) {
            for (String o : lineage(owner)) {
                String s = methods.get(o + "/" + name + desc);
                if (s != null) return s;
            }
            return name;
        }

        private List<String> lineage(String owner) {
            List<String> out = new ArrayList<>();
            List<String> queue = new ArrayList<>(List.of(owner));
            while (!queue.isEmpty()) {
                String o = queue.remove(0);
                if (out.contains(o)) continue;
                out.add(o);
                String[] s = supers.get(o);
                if (s != null) for (String x : s) if (x != null) queue.add(x);
            }
            return out;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("usage: CoremodHookProbe <client-1.7.10.jar> <mcp-1.7.10-srg.zip> <work dir> <mod.jar>...");
            System.exit(2);
        }
        Path client = Path.of(args[0]);
        Path work = Path.of(args[2]);
        Files.createDirectories(work);
        Srg srg = new Srg();
        try (ZipFile zip = new ZipFile(args[1])) {
            ZipEntry joined = zip.getEntry("joined.srg");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(zip.getInputStream(joined), StandardCharsets.UTF_8))) {
                for (String line; (line = in.readLine()) != null;) {
                    String[] a = line.split(" ");
                    switch (a[0]) {
                        case "CL:" -> srg.classes.put(a[1], a[2]);
                        case "FD:" -> srg.fields.put(a[1], a[2].substring(a[2].lastIndexOf('/') + 1));
                        case "MD:" -> {
                            int cut = a[1].lastIndexOf('/');
                            srg.methods.put(a[1].substring(0, cut) + "/" + a[1].substring(cut + 1) + a[2], a[3].substring(a[3].lastIndexOf('/') + 1));
                        }
                        default -> { }
                    }
                }
            }
        }
        Map<String, byte[]> vanilla = new TreeMap<>();
        try (ZipFile zip = new ZipFile(client.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (!e.getName().endsWith(".class")) continue;
                String name = e.getName().substring(0, e.getName().length() - 6);
                if (name.contains("/") && !name.startsWith("net/minecraft/")) continue;
                try (InputStream in = zip.getInputStream(e)) {
                    byte[] bytes = in.readAllBytes();
                    vanilla.put(name, bytes);
                    ClassReader r = new ClassReader(bytes);
                    String[] ifaces = r.getInterfaces();
                    String[] sup = new String[ifaces.length + 1];
                    sup[0] = r.getSuperName();
                    System.arraycopy(ifaces, 0, sup, 1, ifaces.length);
                    srg.supers.put(name, sup);
                }
            }
        }
        List<LegacyJar> jars = new ArrayList<>();
        for (int i = 3; i < args.length; i++) jars.add(LegacyJarInspector.inspect(Path.of(args[i])));
        LegacyCoreModTransformers transformers = LegacyCoreModTransformers.load(jars, work, work, "CLIENT", null, LegacyVersion.V1_7_10);
        if (transformers.isEmpty()) {
            System.out.println("no coremod class transformers in these jars");
            return;
        }
        Map<String, List<String>> report = new LinkedHashMap<>();
        int[] changed = {0};
        for (var e : vanilla.entrySet()) {
            String notch = e.getKey();
            String deobf = srg.map(notch);
            byte[] original = e.getValue();
            byte[] originalSrg = remap(original, srg);
            Map<String, Boolean> seen = new HashMap<>();
            transformers.applyToVanilla(notch.replace('/', '.'), deobf.replace('/', '.'), original, (name, out) -> {
                seen.put(name, true);
                changed[0]++;
                describe(report, name, deobf, "obfuscated names", node(originalSrg), node(remap(out, srg)));
            });
            transformers.applyToVanilla(deobf.replace('/', '.'), deobf.replace('/', '.'), originalSrg, (name, out) -> {
                if (seen.containsKey(name)) return;
                changed[0]++;
                describe(report, name, deobf, "SRG names", node(originalSrg), node(out));
            });
        }
        for (var e : report.entrySet()) {
            System.out.println("== " + e.getKey());
            e.getValue().forEach(System.out::println);
        }
        System.out.println(changed[0] + " transformed class version(s) of " + vanilla.size() + " Minecraft classes");
    }

    private static byte[] remap(byte[] bytes, Remapper remapper) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytes).accept(new ClassRemapper(writer, remapper), 0);
        return writer.toByteArray();
    }

    private static ClassNode node(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static void describe(Map<String, List<String>> report, String transformer, String cls, String form, ClassNode before, ClassNode after) {
        List<String> out = report.computeIfAbsent(transformer, k -> new ArrayList<>());
        out.add("  " + cls + "  (seen in " + form + ")");
        if (SITES_ONLY) {
            CoremodHookDiscovery.hooks(before, after).forEach(h -> out.add("    " + h));
            return;
        }
        for (String i : after.interfaces) if (!before.interfaces.contains(i)) out.add("    + implements " + i);
        if (after.superName != null && !after.superName.equals(before.superName)) out.add("    ~ extends " + after.superName + " (was " + before.superName + ")");
        Map<String, FieldNode> fields = new HashMap<>();
        for (FieldNode f : before.fields) fields.put(f.name + ":" + f.desc, f);
        for (FieldNode f : after.fields) {
            FieldNode old = fields.remove(f.name + ":" + f.desc);
            if (old == null) out.add("    + field " + f.name + " " + f.desc);
            else if (old.access != f.access) out.add("    ~ field " + f.name + " access " + old.access + " -> " + f.access);
        }
        Map<String, MethodNode> methods = new HashMap<>();
        for (MethodNode m : before.methods) methods.put(m.name + m.desc, m);
        for (MethodNode m : after.methods) {
            MethodNode old = methods.remove(m.name + m.desc);
            if (old == null) {
                out.add("    + method " + m.name + m.desc);
                for (String s : text(m)) out.add("        " + s);
                continue;
            }
            if (old.access != m.access) out.add("    ~ method " + m.name + m.desc + " access " + old.access + " -> " + m.access);
            List<String> a = text(old);
            List<String> b = text(m);
            if (a.equals(b)) continue;
            out.add("    ~ method " + m.name + m.desc);
            diff(out, a, b);
        }
        for (String gone : methods.keySet()) out.add("    - method " + gone);
    }

    /** Each edit of a method, with the instruction before and after it. */
    private static void diff(List<String> out, List<String> a, List<String> b) {
        for (CoremodHookDiscovery.Run run : CoremodHookDiscovery.runs(a, b)) {
            out.add("      @ " + (run.previous() == null ? "HEAD" : "after #" + (run.index() - 1) + " " + run.previous()));
            run.removed().forEach(x -> out.add("        - " + x));
            run.added().forEach(x -> out.add("        + " + x));
            out.add("        (next: " + (run.next() == null ? "END" : run.next()) + ")");
        }
    }

    private static List<String> text(MethodNode m) {
        return CoremodHookDiscovery.text(m);
    }
}
