package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** AM2 predicts the flip on the client and sends a toggle packet. The server's reconstructed jump must not toggle it again. */
final class Am2GravityFix {
    private static final String MC = "legacyconnector/v1_7_10/net/minecraft/";

    private Am2GravityFix() {}

    static void apply(ClassNode node) {
        if (!node.name.equals("am2/AMEventHandler")) return;
        for (var method : node.methods) {
            if (!method.name.equals("onEntityJump") || !method.desc.endsWith("/LivingEvent$LivingJumpEvent;)V")) continue;
            for (var instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode sneaking) || !sneaking.name.equals("isSneaking") || !sneaking.desc.equals("()Z")) continue;
                if (!(sneaking.getPrevious() instanceof FieldInsnNode living) || !living.name.equals("entityLiving")) continue;
                if (!(sneaking.getNext() instanceof JumpInsnNode skip) || skip.getOpcode() != Opcodes.IFEQ) continue;
                var guard = new InsnList();
                guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
                guard.add(new FieldInsnNode(Opcodes.GETFIELD, living.owner, living.name, living.desc));
                guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MC + "entity/Entity", "mcc$get$worldObj",
                        "(L" + MC + "entity/Entity;)L" + MC + "world/World;", false));
                guard.add(new FieldInsnNode(Opcodes.GETFIELD, MC + "world/World", "isRemote", "Z"));
                guard.add(new JumpInsnNode(Opcodes.IFEQ, skip.label));
                method.instructions.insert(skip, guard);
                return;
            }
        }
    }
}
