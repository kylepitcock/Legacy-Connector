package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Forge 1.5.2 and 1.6.4 mods cast their class loader to FML's {@code RelaunchClassLoader}
 * ({@code (RelaunchClassLoader) Foo.class.getClassLoader()}). The game's loader is no such class, so the cast would fail; the shim's stand-in
 * (a class of the version's own shim that wraps a loader) replaces the cast with {@code RelaunchClassLoader.wrap(Object)}, which keeps the stack
 * shape (one reference in, one reference of the target type out).
 */
public final class ClassLoaderCasts {
    private ClassLoaderCasts() {}

    private static final String RELAUNCH = "cpw/mods/fml/relauncher/RelaunchClassLoader";

    public static int apply(ClassNode node, Hierarchy h, LegacyVersion version) {
        if (version.forward() == null) return 0;
        String target = version.shimPrefix() + RELAUNCH;
        if (!h.isShim(target)) return 0;
        int count = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.CHECKCAST && insn instanceof TypeInsnNode t && t.desc.equals(target)) {
                    MethodInsnNode wrap = new MethodInsnNode(Opcodes.INVOKESTATIC, target, "wrap", "(Ljava/lang/Object;)L" + target + ";", false);
                    method.instructions.set(t, wrap);
                    insn = wrap;
                    count++;
                }
            }
        }
        return count;
    }
}
