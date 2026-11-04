package org.kylepitcock.legacyconnector.loader.tools;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Milestone 0 of 1.20.1 support: the gap report. Takes Forge 1.20.1 mod jars (SRG-named), renames what the layer would rename
 * (SRG ids to Mojang names, Forge packages to NeoForge) and checks every class, method and field reference against the real
 * Minecraft 1.21.1 + NeoForge jars, the JDK, the other jars of the set and the libraries on the loader's class path. What is left is the
 * work list: symbols that exist in 1.20.1 and not in 1.21.1 (moved, renamed, removed or changed in signature).
 *
 * <p>usage: Gap1201 &lt;srg-table.tsv&gt; &lt;rename-tsv,rename-tsv&gt; &lt;target.jar[,target.jar]&gt; &lt;report.txt&gt; &lt;mod.jar&gt;...
 */
public final class Gap1201 {
    /** Structure of one class, from a target jar or a mod. */
    private static final class Cls {
        String name;
        String superName;
        String[] interfaces;
        final Set<String> methods = new HashSet<>();
        final Set<String> fields = new HashSet<>();
    }

    private final Map<String, Cls> target = new HashMap<>();
    private final Map<String, String> srg = new HashMap<>();
    private final Map<String, String> classRenames = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Gap1201 gap = new Gap1201();
        gap.loadSrg(Path.of(args[0]));
        for (String f : args[1].split(",")) gap.loadRenames(Path.of(f));
        for (String j : args[2].split(",")) gap.index(Path.of(j), gap.target);
        List<Path> mods = new ArrayList<>();
        for (int i = 4; i < args.length; i++) mods.add(Path.of(args[i]));
        gap.run(mods, Path.of(args[3]));
    }

    private void loadSrg(Path p) throws IOException {
        try (Stream<String> lines = Files.lines(p)) {
            lines.forEach(l -> {
                int t = l.indexOf('\t');
                if (t > 0) srg.put(l.substring(0, t), l.substring(t + 1));
            });
        }
    }

    private void loadRenames(Path p) throws IOException {
        try (Stream<String> lines = Files.lines(p)) {
            lines.filter(l -> !l.startsWith("#")).forEach(l -> {
                String[] a = l.split("\t");
                if (a.length == 2) classRenames.put(a[0], a[1]);
            });
        }
    }

    private static void index(Path jar, Map<String, Cls> into) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (var en = zip.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                if (!e.getName().endsWith(".class") || e.getName().startsWith("META-INF/versions/")) continue;
                try {
                    ClassNode n = new ClassNode();
                    new ClassReader(zip.getInputStream(e).readAllBytes()).accept(n, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    Cls c = new Cls();
                    c.name = n.name;
                    c.superName = n.superName;
                    c.interfaces = n.interfaces.toArray(new String[0]);
                    n.methods.forEach(m -> c.methods.add(m.name + m.desc));
                    n.fields.forEach(f -> c.fields.add(f.name + ":" + f.desc));
                    into.putIfAbsent(n.name, c);
                } catch (RuntimeException | IOException ignored) {
                    // unreadable entry
                }
            }
        }
    }

    private Cls jdk(String name) {
        if (!(name.startsWith("java/") || name.startsWith("javax/") || name.startsWith("jdk/") || name.startsWith("sun/"))) return null;
        try {
            Class<?> k = Class.forName(name.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
            Cls c = new Cls();
            c.name = name;
            c.superName = k.getSuperclass() == null ? null : k.getSuperclass().getName().replace('.', '/');
            c.interfaces = java.util.Arrays.stream(k.getInterfaces()).map(i -> i.getName().replace('.', '/')).toArray(String[]::new);
            for (var m : k.getDeclaredMethods()) c.methods.add(m.getName() + Type.getMethodDescriptor(m));
            for (var m : k.getDeclaredConstructors()) c.methods.add("<init>" + Type.getConstructorDescriptor(m));
            for (var f : k.getDeclaredFields()) c.fields.add(f.getName() + ":" + Type.getDescriptor(f.getType()));
            return c;
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    // ---- renaming -------------------------------------------------------------------------------------------------------------

    private String cls(String name) {
        if (name == null) return null;
        if (name.startsWith("[")) return desc(name);
        String r = classRenames.get(name);
        if (r != null) return r;
        int dollar = name.indexOf('$');
        if (dollar > 0) {
            String outer = classRenames.get(name.substring(0, dollar));
            if (outer != null) return outer + name.substring(dollar);
        }
        // the Forge to NeoForge package move for what the tables do not list
        if (name.startsWith("net/minecraftforge/") && !name.startsWith("net/minecraftforge/fml/")) {
            String moved = "net/neoforged/neoforge/" + name.substring("net/minecraftforge/".length());
            if (target.containsKey(moved)) return moved;
        }
        return name;
    }

    private String desc(String d) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            char ch = d.charAt(i);
            if (ch == 'L') {
                int end = d.indexOf(';', i);
                sb.append('L').append(cls(d.substring(i + 1, end))).append(';');
                i = end;
            } else sb.append(ch);
        }
        return sb.toString();
    }

    private String member(String name) {
        String m = srg.get(name);
        return m == null ? name : m;
    }

    // ---- checking -------------------------------------------------------------------------------------------------------------

    private Cls find(String name, Map<String, Cls> own) {
        Cls c = own.get(name);
        if (c == null) c = target.get(name);
        return c != null ? c : jdk(name);
    }

    private boolean hasMember(String owner, String key, boolean method, Map<String, Cls> own, Set<String> seen) {
        if (owner == null || !seen.add(owner)) return false;
        Cls c = find(owner, own);
        if (c == null) return false;
        if ((method ? c.methods : c.fields).contains(key)) return true;
        if (hasMember(c.superName, key, method, own, seen)) return true;
        if (c.interfaces != null) for (String i : c.interfaces) if (hasMember(i, key, method, own, seen)) return true;
        return method && owner.equals(c.name) && c.superName == null && hasMember("java/lang/Object", key, true, own, seen);
    }

    private final Map<String, Set<String>> missingClasses = new TreeMap<>();
    private final Map<String, Set<String>> missingMethods = new TreeMap<>();
    private final Map<String, Set<String>> missingFields = new TreeMap<>();

    private void run(List<Path> mods, Path report) throws IOException {
        Map<String, Cls> own = new HashMap<>();
        for (Path m : mods) index(m, own);
        PrintStream out = new PrintStream(Files.newOutputStream(report));
        out.println("Gap report: Forge 1.20.1 jars against Minecraft 1.21.1 + NeoForge (SRG renamed, Forge packages renamed)\n");
        for (Path mod : mods) {
            Set<String> mc = new TreeSet<>(), mm = new TreeSet<>(), mf = new TreeSet<>();
            int refs = 0;
            try (ZipFile zip = new ZipFile(mod.toFile())) {
                for (var en = zip.entries(); en.hasMoreElements(); ) {
                    ZipEntry e = en.nextElement();
                    if (!e.getName().endsWith(".class") || e.getName().startsWith("META-INF/")) continue;
                    ClassNode n = new ClassNode();
                    try {
                        new ClassReader(zip.getInputStream(e).readAllBytes()).accept(n, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    } catch (RuntimeException ex) {
                        continue;
                    }
                    refs += check(n, own, mc, mm, mf);
                }
            }
            String jar = mod.getFileName().toString();
            out.printf("%s: %d references, %d missing classes, %d missing methods, %d missing fields%n", jar, refs, mc.size(), mm.size(), mf.size());
            mc.forEach(s -> missingClasses.computeIfAbsent(s, k -> new TreeSet<>()).add(jar));
            mm.forEach(s -> missingMethods.computeIfAbsent(s, k -> new TreeSet<>()).add(jar));
            mf.forEach(s -> missingFields.computeIfAbsent(s, k -> new TreeSet<>()).add(jar));
        }
        ranked(out, "Missing classes (by number of jars)", missingClasses);
        ranked(out, "Missing methods", missingMethods);
        ranked(out, "Missing fields", missingFields);
        out.close();
        System.out.println("report: " + report + " (" + missingClasses.size() + " classes, " + missingMethods.size() + " methods, " + missingFields.size() + " fields)");
    }

    private static void ranked(PrintStream out, String title, Map<String, Set<String>> map) {
        out.println("\n== " + title + ": " + map.size() + " distinct ==");
        map.entrySet().stream().sorted(Comparator.comparingInt((Map.Entry<String, Set<String>> e) -> -e.getValue().size()).thenComparing(Map.Entry::getKey))
                .limit(400).forEach(e -> out.println(String.format("%3d  %s", e.getValue().size(), e.getKey())));
    }

    private int check(ClassNode n, Map<String, Cls> own, Set<String> mc, Set<String> mm, Set<String> mf) {
        int[] refs = {0};
        var type = (java.util.function.Consumer<String>) t -> {
            if (t == null) return;
            refs[0]++;
            String name = t;
            while (name.startsWith("[")) name = name.substring(1);
            if (name.startsWith("L") && name.endsWith(";")) name = name.substring(1, name.length() - 1);
            else if (name.length() == 1) return;
            String r = cls(name);
            if (find(r, own) == null && !isLibrary(r)) mc.add(r);
        };
        type.accept(n.superName);
        if (n.interfaces != null) n.interfaces.forEach(type);
        for (MethodNode m : n.methods) {
            descTypes(m.desc, type);
            if (m.instructions == null) continue;
            for (AbstractInsnNode in : m.instructions) {
                if (in instanceof MethodInsnNode mi) {
                    refs[0]++;
                    String owner = cls(mi.owner), name = member(mi.name), d = desc(mi.desc);
                    descTypes(mi.desc, type);
                    if (owner.startsWith("[")) continue;
                    type.accept(mi.owner);
                    Cls c = find(owner, own);
                    if (c == null) continue; // reported as a missing class
                    if (!hasMember(owner, name + d, true, own, new HashSet<>())) mm.add(owner + "." + name + d);
                } else if (in instanceof FieldInsnNode fi) {
                    refs[0]++;
                    String owner = cls(fi.owner), name = member(fi.name), d = desc(fi.desc);
                    type.accept(fi.owner);
                    Cls c = find(owner, own);
                    if (c == null) continue;
                    if (!hasMember(owner, name + ":" + d, false, own, new HashSet<>())) mf.add(owner + "." + name + ":" + d);
                } else if (in instanceof TypeInsnNode ti) {
                    type.accept(ti.desc);
                } else if (in instanceof MultiANewArrayInsnNode ma) {
                    type.accept(ma.desc);
                } else if (in instanceof LdcInsnNode l && l.cst instanceof Type t && t.getSort() == Type.OBJECT) {
                    type.accept(t.getInternalName());
                } else if (in instanceof InvokeDynamicInsnNode) {
                    refs[0]++;
                }
            }
        }
        return refs[0];
    }

    private static void descTypes(String desc, java.util.function.Consumer<String> type) {
        for (Type t : Type.getArgumentTypes(desc)) addType(t, type);
        addType(Type.getReturnType(desc), type);
    }

    private static void addType(Type t, java.util.function.Consumer<String> type) {
        if (t.getSort() == Type.ARRAY) t = t.getElementType();
        if (t.getSort() == Type.OBJECT) type.accept(t.getInternalName());
    }

    /** Libraries a mod gets from the game at run time that this offline tool does not index (annotations, logging, Guava, Gson...). */
    private static boolean isLibrary(String name) {
        return name.startsWith("com/google/") || name.startsWith("org/slf4j/") || name.startsWith("org/apache/") || name.startsWith("io/netty/")
                || name.startsWith("org/lwjgl/") || name.startsWith("org/joml/") || name.startsWith("it/unimi/") || name.startsWith("com/mojang/")
                || name.startsWith("org/jetbrains/") || name.startsWith("org/objectweb/") || name.startsWith("org/spongepowered/") || name.startsWith("javax/")
                || name.startsWith("com/electronwill/") || name.startsWith("org/openjdk/") || name.startsWith("net/neoforged/fml/") || name.startsWith("net/neoforged/bus/")
                || name.startsWith("net/neoforged/neoforgespi/");
    }
}
