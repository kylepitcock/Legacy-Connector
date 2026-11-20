package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Verifies the old mixin interface is no longer required by AlienEvo's surf controllers. */
class AlienEvoStepHeight1201Test implements Opcodes {
    @Test
    void ballRollControllerUsesPlayerAttributeHelpers() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.name = "net/habb/alienevo/aliens/arburian_pelarota/BallRollController";
        node.superName = "java/lang/Object";

        MethodNode move = new MethodNode(ACC_PUBLIC, "move", "(Lnet/minecraft/world/entity/player/Player;F)V", null, null);
        node.methods.add(move);
        move.instructions.add(new VarInsnNode(ALOAD, 1));
        move.instructions.add(new TypeInsnNode(CHECKCAST, AlienEvoStepHeight1201.LEGACY_ACCESSOR));
        move.instructions.add(new VarInsnNode(ASTORE, 3));
        move.instructions.add(new VarInsnNode(ALOAD, 3));
        move.instructions.add(new MethodInsnNode(INVOKEINTERFACE, AlienEvoStepHeight1201.LEGACY_ACCESSOR,
                "alienevo$getRawMaxUpStep", "()F", true));
        move.instructions.add(new VarInsnNode(FSTORE, 4));
        move.instructions.add(new VarInsnNode(ALOAD, 3));
        move.instructions.add(new VarInsnNode(FLOAD, 2));
        move.instructions.add(new MethodInsnNode(INVOKEINTERFACE, AlienEvoStepHeight1201.LEGACY_ACCESSOR,
                "alienevo$setRawMaxUpStep", "(F)V", true));
        move.instructions.add(new InsnNode(RETURN));

        assertEquals(3, AlienEvoStepHeight1201.apply(node));
        TypeInsnNode cast = null;
        MethodInsnNode getter = null;
        MethodInsnNode setter = null;
        for (var instruction : move.instructions.toArray()) {
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == CHECKCAST) cast = type;
            if (instruction instanceof MethodInsnNode call && call.owner.equals(AlienEvoStepHeight1201.HELPER)) {
                if (call.name.equals("getRawMaxUpStep")) getter = call;
                if (call.name.equals("setRawMaxUpStep")) setter = call;
            }
        }
        assertNotNull(cast);
        assertEquals(AlienEvoStepHeight1201.PLAYER, cast.desc);
        assertNotNull(getter);
        assertEquals(INVOKESTATIC, getter.getOpcode());
        assertEquals("(L" + AlienEvoStepHeight1201.PLAYER + ";)F", getter.desc);
        assertNotNull(setter);
        assertEquals(INVOKESTATIC, setter.getOpcode());
        assertEquals("(L" + AlienEvoStepHeight1201.PLAYER + ";F)V", setter.desc);
        assertNull(findLegacyCall(move));
    }

    @Test
    void unrelatedClassesAreNotModified() {
        ClassNode node = new ClassNode();
        node.name = "net/habb/alienevo/aliens/other/UnrelatedController";
        node.superName = "java/lang/Object";
        assertEquals(0, AlienEvoStepHeight1201.apply(node));
    }

    private static MethodInsnNode findLegacyCall(MethodNode method) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(AlienEvoStepHeight1201.LEGACY_ACCESSOR)) return call;
        }
        return null;
    }
}
