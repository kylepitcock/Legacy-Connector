package org.kylepitcock.legacyconnector.loader.shim;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Build-time tool: {@code LegacyApiStubGenerator <classes dir> <shim prefix e.g. legacyconnector/v1_7_10/> <output jar>}.
 *
 * <p>Reverses the shim relocation to produce a compile-only jar that looks like the original legacy API
 * ({@code cpw.mods.fml...}, {@code net.minecraft...}) as seen by a Java 8 compiler. Test fixture mods are
 * compiled against it, giving byte-for-byte realistic legacy jars without redistributing Mojang code.
 * Bridge members ({@code mcc$...}) and anything that mentions modern types are omitted.</p>
 */
public final class LegacyApiStubGenerator {
    private LegacyApiStubGenerator() {}

    public static void main(String[] args) throws IOException {
        Path classes = Path.of(args[0]);
        String prefix = args[1];
        Path output = Path.of(args[2]);
        Path root = classes.resolve(prefix);
        shimPrefix = prefix;
        Remapper unrelocate = new Remapper() {
            @Override
            public String map(String name) {
                return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
            }
        };
        Files.createDirectories(output.getParent());
        int count = 0;
        try (OutputStream os = Files.newOutputStream(output); JarOutputStream jar = new JarOutputStream(os, new Manifest());
             Stream<Path> walk = Files.walk(root)) {
            for (Path p : (Iterable<Path>) walk::iterator) {
                if (!p.toString().endsWith(".class")) continue;
                // Filter on the relocated names (where legacy and modern classes are distinguishable, e.g. both
                // worlds have a net.minecraft.client.Minecraft), then strip the prefix.
                ClassNode node = new ClassNode();
                try (InputStream in = Files.newInputStream(p)) {
                    new ClassReader(in).accept(node, ClassReader.SKIP_FRAMES);
                }
                if (!stub(node)) continue;
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                node.accept(new ClassRemapper(cw, unrelocate));
                ZipEntry entry = new ZipEntry(unrelocate.map(node.name) + ".class");
                entry.setTime(0);
                jar.putNextEntry(entry);
                jar.write(cw.toByteArray());
                jar.closeEntry();
                count++;
            }
        }
        System.out.println("Wrote " + count + " legacy API stubs -> " + output);
    }

    private static boolean stub(ClassNode node) {
        if (!isLegacyVisible(node.superName)) node.superName = "java/lang/Object";
        // Shim classes may extend the real library class they replace (GL11 extends LWJGL 3's GL11); after
        // un-relocation that would be a self-reference.
        if (node.superName != null && node.name.startsWith(shimPrefix) && node.superName.equals(node.name.substring(shimPrefix.length()))) {
            node.superName = "java/lang/Object";
        }
        node.interfaces.removeIf(i -> !isLegacyVisible(i));
        node.version = Opcodes.V1_8;
        node.nestHostClass = null;
        node.nestMembers = null;
        node.permittedSubclasses = null;
        node.recordComponents = null;
        node.module = null;
        if (node.signature != null && mentionsModern(node.signature)) node.signature = null;
        node.visibleAnnotations = filterAnnotations(node.visibleAnnotations);
        node.invisibleAnnotations = filterAnnotations(node.invisibleAnnotations);

        List<FieldNode> fields = new ArrayList<>();
        for (FieldNode f : node.fields) {
            if (f.name.startsWith("mcc$") || (f.access & Opcodes.ACC_PRIVATE) != 0 || mentionsModern(f.desc)) continue;
            if (f.signature != null && mentionsModern(f.signature)) f.signature = null;
            f.visibleAnnotations = filterAnnotations(f.visibleAnnotations);
            f.invisibleAnnotations = filterAnnotations(f.invisibleAnnotations);
            fields.add(f);
        }
        node.fields = fields;

        List<MethodNode> methods = new ArrayList<>();
        for (MethodNode m : node.methods) {
            if (m.name.startsWith("mcc$") || m.name.equals("<clinit>") || (m.access & Opcodes.ACC_PRIVATE) != 0
                    || (m.access & Opcodes.ACC_SYNTHETIC) != 0 || mentionsModern(m.desc)) {
                continue;
            }
            if (m.signature != null && mentionsModern(m.signature)) m.signature = null;
            m.visibleAnnotations = filterAnnotations(m.visibleAnnotations);
            m.invisibleAnnotations = filterAnnotations(m.invisibleAnnotations);
            m.visibleParameterAnnotations = null;
            m.invisibleParameterAnnotations = null;
            m.localVariables = null;
            m.tryCatchBlocks = new ArrayList<>();
            m.exceptions = m.exceptions == null ? null : new ArrayList<>(m.exceptions.stream().filter(LegacyApiStubGenerator::isLegacyVisible).toList());
            if ((m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                InsnList body = new InsnList();
                body.add(new InsnNode(Opcodes.ACONST_NULL));
                body.add(new InsnNode(Opcodes.ATHROW));
                m.instructions = body;
            }
            methods.add(m);
        }
        node.methods = methods;
        node.innerClasses.removeIf(ic -> !isLegacyVisible(ic.name));
        return true;
    }

    private static List<org.objectweb.asm.tree.AnnotationNode> filterAnnotations(List<org.objectweb.asm.tree.AnnotationNode> list) {
        if (list == null) return null;
        list.removeIf(a -> mentionsModern(a.desc));
        return list;
    }

    /** True if a descriptor or generic signature references a type a legacy compiler should not see. */
    static boolean mentionsModern(String descOrSig) {
        boolean[] modern = new boolean[1];
        org.objectweb.asm.signature.SignatureVisitor v = new org.objectweb.asm.signature.SignatureVisitor(Opcodes.ASM9) {
            @Override
            public void visitClassType(String name) {
                if (!isLegacyVisible(name)) modern[0] = true;
            }
        };
        if (descOrSig.startsWith("(") || descOrSig.startsWith("<")) {
            new org.objectweb.asm.signature.SignatureReader(descOrSig).accept(v);
        } else {
            new org.objectweb.asm.signature.SignatureReader(descOrSig).acceptType(v);
        }
        return modern[0];
    }

    private static String shimPrefix = "legacyconnector/";

    static boolean isLegacyVisible(String internalName) {
        if (internalName == null) return true;
        if (internalName.startsWith(shimPrefix)) return true;
        return internalName.startsWith("java/") || internalName.startsWith("javax/") || internalName.startsWith("com/google/")
                || internalName.startsWith("io/netty/") || internalName.startsWith("org/apache/") || internalName.startsWith("org/lwjgl/")
                || internalName.startsWith("com/mojang/authlib/");
    }

}
