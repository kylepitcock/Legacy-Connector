package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** The legacy modifier looked up by UUID can be a new wrapper around the modern attribute modifier. */
final class Am2BuffCleanupFix {
    private Am2BuffCleanupFix() {}

    static void apply(ClassNode node) {
        if (!node.name.equals("am2/buffs/BuffStatModifiers")) return;
        for (var method : node.methods) {
            if (!method.name.equals("clearAllModifiers")) continue;
            for (var instruction : method.instructions) {
                if (!(instruction instanceof JumpInsnNode jump) || jump.getOpcode() != Opcodes.IF_ACMPNE) continue;
                if (!(jump.getPrevious() instanceof MethodInsnNode expected) || !expected.owner.equals("am2/utility/KeyValuePair")
                        || !expected.name.equals("getValue") || !expected.desc.equals("()Ljava/lang/Object;")) continue;
                if (!(expected.getPrevious() instanceof VarInsnNode pair) || pair.getOpcode() != Opcodes.ALOAD) continue;
                // getModifier already matched the UUID. Remove the returned modifier if present.
                method.instructions.remove(pair);
                method.instructions.remove(expected);
                jump.setOpcode(Opcodes.IFNULL);
                return;
            }
        }
    }
}
