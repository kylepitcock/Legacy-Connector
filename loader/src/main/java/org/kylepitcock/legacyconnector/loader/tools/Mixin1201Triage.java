package org.kylepitcock.legacyconnector.loader.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Offline mixin triage for Forge 1.20.1 jars (milestone 2): which mixin targets, injection methods and {@code @At} targets no longer resolve against the
 * Minecraft 1.21.1 + NeoForge classes. Names are checked as written in the annotations (Mojang names; 1.20.1 refmaps are not needed), after the class
 * renames of {@code mc-classes.tsv} and the member renames of {@code mc-members.tsv} when {@code -Dtriage.tables=<dir>} is given.
 *
 * <p>usage: {@code Mixin1201Triage <merged.jar> <report.txt> <jar>...} (directories are expanded to their jars).
 */
public final class Mixin1201Triage {
    private static final Set<String> INJECTORS = Set.of("Inject", "Redirect", "ModifyArg", "ModifyArgs", "ModifyVariable", "ModifyConstant", "ModifyExpressionValue",
            "ModifyReceiver", "ModifyReturnValue", "WrapOperation", "WrapWithCondition", "WrapMethod", "Overwrite", "Accessor", "Invoker", "Shadow");

    private record MC(String name, String sup, List<String> ifaces, Set<String> methods, Set<String> fields) {}

    private static final Map<String, MC> MC_INDEX = new HashMap<>();
    private static final Map<String, String> CLASS_RENAMES = new HashMap<>();
    /** owner + "." + name -> new name */
    private static final Map<String, String> MEMBER_RENAMES = new HashMap<>();

    private Mixin1201Triage() {}

    static final class Stats {
        int jars, jarsWithMixins, configs, mixins, mixinsMc, mixinsOther;
        int classMissing, classRenamed, memberChecks, memberMissing, atMissing, srgNames, plugin;
        final Map<String, Integer> missingClass = new TreeMap<>();
        final Map<String, Integer> missingMember = new TreeMap<>();
        final List<String> perJar = new ArrayList<>();
    }

