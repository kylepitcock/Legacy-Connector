package org.kylepitcock.legacyconnector.loader.transform.compat;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * MOD-SPECIFIC FIX (library mods with a "fatal error screen"), kept isolated so it is easy to remove: on a dedicated server such a
 * screen logs the problem and then sleeps forever in a static {@code sleepForever()}, which hangs the whole startup on a plain
 * configuration problem (two mods configured with the same block id). The method throws instead, so the mod fails to load
 * with its own message in the log and the rest of the pack continues.
 */
public final class FatalErrorScreenFix {
    private FatalErrorScreenFix() {}

    public static int apply(ClassNode node) {
        int changed = 0;
        for (MethodNode m : node.methods) {
            if (!m.name.equals("sleepForever") || !m.desc.equals("()V") || (m.access & Opcodes.ACC_STATIC) == 0) continue;
            InsnList code = new InsnList();
            code.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalStateException"));
            code.add(new InsnNode(Opcodes.DUP));
            code.add(new LdcInsnNode("Fatal error screen of a legacy mod (see the log lines above); stopping this mod instead of waiting forever"));
            code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V", false));
            code.add(new InsnNode(Opcodes.ATHROW));
            m.instructions = code;
            m.tryCatchBlocks.clear();
            m.localVariables = null;
            m.maxStack = 3;
            changed++;
        }
        return changed;
    }
}
