package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression coverage for AlienEvo SwimmingController's 1.21.1 dimensions access. */
class AlienEvoSwimmingController1201Test implements Opcodes {
    private static ClassNode fixture(String firstHeight, String secondHeight) {
        ClassNode node = new ClassNode();
        node.name = AlienEvoSwimmingController1201.TARGET;

        MethodNode update = new MethodNode(ACC_PUBLIC, "updateHitboxScaling", "()V", null, null);
        node.methods.add(update);
        update.instructions.add(new VarInsnNode(ALOAD, 0));
        update.instructions.add(new FieldInsnNode(GETFIELD,
                AlienEvoSwimmingController1201.ENTITY_DIMENSIONS, firstHeight, "F"));
        update.instructions.add(new InsnNode(POP));
        update.instructions.add(new VarInsnNode(ALOAD, 0));
        update.instructions.add(new FieldInsnNode(GETFIELD,
                AlienEvoSwimmingController1201.ENTITY_DIMENSIONS, secondHeight, "F"));
        update.instructions.add(new InsnNode(POP));
        update.instructions.add(new InsnNode(RETURN));
        update.maxStack = 1;
        update.maxLocals = 1;

        MethodNode unrelated = new MethodNode(ACC_PUBLIC, "tick", "()V", null, null);
        node.methods.add(unrelated);
        unrelated.instructions.add(new VarInsnNode(ALOAD, 0));
        unrelated.instructions.add(new FieldInsnNode(GETFIELD,
                AlienEvoSwimmingController1201.ENTITY_DIMENSIONS, "height", "F"));
        unrelated.instructions.add(new InsnNode(POP));
        unrelated.instructions.add(new InsnNode(RETURN));
        unrelated.maxStack = 1;
        unrelated.maxLocals = 1;
        return node;
    }

    @Test
    void rewritesEveryHeightReadOnlyInsideUpdateHitboxScalingAndIsIdempotent() {
        ClassNode node = fixture("height", "height");
        assertEquals(2, AlienEvoSwimmingController1201.apply(node));

        int accessors = 0;
        for (var instruction : node.methods.get(0).instructions) {
            if (instruction instanceof MethodInsnNode call && "height".equals(call.name)) accessors++;
        }
        assertEquals(2, accessors);
        assertInstanceOf(FieldInsnNode.class, node.methods.get(1).instructions.get(1));
        assertEquals(0, AlienEvoSwimmingController1201.apply(node));
    }

    @Test
    void acceptsTheOriginalSrgHeightFieldName() {
        ClassNode node = fixture("f_20378_", "f_20378_");
        assertEquals(2, AlienEvoSwimmingController1201.apply(node));
    }

    @Test
    void failsClosedWhenTheExpectedHeightReadIsMissing() {
        ClassNode node = fixture("width", "width");
        assertThrows(IllegalStateException.class, () -> AlienEvoSwimmingController1201.apply(node));
    }

    @Test
    void failsClosedWhenTheExpectedMethodIsMissing() {
        ClassNode node = fixture("height", "height");
        node.methods.get(0).name = "tickSwimming";
        assertThrows(IllegalStateException.class, () -> AlienEvoSwimmingController1201.apply(node));
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        ClassNode node = fixture("height", "height");
        node.name = "example/OtherController";
        assertEquals(0, AlienEvoSwimmingController1201.apply(node));
        assertInstanceOf(FieldInsnNode.class, node.methods.get(0).instructions.get(1));
    }

    @Test
    void isIntegratedIntoTheAlienEvoTransformerPass() {
        ClassNode node = fixture("height", "height");
        assertEquals(2, AlienEvoMixins1201.apply(node));
        assertEquals(0, AlienEvoMixins1201.apply(node));
    }
}
