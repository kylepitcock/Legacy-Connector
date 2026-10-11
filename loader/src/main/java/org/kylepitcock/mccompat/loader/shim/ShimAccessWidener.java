package org.kylepitcock.mccompat.loader.shim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Build-time step over the compiled shim ({@code mccompat/**}): every field, non-private method and class becomes public,
 * and methods/classes lose {@code final}. Legacy mods were routinely built with FML access transformers that did
 * exactly this to Minecraft classes, and the shim lives in another module, so a protected member called from a mod
 * would otherwise fail with IllegalAccessError. Private members (runtime internals) are untouched.
 *
 * <p>usage: {@code ShimAccessWidener <classes dir>...}</p>
 */
public final class ShimAccessWidener {
    private ShimAccessWidener() {}

    public static void main(String[] args) throws IOException {
        int changed = 0;
        for (String dir : args) {
            Path root = Path.of(dir).resolve("mccompat");
            if (!Files.isDirectory(root)) continue;
            List<Path> files;
            try (Stream<Path> walk = Files.walk(root)) {
                files = walk.filter(p -> p.toString().endsWith(".class")).toList();
            }
            for (Path p : files) {
                byte[] in = Files.readAllBytes(p);
                ClassNode node = new ClassNode();
                new ClassReader(in).accept(node, 0);
                if (!widen(node)) continue;
                ClassWriter cw = new ClassWriter(0);
                node.accept(cw);
                Files.write(p, cw.toByteArray());
                changed++;
            }
        }
        System.out.println("Widened access in " + changed + " shim classes");
    }

    static boolean widen(ClassNode node) {
        boolean changed = false;
        int a = publicNonFinal(node.access, true);
        if (a != node.access) {
            node.access = a;
            changed = true;
        }
        for (MethodNode m : node.methods) {
            int w = publicNonFinal(m.access, (node.access & Opcodes.ACC_ENUM) == 0);
            if (w != m.access) {
                m.access = w;
                changed = true;
            }
        }
        for (FieldNode f : node.fields) {
            // Static fields too: mods replaced vanilla singletons (Blocks.quartz_ore = ...) through ATs. Constants
            // keep their ConstantValue attribute, so compiled-in values are unaffected.
            boolean isStatic = (f.access & Opcodes.ACC_STATIC) != 0 && (node.access & Opcodes.ACC_INTERFACE) == 0
                    && (f.access & Opcodes.ACC_ENUM) == 0 && f.value == null;
            // Private fields too: legacy fields mods reached through access transformers (Block.unlocalizedName, ...).
            // Unlike methods, a widened field cannot change dispatch; subclasses can only shadow it.
            int fa = (f.access & Opcodes.ACC_SYNTHETIC) == 0 ? f.access & ~Opcodes.ACC_PRIVATE : f.access;
            int w = publicNonFinal(fa, isStatic);
            if (w != f.access) {
                f.access = w;
                changed = true;
            }
        }
        for (InnerClassNode ic : node.innerClasses) {
            if (!ic.name.startsWith("mccompat/")) continue;
            int w = publicNonFinal(ic.access, true);
            if (w != ic.access) {
                ic.access = w;
                changed = true;
            }
        }
        return changed;
    }

    private static int publicNonFinal(int access, boolean dropFinal) {
        if ((access & Opcodes.ACC_PRIVATE) != 0 || (access & Opcodes.ACC_SYNTHETIC) != 0) return access;
        access = (access & ~Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
        if (dropFinal && (access & Opcodes.ACC_ENUM) == 0) access &= ~Opcodes.ACC_FINAL;
        return access;
    }
}
