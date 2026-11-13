package org.kylepitcock.legacyconnector.loader.transform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.kylepitcock.legacyconnector.loader.mapping.SrgMojangTable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

/**
 * Offline measurement of the mixin target proof on local Forge 1.20.1 jars (no game, nothing written): how many injection selectors of their mixin
 * classes are exact in 1.21.1, repaired by re-signing, dropped, or not provable, with the report lines of the repaired and dropped ones.
 * Classes get the class renames of the tables only (the full translation is not run).
 *
 * <p>usage: {@code Mixin1201Survey <report.txt> <jar-or-dir>...}
 */
public final class Mixin1201Survey {
    private Mixin1201Survey() {}

    public static void main(String[] args) throws Exception {
        List<Path> jars = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            Path p = Path.of(args[i]);
            if (Files.isDirectory(p)) {
                try (var s = Files.list(p)) {
                    s.filter(x -> x.toString().endsWith(".jar")).sorted().forEach(jars::add);
                }
            } else jars.add(p);
        }
        Mc1201Tables t = Mc1201Tables.get();
        Remapper mapper = new Remapper() {
            @Override
            public String map(String name) {
                String r = t.classes.get(name);
                return r != null && !r.startsWith("org/kylepitcock/") ? r : name;
            }
        };
        Map<String, String> srg = SrgMojangTable.load();
        StringBuilder out = new StringBuilder();
        Mixin1201.Stats total = new Mixin1201.Stats();
        int jarsWithMixins = 0;
        int sampleSize = Integer.getInteger("survey.sample", 30);
        Mixin1201.Stats sample = new Mixin1201.Stats();
        for (Path jar : jars) {
            Mixin1201.Stats st = new Mixin1201.Stats();
            try {
                Map<String, byte[]> resources = new LinkedHashMap<>();
                List<ClassNode> nodes = new ArrayList<>();
                Mc1201Hierarchy h = new Mc1201Hierarchy();
                try (ZipFile z = new ZipFile(jar.toFile())) {
                    for (var en = z.entries(); en.hasMoreElements(); ) {
                        ZipEntry e = en.nextElement();
                        if (e.isDirectory()) continue;
                        if (e.getName().endsWith(".refmap.json")) {
                            try (InputStream in = z.getInputStream(e)) {
                                resources.put(e.getName(), in.readAllBytes());
                            }
                        } else if (e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/")) {
                            try (InputStream in = z.getInputStream(e)) {
                                ClassNode n = new ClassNode();
                                new ClassReader(in).accept(new ClassRemapper(n, mapper), 0);
                                nodes.add(n);
                                h.add(n);
                            } catch (RuntimeException ex) {
                                // unreadable class: skipped
                            }
                        }
                    }
                }
                st.refmaps = org.kylepitcock.legacyconnector.loader.transform.mixin1201.Refmaps.parse(resources);
                for (ClassNode n : nodes) if (Mixin1201.isMixin(n)) Mixin1201.apply(n, srg, h, t, st);
            } catch (IOException | RuntimeException e) {
                out.append(jar.getFileName()).append(": ERROR ").append(e).append('\n');
                continue;
            }
            if (st.mixinClasses == 0) continue;
            jarsWithMixins++;
            var c = st.repair;
            out.append(jar.getFileName()).append(": ").append(st.mixinClasses).append(" mixins; exact ").append(c.exact).append(", repaired ").append(c.repaired)
                    .append(", dropped ").append(c.dropped).append(", unproven ").append(c.unproven).append("; @At exact ").append(c.atExact).append(", repaired ").append(c.atRepaired)
                    .append(", dropped ").append(c.atDropped).append(", mixin classes dropped ").append(st.droppedClasses.size()).append('\n');
            for (String n : c.notes) out.append("    ").append(n).append('\n');
            total.mixinClasses += st.mixinClasses;
            total.droppedClasses.addAll(st.droppedClasses);
            total.repair.exact += c.exact;
            total.repair.repaired += c.repaired;
            total.repair.dropped += c.dropped;
            total.repair.unproven += c.unproven;
            total.repair.atExact += c.atExact;
            total.repair.atRepaired += c.atRepaired;
            total.repair.atDropped += c.atDropped;
            if (jarsWithMixins <= sampleSize) {
                sample.repair.exact += c.exact;
                sample.repair.repaired += c.repaired;
                sample.repair.dropped += c.dropped;
                sample.repair.unproven += c.unproven;
                sample.repair.atExact += c.atExact;
                sample.repair.atRepaired += c.atRepaired;
                sample.repair.atDropped += c.atDropped;
            }
        }
        var c = total.repair;
        String head = jars.size() + " jars, " + jarsWithMixins + " with mixin classes, " + total.mixinClasses + " mixin classes (" + total.droppedClasses.size() + " dropped as a whole)\n"
                + "injection selectors: exact " + c.exact + ", repaired " + c.repaired + ", dropped " + c.dropped + ", not provable (kept) " + c.unproven + "\n"
                + "@At targets: exact " + c.atExact + ", repaired " + c.atRepaired + ", dropped " + c.atDropped + "\n"
                + "first " + sampleSize + " jars with mixins: injection selectors exact " + sample.repair.exact + ", repaired " + sample.repair.repaired + ", dropped " + sample.repair.dropped
                + ", not provable " + sample.repair.unproven + "; @At exact " + sample.repair.atExact + ", repaired " + sample.repair.atRepaired + ", dropped " + sample.repair.atDropped + "\n\n";
        Files.writeString(Path.of(args[0]), head + out);
        System.out.println(head);
    }
}
