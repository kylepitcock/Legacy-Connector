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

/** Regression tests for Palladium CustomProjectile's private EntityDimensions.width access. */
class PalladiumCustomProjectile1201Test implements Opcodes {
    private static ClassNode fixture(String fieldName) {
        ClassNode node = new ClassNode();
        node.name = PalladiumCustomProjectile1201.CUSTOM_PROJECTILE;
        MethodNode method = new MethodNode(ACC_PRIVATE, "writeSpawnDimensions", "()F", null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(
                GETFIELD, PalladiumCustomProjectile1201.ENTITY_DIMENSIONS, fieldName, "F"));
        method.instructions.add(new InsnNode(FRETURN));
        node.methods.add(method);
        return node;
    }

    private static MethodInsnNode assertWidthAccessor(ClassNode node) {
        return assertInstanceOf(MethodInsnNode.class, node.methods.get(0).instructions.get(1));
    }

    @Test
    void migratesNamedWidthFieldReadAndIsIdempotent() {
        ClassNode node = fixture("width");
        assertEquals(1, PalladiumCustomProjectile1201.apply(node));
        MethodInsnNode call = assertWidthAccessor(node);
        assertEquals(INVOKEVIRTUAL, call.getOpcode());
        assertEquals(PalladiumCustomProjectile1201.ENTITY_DIMENSIONS, call.owner);
        assertEquals("width", call.name);
        assertEquals("()F", call.desc);
        assertEquals(0, PalladiumCustomProjectile1201.apply(node));
    }

    @Test
    void migratesOriginalSrgWidthFieldRead() {
        ClassNode node = fixture("f_20377_");
        assertEquals(1, PalladiumCustomProjectile1201.apply(node));
        MethodInsnNode call = assertWidthAccessor(node);
        assertEquals("width", call.name);
        assertEquals("()F", call.desc);
    }

    @Test
    void rejectsAnUnexpectedCustomProjectileDimensionsAccessShape() {
        ClassNode node = fixture("eyeHeight");
        assertThrows(IllegalStateException.class, () -> PalladiumCustomProjectile1201.apply(node));
        assertInstanceOf(FieldInsnNode.class, node.methods.get(0).instructions.get(1));
    }

    @Test
    void ignoresUnrelatedClasses() {
        ClassNode node = fixture("eyeHeight");
        node.name = "net/threetag/palladium/entity/UnrelatedProjectile";
        assertEquals(0, PalladiumCustomProjectile1201.apply(node));
        assertInstanceOf(FieldInsnNode.class, node.methods.get(0).instructions.get(1));
    }
}
