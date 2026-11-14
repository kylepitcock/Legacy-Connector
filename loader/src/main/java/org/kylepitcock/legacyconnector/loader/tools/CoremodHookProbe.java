package org.kylepitcock.legacyconnector.loader.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.LegacyJarInspector;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.transform.CoremodHookDiscovery;
import org.kylepitcock.legacyconnector.loader.transform.CoremodHookScan;
import org.kylepitcock.legacyconnector.loader.transform.LegacyCoreModTransformers;
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

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("usage: CoremodHookProbe <client.jar> <unused> <work dir> <mod.jar>... (the Minecraft version is the first jar's)");
            System.exit(2);
        }
        Path client = Path.of(args[0]); // args[1] (joined.srg) is no longer read: the names come from legacy/1.7.10/notch.txt, as at run time
        Path work = Path.of(args[2]);
        Files.createDirectories(work);
        List<LegacyJar> jars = new ArrayList<>();
        for (int i = 3; i < args.length; i++) {
            LegacyJar jar = LegacyJarInspector.inspect(Path.of(args[i]));
            if (jar != null) jars.add(jar);
            else System.err.println("not a legacy mod jar, skipped: " + args[i]);
        }
        LegacyCoreModTransformers transformers = LegacyCoreModTransformers.load(jars, work, work, "CLIENT", null, LegacyCoreModTransformers.apiFor(jars.get(0).version));
        if (transformers.isEmpty()) {
            System.out.println("no coremod class transformers in these jars");
            return;
        }
        Map<String, List<String>> report = new LinkedHashMap<>();
        List<CoremodHookScan.Change> changes = CoremodHookScan.changes(transformers, client, jars.get(0).version.id());
        for (CoremodHookScan.Change c : changes) {
            describe(report, c.transformer(), c.className(), c.obfuscated() ? "obfuscated names" : "SRG names", c.before(), c.after());
        }
        for (var e : report.entrySet()) {
            System.out.println("== " + e.getKey());
            e.getValue().forEach(System.out::println);
        }
        System.out.println(changes.size() + " transformed class version(s)");
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
