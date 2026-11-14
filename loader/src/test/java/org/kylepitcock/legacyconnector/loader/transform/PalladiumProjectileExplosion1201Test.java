package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for Palladium 4.5.10's 1.20.1 explosion-packet constructor. */
class PalladiumProjectileExplosion1201Test implements Opcodes {
    private static ClassNode fixture(boolean includeExplosionLocal) {
        ClassNode node = new ClassNode();
        node.name = PalladiumProjectileExplosion1201.CUSTOM_PROJECTILE;
        MethodNode method = new MethodNode(
                ACC_PUBLIC, "explode", PalladiumProjectileExplosion1201.EXPLODE_METHOD_DESC, null, null);
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        method.instructions.add(start);
        method.instructions.add(new TypeInsnNode(NEW, PalladiumProjectileExplosion1201.PACKET));
        method.instructions.add(new InsnNode(DUP));
        method.instructions.add(new InsnNode(DCONST_0));
        method.instructions.add(new InsnNode(DCONST_0));
        method.instructions.add(new InsnNode(DCONST_0));
        method.instructions.add(new InsnNode(FCONST_1));
        method.instructions.add(new InsnNode(ACONST_NULL));
        method.instructions.add(new InsnNode(ACONST_NULL));
        method.instructions.add(new MethodInsnNode(
                INVOKESPECIAL,
                PalladiumProjectileExplosion1201.PACKET,
                "<init>",
                PalladiumProjectileExplosion1201.OLD_CONSTRUCTOR_DESC,
                false));
        method.instructions.add(new InsnNode(POP));
        method.instructions.add(new InsnNode(RETURN));
        method.instructions.add(end);
        method.localVariables = new ArrayList<>();
        if (includeExplosionLocal) {
            method.localVariables.add(new LocalVariableNode(
                    "explosion", "L" + PalladiumProjectileExplosion1201.EXPLOSION + ";", null, start, end, 12));
        }
        method.maxLocals = 13;
        method.maxStack = 8;
        node.methods.add(method);
        return node;
    }

    @Test
    void rewritesLegacyConstructorToExplosionAwareFactoryAndIsIdempotent() {
        ClassNode node = fixture(true);
        assertEquals(1, PalladiumProjectileExplosion1201.apply(node));

        MethodInsnNode migrated = null;
        for (var instruction : node.methods.get(0).instructions) {
            if (instruction instanceof MethodInsnNode call
                    && PalladiumProjectileExplosion1201.HELPER.equals(call.owner)) {
                migrated = call;
                break;
            }
        }
        migrated = assertInstanceOf(MethodInsnNode.class, migrated);
        assertEquals(INVOKESTATIC, migrated.getOpcode());
        assertEquals("createExplodePacket", migrated.name);
        assertEquals(PalladiumProjectileExplosion1201.HELPER_DESC, migrated.desc);

        var previous = migrated.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        VarInsnNode explosion = assertInstanceOf(VarInsnNode.class, previous);
        assertEquals(ALOAD, explosion.getOpcode());
        assertEquals(12, explosion.var);

        for (var instruction : node.methods.get(0).instructions) {
            if (instruction instanceof MethodInsnNode call) {
                assertEquals(false, call.getOpcode() == INVOKESPECIAL
                        && PalladiumProjectileExplosion1201.PACKET.equals(call.owner)
                        && "<init>".equals(call.name));
            }
            if (instruction instanceof TypeInsnNode allocation) {
                assertEquals(false, NEW == allocation.getOpcode()
                        && PalladiumProjectileExplosion1201.PACKET.equals(allocation.desc));
            }
        }
        assertEquals(0, PalladiumProjectileExplosion1201.apply(node));
    }

    @Test
    void rejectsPacketConstructorWhenExplosionLocalIsNotAvailable() {
        ClassNode node = fixture(false);
        assertThrows(IllegalStateException.class, () -> PalladiumProjectileExplosion1201.apply(node));
    }

    @Test
    void ignoresUnrelatedClasses() {
        ClassNode node = fixture(true);
        node.name = "net/threetag/palladium/entity/UnrelatedProjectile";
        assertEquals(0, PalladiumProjectileExplosion1201.apply(node));
    }
}
