package org.kylepitcock.legacyconnector.loader.tools;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * Writes {@code legacyconnector/forward/1.20.1/mc-members.index.gz}: the declared members of every Minecraft 1.21.1 and NeoForge class (the same
 * classes as {@code mc-hierarchy.tsv.gz}), one line per class: {@code owner TAB name+desc SPACE name+desc... TAB name:desc SPACE name:desc...}.
 * {@link org.kylepitcock.legacyconnector.loader.transform.SoftLink1201} uses it to tell which references of a mod cannot link. Next to it {@code mc-static.index.gz}
 * lists the static methods only ({@code owner TAB name+desc SPACE name+desc...}), which the mixin repair needs (a handler must be static exactly when its target is).
 *
 * <p>usage: {@code java -cp asm.jar:asm-tree.jar McMembers1201Tool.java <neoforge-merged.jar> [out.gz]}
 */
public final class McMembers1201Tool {
    private McMembers1201Tool() {}

    private static final String[] PREFIXES = {"net/minecraft/", "net/neoforged/neoforge/", "com/mojang/blaze3d/", "com/mojang/math/"};

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 1 ? args[1] : "loader/src/main/resources/legacyconnector/forward/1.20.1/mc-members.index.gz");
        TreeMap<String, String> lines = new TreeMap<>();
        TreeMap<String, String> statics = new TreeMap<>();
        try (ZipFile zip = new ZipFile(args[0])) {
            for (var en = zip.entries(); en.hasMoreElements(); ) {
                var e = en.nextElement();
                String name = e.getName();
                if (!name.endsWith(".class")) continue;
                boolean ok = false;
                for (String p : PREFIXES) if (name.startsWith(p)) ok = true;
                if (!ok) continue;
                ClassNode n = new ClassNode();
                new ClassReader(zip.getInputStream(e).readAllBytes()).accept(n, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                StringBuilder ms = new StringBuilder();
                for (var m : n.methods) ms.append(ms.length() > 0 ? " " : "").append(m.name).append(m.desc);
                StringBuilder fs = new StringBuilder();
                for (var f : n.fields) fs.append(fs.length() > 0 ? " " : "").append(f.name).append(':').append(f.desc);
                lines.put(n.name, n.name + "\t" + ms + "\t" + fs);
                StringBuilder ss = new StringBuilder();
                for (var m : n.methods) if ((m.access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0) ss.append(ss.length() > 0 ? " " : "").append(m.name).append(m.desc);
                if (ss.length() > 0) statics.put(n.name, n.name + "\t" + ss);
            }
        }
        try (OutputStream o = new GZIPOutputStream(Files.newOutputStream(out))) {
            o.write(String.join("\n", lines.values()).getBytes(StandardCharsets.UTF_8));
        }
        try (OutputStream o = new GZIPOutputStream(Files.newOutputStream(out.resolveSibling("mc-static.index.gz")))) {
            o.write(String.join("\n", statics.values()).getBytes(StandardCharsets.UTF_8));
        }
        System.out.println(lines.size() + " classes -> " + out + " (" + Files.size(out) + " bytes)");
    }
}
