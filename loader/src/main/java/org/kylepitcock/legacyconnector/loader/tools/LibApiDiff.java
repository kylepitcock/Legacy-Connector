package org.kylepitcock.legacyconnector.loader.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * API difference of one library between its 1.20.1 and 1.21.1 builds: public and protected classes, methods and fields of the old jar
 * that the new jar lacks (a method counts as present when the new class or one of its supers/interfaces in the new jar declares it).
 * Output: one line per symbol, {@code class|method|field TAB symbol}. Descriptors are compared as written (types of Minecraft/Forge are
 * not renamed here), so a changed parameter type shows as a missing method; read the output next to the Forge/Minecraft rename tables.
 * <p>usage: {@code LibApiDiff <old.jar> <new.jar> <package prefix, internal form, comma separated> [<report file or empty> [<class renames tsv>]]}
 */
public final class LibApiDiff {
    private final Map<String, ClassNode> oldC = new HashMap<>();
    private final Map<String, ClassNode> newC = new HashMap<>();
    /** Class renames (old to new internal name) applied to the old jar's names before comparing: the rows of a lib-classes table. */
    private final Map<String, String> renames = new HashMap<>();

    private String ren(String name) {
        return renames.getOrDefault(name, name);
    }

    private String renDesc(String desc) {
        if (renames.isEmpty() || desc.indexOf('L') < 0) return desc;
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < desc.length()) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                if (end > i) {
                    out.append('L').append(ren(desc.substring(i + 1, end))).append(';');
                    i = end + 1;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    public static void main(String[] args) throws IOException {
        LibApiDiff d = new LibApiDiff();
        d.read(Path.of(args[0]), d.oldC);
        d.read(Path.of(args[1]), d.newC);
        List<String> prefixes = List.of(args[2].split(","));
        if (args.length > 4) {
            for (String l : Files.readAllLines(Path.of(args[4]))) {
                String[] a = l.split("\t");
                if (a.length >= 2 && !l.startsWith("#")) d.renames.put(a[0], a[1]);
            }
        }
        List<String> out = d.diff(prefixes);
        if (args.length > 3 && !args[3].isEmpty()) Files.write(Path.of(args[3]), out);
        else out.forEach(System.out::println);
        System.err.println(out.size() + " symbols of " + args[0] + " missing in " + args[1]);
    }

    private void read(Path jar, Map<String, ClassNode> into) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                if (!e.getName().endsWith(".class") || e.getName().startsWith("META-INF/")) continue;
                try (InputStream in = zip.getInputStream(e)) {
                    ClassNode n = new ClassNode();
                    new ClassReader(in).accept(n, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    into.put(n.name, n);
                }
            }
        }
    }

    private static boolean api(int access) {
        return (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0 && (access & Opcodes.ACC_SYNTHETIC) == 0;
    }

    private List<String> diff(List<String> prefixes) {
        Set<String> out = new TreeSet<>();
        for (ClassNode o : oldC.values()) {
            if (!api(o.access) && (o.access & Opcodes.ACC_PUBLIC) == 0) continue;
            if (prefixes.stream().noneMatch(o.name::startsWith)) continue;
            ClassNode n = newC.get(ren(o.name));
            if (n == null) {
                out.add("class\t" + o.name);
                continue;
            }
            for (MethodNode m : o.methods) {
                if (!api(m.access) || m.name.equals("<clinit>") || m.name.startsWith("lambda$")) continue;
                if (!hasMethod(n, m.name, renDesc(m.desc), new HashSet<>())) out.add("method\t" + o.name + "." + m.name + m.desc);
            }
            for (FieldNode f : o.fields) {
                if (!api(f.access)) continue;
                if (!hasField(n, f.name, renDesc(f.desc), new HashSet<>())) out.add("field\t" + o.name + "." + f.name + ":" + f.desc);
            }
        }
        return new ArrayList<>(out);
    }

    private boolean hasMethod(ClassNode c, String name, String desc, Set<String> seen) {
        if (c == null || !seen.add(c.name)) return false;
        for (MethodNode m : c.methods) if (m.name.equals(name) && m.desc.equals(desc)) return true;
        if (hasMethod(newC.get(c.superName), name, desc, seen)) return true;
        for (String i : c.interfaces) if (hasMethod(newC.get(i), name, desc, seen)) return true;
        return false;
    }

    private boolean hasField(ClassNode c, String name, String desc, Set<String> seen) {
        if (c == null || !seen.add(c.name)) return false;
        for (FieldNode f : c.fields) if (f.name.equals(name) && f.desc.equals(desc)) return true;
        if (hasField(newC.get(c.superName), name, desc, seen)) return true;
        for (String i : c.interfaces) if (hasField(newC.get(i), name, desc, seen)) return true;
        return false;
    }
}
