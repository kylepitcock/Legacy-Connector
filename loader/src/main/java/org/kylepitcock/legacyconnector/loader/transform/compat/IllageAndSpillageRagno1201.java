package org.kylepitcock.legacyconnector.loader.transform.compat;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ClassNode;

/** Keeps RagnoEntity's initial stun health independent of entityData during Entity construction. */
public final class IllageAndSpillageRagno1201 implements Opcodes {
    public static final String RAGNO = "com/yellowbrossproductions/illageandspillage/entities/RagnoEntity";
    private static final String DEFINE_DATA = "defineSynchedData";
    private static final String MAX_STUN_HEALTH = "getMaxStunHealth";

    private IllageAndSpillageRagno1201() {}

    /** Replaces the constructor-time getter with its value after CRAZY is defined as false. */
    public static int apply(ClassNode node) {
        if (!RAGNO.equals(node.name)) return 0;

        MethodNode define = null;
        for (MethodNode method : node.methods) {
            if (DEFINE_DATA.equals(method.name) && "()V".equals(method.desc)) {
                if (define != null) throw new IllegalStateException("RagnoEntity has duplicate legacy defineSynchedData methods");
                define = method;
            }
        }
        if (define == null) return 0;

        MethodInsnNode getter = null;
        int getters = 0;
        for (AbstractInsnNode instruction : define.instructions) {
            if (!(instruction instanceof MethodInsnNode call) || !MAX_STUN_HEALTH.equals(call.name)) continue;
            getter = call;
            getters++;
        }
        if (getters == 0 && hasAdaptedConstant(define)) return 0;
        if (getters != 1 || getter.getOpcode() != INVOKEVIRTUAL || !"()I".equals(getter.desc)
                || !RAGNO.equals(getter.owner)) {
            throw new IllegalStateException("RagnoEntity defineSynchedData expected one virtual getMaxStunHealth()I call; found " + getters);
        }

        define.instructions.insertBefore(getter, new InsnNode(POP));
        define.instructions.insertBefore(getter, new org.objectweb.asm.tree.IntInsnNode(BIPUSH, 50));
        define.instructions.remove(getter);
        return 1;
    }

    private static boolean hasAdaptedConstant(MethodNode method) {
        AbstractInsnNode[] instructions = method.instructions.toArray();
        for (int i = 1; i + 1 < instructions.length; i++) {
            if (instructions[i - 1].getOpcode() == ALOAD && instructions[i] instanceof InsnNode pop
                    && pop.getOpcode() == POP && instructions[i + 1] instanceof org.objectweb.asm.tree.IntInsnNode value
                    && value.getOpcode() == BIPUSH && value.operand == 50) return true;
        }
        return false;
    }
}