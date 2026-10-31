package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * MOD-SPECIFIC FIX (CoFH Core: MineFactory Reloaded's grinder and slaughterhouse), kept isolated so it is easy to remove.
 * CoFH's coremod rewrites {@code skyboy/core/world/WorldServerShim} (and generates the bodies of its WorldProxy classes) from
 * Minecraft's own classes, which are not present here, so the shim keeps its stub constructor: it calls the
 * {@code WorldServer} constructor and then throws "WorldServerShim cannot be extended. Extend WorldServerProxy instead."
 * Creating the grinder's proxy world therefore failed. The throw is removed (the shim's {@code WorldServer} constructor
 * makes a view of the live level, which is what the proxy forwards to anyway).
 */
public final class CofhWorldProxyFix {
    private static final String CLASS = "skyboy/core/world/WorldServerShim";

    private CofhWorldProxyFix() {}

    public static int apply(ClassNode node) {
        if (!node.name.equals(CLASS)) return 0;
        int changed = 0;
        for (MethodNode m : node.methods) {
            if (!m.name.equals("<init>")) continue;
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof TypeInsnNode t && t.getOpcode() == Opcodes.NEW && t.desc.equals("java/lang/IllegalAccessError")) {
                    // NEW, DUP, LDC message, INVOKESPECIAL <init>, ATHROW -> RETURN
                    AbstractInsnNode end = insn;
                    while (end != null && end.getOpcode() != Opcodes.ATHROW) end = end.getNext();
                    if (end == null) break;
                    AbstractInsnNode after = end.getNext();
                    m.instructions.insertBefore(insn, new InsnNode(Opcodes.RETURN));
                    for (AbstractInsnNode i = insn; i != after;) {
                        AbstractInsnNode next = i.getNext();
                        m.instructions.remove(i);
                        i = next;
                    }
                    changed++;
                    break;
                }
            }
        }
        return changed;
    }
}
