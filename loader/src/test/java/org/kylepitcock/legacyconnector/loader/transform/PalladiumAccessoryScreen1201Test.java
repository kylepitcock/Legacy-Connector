package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/** Regression tests for Palladium 4.5.10's new OptionsSubScreen abstract hook. */
class PalladiumAccessoryScreen1201Test implements Opcodes {
    @Test
    void accessoryScreenGetsConcreteEmptyAddOptionsHook() {
        ClassNode node = accessoryScreenNode();

        assertEquals(1, PalladiumAccessoryScreen1201.apply(node));

        MethodNode addOptions = find(node, "addOptions", "()V");
        assertNotNull(addOptions);
        assertEquals(ACC_PROTECTED, addOptions.access & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED));
        assertFalse((addOptions.access & ACC_ABSTRACT) != 0);
        assertEquals(1, addOptions.instructions.size());
        assertEquals(RETURN, addOptions.instructions.getFirst().getOpcode());
        assertEquals(0, addOptions.maxStack);
        assertEquals(1, addOptions.maxLocals);

        assertEquals(0, PalladiumAccessoryScreen1201.apply(node),
                "the adapter must be idempotent and must not add duplicate hooks");
        assertEquals(1, count(node, "addOptions", "()V"));
    }

    @Test
    void existingConcreteAddOptionsImplementationIsPreserved() {
        ClassNode node = accessoryScreenNode();
        MethodNode existing = new MethodNode(ACC_PROTECTED, "addOptions", "()V", null, null);
        existing.instructions.add(new org.objectweb.asm.tree.InsnNode(RETURN));
        existing.maxStack = 0;
        existing.maxLocals = 1;
        node.methods.add(existing);

        assertEquals(0, PalladiumAccessoryScreen1201.apply(node));
        assertEquals(existing, find(node, "addOptions", "()V"));
    }

    @Test
    void unrelatedClassesAreNotChanged() {
        ClassNode node = accessoryScreenNode();
        node.name = "example/OtherScreen";

        assertEquals(0, PalladiumAccessoryScreen1201.apply(node));
        assertEquals(0, count(node, "addOptions", "()V"));
    }

    @Test
    void unexpectedAccessoryScreenSuperclassIsRejected() {
        ClassNode node = accessoryScreenNode();
        node.superName = "net/minecraft/client/gui/screens/Screen";

        assertThrows(IllegalStateException.class, () -> PalladiumAccessoryScreen1201.apply(node));
    }

    private static ClassNode accessoryScreenNode() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumAccessoryScreen1201.ACCESSORY_SCREEN;
        node.superName = "net/minecraft/client/gui/screens/options/OptionsSubScreen";
        return node;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    private static int count(ClassNode node, String name, String descriptor) {
        int count = 0;
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                count++;
            }
        }
        return count;
    }
}
