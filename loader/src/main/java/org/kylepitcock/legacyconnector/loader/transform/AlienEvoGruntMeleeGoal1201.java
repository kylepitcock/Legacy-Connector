package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Restores AlienEvo GruntEntity's 1.20.1 melee reach calculation after
 * MeleeAttackGoal.getAttackReachSqr was removed in 1.21.1.
 */
final class AlienEvoGruntMeleeGoal1201 implements Opcodes {
    static final String TARGET = "net/habb/alienevo/entity/mob/GruntEntity$GruntMeleeGoal";
    static final String GRUNT = "net/habb/alienevo/entity/mob/GruntEntity";
    static final String SUPER = "net/minecraft/world/entity/ai/goal/MeleeAttackGoal";
    static final String MOB = "net/minecraft/world/entity/Mob";
    static final String LIVING = "net/minecraft/world/entity/LivingEntity";
    static final String HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoGruntMeleeGoal1201Runtime";
    static final String REACH_DESC = "(L" + LIVING + ";)D";
    static final String BRIDGE_DESC = "(L" + MOB + ";L" + LIVING + ";)D";

    private AlienEvoGruntMeleeGoal1201() {}

    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        MethodNode reach = null;
        for (MethodNode method : node.methods) {
            if (!"getAttackReachSqr".equals(method.name) || !REACH_DESC.equals(method.desc)) continue;
            if (reach != null) {
                throw new IllegalStateException("AlienEvo GruntMeleeGoal has duplicate getAttackReachSqr methods");
            }
            reach = method;
        }
        if (reach == null || reach.instructions == null) {
            throw new IllegalStateException("AlienEvo GruntMeleeGoal no longer declares getAttackReachSqr(LivingEntity)D");
        }

        MethodInsnNode legacyCall = null;
        int legacyCalls = 0;
        int bridgeCalls = 0;
        for (AbstractInsnNode instruction : reach.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.getOpcode() == INVOKESPECIAL && SUPER.equals(call.owner)
                    && "getAttackReachSqr".equals(call.name) && REACH_DESC.equals(call.desc)) {
                legacyCall = call;
                legacyCalls++;
            } else if (call.getOpcode() == INVOKESTATIC && HELPER.equals(call.owner)
                    && "baseAttackReachSqr".equals(call.name) && BRIDGE_DESC.equals(call.desc)) {
                bridgeCalls++;
            }
        }

        if (legacyCalls == 0 && bridgeCalls == 1) return 0;
        if (legacyCalls != 1 || bridgeCalls != 0) {
            throw new IllegalStateException("AlienEvo GruntMeleeGoal expected one legacy superclass reach call; found "
                    + legacyCalls + " legacy and " + bridgeCalls + " migrated");
        }

        AbstractInsnNode targetLoad = previousCode(legacyCall);
        AbstractInsnNode goalLoad = previousCode(targetLoad);
        if (!(targetLoad instanceof VarInsnNode targetVar) || targetVar.getOpcode() != ALOAD || targetVar.var != 1
                || !(goalLoad instanceof VarInsnNode goalVar) || goalVar.getOpcode() != ALOAD || goalVar.var != 0) {
            throw new IllegalStateException("AlienEvo GruntMeleeGoal superclass reach call no longer has the expected this/target stack shape");
        }

        AbstractInsnNode afterThisLoad = nextCode(legacyCall);
        AbstractInsnNode outerField = nextCode(afterThisLoad);
        AbstractInsnNode extraReach = nextCode(outerField);
        AbstractInsnNode add = nextCode(extraReach);
        AbstractInsnNode returnValue = nextCode(add);
        if (!(afterThisLoad instanceof VarInsnNode afterThis) || afterThis.getOpcode() != ALOAD || afterThis.var != 0
                || !(outerField instanceof FieldInsnNode field)
                || field.getOpcode() != GETFIELD || !TARGET.equals(field.owner)
                || !"this$0".equals(field.name) || !("L" + GRUNT + ";").equals(field.desc)
                || !(extraReach instanceof MethodInsnNode extra)
                || extra.getOpcode() != INVOKEVIRTUAL || !GRUNT.equals(extra.owner)
                || !"extraReachSqr".equals(extra.name) || !"()D".equals(extra.desc)
                || add == null || add.getOpcode() != DADD
                || returnValue == null || returnValue.getOpcode() != DRETURN) {
            throw new IllegalStateException("AlienEvo GruntMeleeGoal custom extraReachSqr tail changed; refusing partial rewrite");
        }

        InsnList replacement = new InsnList();
        replacement.add(new VarInsnNode(ALOAD, 0));
        replacement.add(new FieldInsnNode(GETFIELD, TARGET, "this$0", "L" + GRUNT + ";"));
        replacement.add(new VarInsnNode(ALOAD, 1));
        replacement.add(new MethodInsnNode(INVOKESTATIC, HELPER, "baseAttackReachSqr", BRIDGE_DESC, false));
        reach.instructions.insertBefore(goalLoad, replacement);
        reach.instructions.remove(goalLoad);
        reach.instructions.remove(targetLoad);
        reach.instructions.remove(legacyCall);
        return 1;
    }

    private static AbstractInsnNode previousCode(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction == null ? null : instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }

    private static AbstractInsnNode nextCode(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction == null ? null : instruction.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        return next;
    }
}
