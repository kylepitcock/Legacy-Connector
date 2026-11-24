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

/** Regression coverage for AlienEvo PossessionAbility's 1.21.1 EntityDimensions access. */
class AlienEvoPossessionAbility1201Test implements Opcodes {
    private static ClassNode fixture(String widthName, String heightName) {
        ClassNode node = new ClassNode();
        node.name = AlienEvoPossessionAbility1201.ABILITY;
        MethodNode method = new MethodNode(ACC_PUBLIC, "tick", "()V", null, null);
        node.methods.add(method);

        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(GETFIELD,
                AlienEvoPossessionAbility1201.ENTITY_DIMENSIONS, widthName, "F"));
        method.instructions.add(new InsnNode(POP));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(GETFIELD,
                AlienEvoPossessionAbility1201.ENTITY_DIMENSIONS, heightName, "F"));
        method.instructions.add(new InsnNode(POP));
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 1;
        method.maxLocals = 1;
        return node;
    }

    @Test
    void migratesBothDimensionsRecordComponentsAndIsIdempotent() {
        ClassNode node = fixture("width", "height");

        assertEquals(2, AlienEvoPossessionAbility1201.apply(node));
        MethodInsnNode width = null;
        MethodInsnNode height = null;
        for (var instruction : node.methods.get(0).instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if ("width".equals(call.name)) width = call;
                if ("height".equals(call.name)) height = call;
            }
        }

        assertInstanceOf(MethodInsnNode.class, width);
        assertEquals(INVOKEVIRTUAL, width.getOpcode());
        assertEquals(AlienEvoPossessionAbility1201.ENTITY_DIMENSIONS, width.owner);
        assertEquals("()F", width.desc);

        assertInstanceOf(MethodInsnNode.class, height);
        assertEquals(INVOKEVIRTUAL, height.getOpcode());
        assertEquals(AlienEvoPossessionAbility1201.ENTITY_DIMENSIONS, height.owner);
        assertEquals("()F", height.desc);

        assertEquals(0, AlienEvoPossessionAbility1201.apply(node));
    }

    @Test
    void migratesOriginalSrgFieldNames() {
        ClassNode node = fixture("f_20377_", "f_20378_");
        assertEquals(2, AlienEvoPossessionAbility1201.apply(node));
    }

    @Test
    void failsClosedWhenOneExpectedDimensionReadIsMissing() {
        ClassNode node = fixture("width", "other");
        assertThrows(IllegalStateException.class, () -> AlienEvoPossessionAbility1201.apply(node));
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        ClassNode node = fixture("width", "height");
        node.name = "example/OtherAbility";
        assertEquals(0, AlienEvoPossessionAbility1201.apply(node));
        assertInstanceOf(FieldInsnNode.class, node.methods.get(0).instructions.get(1));
    }

    @Test
    void isIntegratedIntoTheAlienEvoTransformerPass() {
        ClassNode node = fixture("f_20377_", "f_20378_");
        assertEquals(2, AlienEvoMixins1201.apply(node));
        assertEquals(0, AlienEvoMixins1201.apply(node));
    }
}
