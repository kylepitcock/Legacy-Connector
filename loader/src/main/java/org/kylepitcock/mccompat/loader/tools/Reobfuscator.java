package org.kylepitcock.mccompat.loader.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

/**
 * Developer tool: {@code Reobfuscator <in.jar> <legacy-x.sig> <out.jar>}. Renames MCP member names to SRG ids
 * exactly like ForgeGradle's {@code reobf} did for released legacy mods, using the signature table produced by
 * {@code tools/extract_legacy_signatures.py}. Used to turn test fixtures into realistic legacy release jars.
 */
public final class Reobfuscator {
    private record Member(String owner, String name, String desc) {}

    private final Map<String, String> supers = new HashMap<>();
    private final Map<String, List<String>> interfaces = new HashMap<>();
    private final Map<Member, String> methods = new HashMap<>();
    private final Map<String, String> fields = new HashMap<>(); // owner.name -> srg

    private Reobfuscator() {}

    public static void main(String[] args) throws IOException {
        Reobfuscator r = new Reobfuscator();
        r.loadSig(Path.of(args[1]));
        r.run(Path.of(args[0]), Path.of(args[2]));
    }

    private void loadSig(Path sig) throws IOException {
        String cls = null;
        for (String line : Files.readAllLines(sig)) {
            String[] p = line.split(" ");
            switch (p[0]) {
                case "C" -> {
                    cls = p[1];
                    supers.put(cls, p[2].equals("-") ? null : p[2]);
                    interfaces.put(cls, p[3].equals("-") ? List.of() : List.of(p[3].split(",")));
                }
                case "M" -> {
                    if (p[4].startsWith("func_")) methods.put(new Member(cls, p[1], p[2]), p[4]);
                }
                case "F" -> {
                    if (p[4].startsWith("field_")) fields.put(cls + "." + p[1], p[4]);
                }
                default -> { }
            }
        }
    }

    private void run(Path in, Path out) throws IOException {
        Map<String, byte[]> classes = new HashMap<>();
        Map<String, byte[]> other = new HashMap<>();
        try (ZipFile zip = new ZipFile(in.toFile())) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                try (InputStream s = zip.getInputStream(e)) {
                    byte[] data = s.readAllBytes();
                    if (e.getName().endsWith(".class")) classes.put(e.getName(), data);
                    else other.put(e.getName(), data);
                }
            }
        }
        // Mod classes join the hierarchy so inherited/overridden members resolve to their legacy declaration.
        for (byte[] data : classes.values()) {
            ClassReader r = new ClassReader(data);
            supers.put(r.getClassName(), r.getSuperName());
            interfaces.put(r.getClassName(), List.of(r.getInterfaces()));
        }
        Remapper remapper = new Remapper() {
            @Override
            public String mapMethodName(String owner, String name, String desc) {
                String srg = findMethod(owner, name, desc, new ArrayList<>());
                return srg == null ? name : srg;
            }

            @Override
            public String mapFieldName(String owner, String name, String desc) {
                for (String c = owner; c != null; c = supers.get(c)) {
                    String srg = fields.get(c + "." + name);
                    if (srg != null) return srg;
                }
                return name;
            }
        };
        Files.createDirectories(out.getParent());
        try (OutputStream os = Files.newOutputStream(out); JarOutputStream jar = new JarOutputStream(os)) {
            for (var e : other.entrySet()) put(jar, e.getKey(), e.getValue());
            for (var e : classes.entrySet()) {
                ClassNode node = new ClassNode();
                new ClassReader(e.getValue()).accept(new ClassRemapper(node, remapper), 0);
                ClassWriter w = new ClassWriter(0);
                node.accept(w);
                put(jar, e.getKey(), w.toByteArray());
            }
        }
        System.out.println("Reobfuscated " + classes.size() + " classes -> " + out);
    }

    private String findMethod(String owner, String name, String desc, List<String> seen) {
        if (owner == null || seen.contains(owner)) return null;
        seen.add(owner);
        String srg = methods.get(new Member(owner, name, desc));
        if (srg != null) return srg;
        String s = findMethod(supers.get(owner), name, desc, seen);
        if (s != null) return s;
        for (String i : interfaces.getOrDefault(owner, List.of())) {
            s = findMethod(i, name, desc, seen);
            if (s != null) return s;
        }
        return null;
    }

    private static void put(JarOutputStream jar, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0);
        jar.putNextEntry(entry);
        jar.write(data);
        jar.closeEntry();
    }
}