    public static void main(String[] args) throws Exception {
        loadIndex(Path.of(args[0]));
        String tables = System.getProperty("triage.tables");
        if (tables != null) loadTables(Path.of(tables));
        List<Path> jars = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            Path p = Path.of(args[i]);
            if (Files.isDirectory(p)) {
                try (var s = Files.list(p)) {
                    s.filter(x -> x.toString().endsWith(".jar")).sorted().forEach(jars::add);
                }
            } else jars.add(p);
        }
        Stats st = new Stats();
        for (Path j : jars) {
            try {
                triage(j, st);
            } catch (IOException | RuntimeException e) {
                st.perJar.add(j.getFileName() + ": ERROR " + e);
            }
        }
        StringBuilder out = new StringBuilder();
        out.append("mixin triage: ").append(st.jars).append(" jars, ").append(st.jarsWithMixins).append(" with mixin configs, ").append(st.configs).append(" configs, ")
                .append(st.mixins).append(" mixin classes (").append(st.mixinsMc).append(" target Minecraft/NeoForge, ").append(st.mixinsOther).append(" other)\n");
        out.append("  mixin target class missing in 1.21.1: ").append(st.classMissing).append(" (renamed by tables: ").append(st.classRenamed).append(")\n");
        out.append("  member references checked: ").append(st.memberChecks).append(", unresolved: ").append(st.memberMissing).append(" (@At targets unresolved: ").append(st.atMissing)
                .append("), SRG-style names: ").append(st.srgNames).append(", config plugins: ").append(st.plugin).append('\n');
        out.append("\nper jar (mixins; MC classes missing; members unresolved):\n");
        for (String s : st.perJar) out.append("  ").append(s).append('\n');
        out.append("\nmissing target classes:\n");
        st.missingClass.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).forEach(e -> out.append("  ").append(e.getValue()).append("  ").append(e.getKey()).append('\n'));
        out.append("\nunresolved members (owner.name):\n");
        st.missingMember.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).forEach(e -> out.append("  ").append(e.getValue()).append("  ").append(e.getKey()).append('\n'));
        Files.writeString(Path.of(args[1]), out.toString());
        System.out.println(out.substring(0, Math.min(out.length(), 1500)));
    }

    private static void loadTables(Path dir) throws IOException {
        for (String l : Files.readAllLines(dir.resolve("mc-classes.tsv"))) {
            if (l.startsWith("#") || l.isBlank()) continue;
            String[] p = l.split("\t");
            if (p.length >= 2) CLASS_RENAMES.put(p[0], p[1]);
        }
        for (String l : Files.readAllLines(dir.resolve("mc-members.tsv"))) {
            if (l.startsWith("#") || l.isBlank()) continue;
            String[] p = l.split("\t");
            if (p.length >= 4) MEMBER_RENAMES.put(p[0] + "." + p[1], p[3]);
        }
    }

    private static void loadIndex(Path merged) throws IOException {
        try (ZipFile z = new ZipFile(merged.toFile())) {
            for (var en = z.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                if (!e.getName().endsWith(".class")) continue;
                try (InputStream in = z.getInputStream(e)) {
                    ClassNode n = new ClassNode();
                    new ClassReader(in).accept(n, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    Set<String> ms = new HashSet<>();
                    Set<String> fs = new HashSet<>();
                    for (MethodNode m : n.methods) ms.add(m.name);
                    for (FieldNode f : n.fields) fs.add(f.name);
                    MC_INDEX.put(n.name, new MC(n.name, n.superName, n.interfaces, ms, fs));
                }
            }
        }
    }

    private static boolean isMcName(String internal) {
        return internal.startsWith("net/minecraft/") || internal.startsWith("com/mojang/") || internal.startsWith("net/neoforged/");
    }

    private static boolean isForgeName(String internal) {
        return internal.startsWith("net/minecraftforge/");
    }

    private static boolean hasMember(String owner, String name, boolean field) {
        Set<String> seen = new HashSet<>();
        List<String> todo = new ArrayList<>(List.of(owner));
        while (!todo.isEmpty()) {
            String o = todo.remove(todo.size() - 1);
            if (o == null || !seen.add(o)) continue;
            MC c = MC_INDEX.get(o);
            if (c == null) {
                if (o.startsWith("java/")) return true; // not indexed: assume
                continue;
            }
            if ((field ? c.fields : c.methods).contains(name)) return true;
            todo.add(c.sup);
            todo.addAll(c.ifaces);
        }
        return false;
    }

    private static void triage(Path jar, Stats st) throws IOException {
        st.jars++;
        Map<String, ClassNode> classes = new HashMap<>();
        List<JsonObject> configs = new ArrayList<>();
        try (ZipFile z = new ZipFile(jar.toFile())) {
            for (var en = z.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                String name = e.getName();
                if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
                    try (InputStream in = z.getInputStream(e)) {
                        ClassNode n = new ClassNode();
                        new ClassReader(in).accept(n, ClassReader.SKIP_FRAMES);
                        classes.put(n.name, n);
                    }
                } else if (name.endsWith(".json") && !name.startsWith("assets/") && !name.startsWith("data/") && e.getSize() < 300_000 && !name.startsWith("META-INF/")) {
                    try (InputStream in = z.getInputStream(e)) {
                        String s = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                        if (!s.contains("\"package\"") || !s.contains("mixins") && !s.contains("\"client\"")) continue;
                        JsonElement el = JsonParser.parseString(s);
                        if (el.isJsonObject() && el.getAsJsonObject().has("package")) {
                            JsonObject o = el.getAsJsonObject();
                            o.addProperty("$name", name);
                            configs.add(o);
                        }
                    } catch (RuntimeException ignore) {
                    }
                }
            }
        }
        if (configs.isEmpty()) return;
        st.jarsWithMixins++;
        int jm = 0, jc = 0, jmem = 0;
        for (JsonObject cfg : configs) {
            st.configs++;
            if (cfg.has("plugin")) st.plugin++;
            String pkg = cfg.get("package").getAsString().replace('.', '/') + "/";
            for (String section : new String[] {"mixins", "client", "server"}) {
                if (!cfg.has(section) || !cfg.get(section).isJsonArray()) continue;
                for (JsonElement m : cfg.getAsJsonArray(section)) {
                    ClassNode node = classes.get(pkg + m.getAsString().replace('.', '/'));
                    if (node == null) continue;
                    st.mixins++;
                    jm++;
                    int[] r = checkMixin(node, st);
                    jc += r[0];
                    jmem += r[1];
                }
            }
        }
        st.perJar.add(jar.getFileName() + ": " + jm + " mixins, " + jc + " classes missing, " + jmem + " members unresolved");
    }

    /** returns {classes missing, members unresolved} */
    private static int[] checkMixin(ClassNode mixin, Stats st) {
        List<String> targets = new ArrayList<>();
        if (mixin.invisibleAnnotations != null) {
            for (AnnotationNode a : mixin.invisibleAnnotations) {
                if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(a.desc) || a.values == null) continue;
                for (int i = 0; i + 1 < a.values.size(); i += 2) {
                    String key = (String) a.values.get(i);
                    if (!(a.values.get(i + 1) instanceof List<?> list)) continue;
                    for (Object o : list) {
                        if (o instanceof Type t) targets.add(t.getInternalName());
                        else if (key.equals("targets")) targets.add(String.valueOf(o).replace('.', '/'));
                    }
                }
            }
        }
        int missingClasses = 0, missingMembers = 0;
        List<String> mcTargets = new ArrayList<>();
        for (String t : targets) {
            String t2 = CLASS_RENAMES.getOrDefault(t, t);
            if (!t2.equals(t)) st.classRenamed++;
            if (!isMcName(t2) && !isForgeName(t2)) continue;
            if (isForgeName(t2)) {
                missingClasses++;
                st.classMissing++;
                st.missingClass.merge(t2, 1, Integer::sum);
                continue;
            }
            mcTargets.add(t2);
            if (!MC_INDEX.containsKey(t2)) {
                missingClasses++;
                st.classMissing++;
                st.missingClass.merge(t2, 1, Integer::sum);
            }
        }
        if (!mcTargets.isEmpty()) st.mixinsMc++;
        else st.mixinsOther++;
        if (mcTargets.isEmpty()) return new int[] {missingClasses, 0};
        // members
        for (MethodNode m : mixin.methods) {
            missingMembers += checkAnnotations(m.visibleAnnotations, m.name, m.desc, mcTargets, st, false);
            missingMembers += checkAnnotations(m.invisibleAnnotations, m.name, m.desc, mcTargets, st, false);
        }
        for (FieldNode f : mixin.fields) {
            missingMembers += checkAnnotations(f.visibleAnnotations, f.name, f.desc, mcTargets, st, true);
            missingMembers += checkAnnotations(f.invisibleAnnotations, f.name, f.desc, mcTargets, st, true);
        }
        return new int[] {missingClasses, missingMembers};
    }

    private static int checkAnnotations(List<AnnotationNode> anns, String name, String desc, List<String> targets, Stats st, boolean field) {
        if (anns == null) return 0;
        int missing = 0;
        for (AnnotationNode a : anns) {
            String an = a.desc.substring(a.desc.lastIndexOf('/') + 1, a.desc.length() - 1);
            if (!INJECTORS.contains(an) || a.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/")) {
                if (!INJECTORS.contains(an)) continue;
            }
            List<String> names = new ArrayList<>();
            if (an.equals("Shadow")) {
                names.add(name);
                String alias = stringValue(a, "prefix");
                if (alias != null && name.startsWith(alias)) {
                    names.clear();
                    names.add(name.substring(alias.length()));
                }
            } else if (an.equals("Accessor") || an.equals("Invoker")) {
                String v = stringValue(a, "value");
                if (v != null && !v.isEmpty()) names.add(v);
                else {
                    String n = name;
                    if (n.startsWith("get") || n.startsWith("set") || n.startsWith("is") || n.startsWith("invoke") || n.startsWith("call")) {
                        n = n.replaceFirst("^(get|set|is|invoke|call)", "");
                        if (!n.isEmpty()) n = Character.toLowerCase(n.charAt(0)) + n.substring(1);
                    }
                    names.add(n);
                }
            } else {
                Object mv = value(a, "method");
                if (mv instanceof List<?> l) for (Object o : l) names.add(String.valueOf(o));
            }
            boolean isField = field || an.equals("Accessor") && desc.startsWith("()") == false && false;
            for (String n : names) {
                if (n.contains("func_") || n.contains("field_") || n.contains("m_") && n.matches(".*m_\\d+_.*")) {
                    st.srgNames++;
                    continue;
                }
                String mname = n;
                int p = mname.indexOf('(');
                if (p >= 0) mname = mname.substring(0, p);
                int semi = mname.indexOf(';');
                if (semi >= 0) mname = mname.substring(semi + 1);
                if (mname.equals("<init>") || mname.equals("<clinit>") || mname.equals("*") || mname.isEmpty() || mname.contains("*")) continue;
                st.memberChecks++;
                boolean fieldCheck = field || (an.equals("Accessor") && !targetHasMethod(targets, mname));
                boolean found = false;
                String key = null;
                for (String t : targets) {
                    String ren = MEMBER_RENAMES.getOrDefault(t + "." + mname, mname);
                    if (hasMember(t, ren, fieldCheck) || (an.equals("Accessor") && hasMember(t, ren, !fieldCheck))) found = true;
                    key = t + "." + mname;
                }
                if (!found) {
                    missing++;
                    st.memberMissing++;
                    st.missingMember.merge(key + (fieldCheck ? " (field)" : "") + " [" + an + "]", 1, Integer::sum);
                }
            }
            // @At targets
            Object at = value(a, "at");
            List<AnnotationNode> ats = new ArrayList<>();
            if (at instanceof AnnotationNode an1) ats.add(an1);
            else if (at instanceof List<?> l) for (Object o : l) if (o instanceof AnnotationNode x) ats.add(x);
            Object slice = value(a, "slice");
            for (AnnotationNode x : ats) {
                String target = stringValue(x, "target");
                if (target == null || target.isEmpty()) continue;
                String owner = target.startsWith("L") && target.indexOf(';') > 0 ? target.substring(1, target.indexOf(';')) : null;
                if (owner == null) continue;
                String rest = target.substring(target.indexOf(';') + 1);
                boolean isF = rest.contains(":") && !rest.contains("(");
                String mn = rest.contains("(") ? rest.substring(0, rest.indexOf('(')) : rest.contains(":") ? rest.substring(0, rest.indexOf(':')) : rest;
                if (!isMcName(CLASS_RENAMES.getOrDefault(owner, owner))) continue;
                String o2 = CLASS_RENAMES.getOrDefault(owner, owner);
                st.memberChecks++;
                if (!MC_INDEX.containsKey(o2)) {
                    st.atMissing++;
                    st.missingClass.merge(o2 + " (@At)", 1, Integer::sum);
                    missing++;
                } else {
                    String ren = MEMBER_RENAMES.getOrDefault(o2 + "." + mn, mn);
                    if (!hasMember(o2, ren, isF)) {
                        st.atMissing++;
                        st.memberMissing++;
                        missing++;
                        st.missingMember.merge(o2 + "." + mn + (isF ? " (field)" : "") + " [@At]", 1, Integer::sum);
                    }
                }
            }
        }
        return missing;
    }

    private static boolean targetHasMethod(List<String> targets, String name) {
        for (String t : targets) if (hasMember(t, name, false)) return true;
        return false;
    }

    private static Object value(AnnotationNode a, String key) {
        if (a.values == null) return null;
        for (int i = 0; i + 1 < a.values.size(); i += 2) if (key.equals(a.values.get(i))) return a.values.get(i + 1);
        return null;
    }

    private static String stringValue(AnnotationNode a, String key) {
        Object v = value(a, key);
        return v instanceof String s ? s : null;
    }
}
