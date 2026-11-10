package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

class PalladiumSelectionLists1201Test implements Opcodes {
    private static final String DESC =
            "(Lnet/minecraft/client/gui/narration/NarrationElementOutput;)V";
    private static final String SUPER =
            "net/minecraft/client/gui/components/AbstractSelectionList";
    private static final String[] TARGETS = {
            PalladiumSelectionLists1201.ACCESSORY_SLOT_LIST,
            PalladiumSelectionLists1201.ACCESSORY_LIST,
            PalladiumSelectionLists1201.ADDON_PACK_DEV_LOG_LIST,
            PalladiumSelectionLists1201.POWER_ABILITY_LIST
    };

    @Test
    void renamesOnlyTheEmptyNarrationOverrideToTheNewHookForEveryAffectedPalladiumList() {
        for (String name : TARGETS) {
            ClassNode node = listNode(name);
            MethodNode method = new MethodNode(ACC_PUBLIC, "updateNarration", DESC, null, null);
            method.instructions.add(new InsnNode(RETURN));
            method.maxStack = 0;
            method.maxLocals = 2;
            node.methods.add(method);

            assertEquals(1, PalladiumSelectionLists1201.apply(node), name);
            assertNull(find(node, "updateNarration"), name);
            assertNotNull(find(node, "updateWidgetNarration"), name);
            assertEquals(0, PalladiumSelectionLists1201.apply(node), name);
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                ClassWriter writer = new ClassWriter(0);
                node.accept(writer);
                writer.toByteArray();
            });
        }
    }

    @Test
    void unrelatedClassesAreNotChanged() {
        ClassNode node = listNode(PalladiumSelectionLists1201.ACCESSORY_LIST);
        node.name = "example/OtherList";
        MethodNode method = new MethodNode(ACC_PUBLIC, "updateNarration", DESC, null, null);
        method.instructions.add(new InsnNode(RETURN));
        node.methods.add(method);

        assertEquals(0, PalladiumSelectionLists1201.apply(node));
        assertSame(method, find(node, "updateNarration"));
    }

    @Test
    void unexpectedSuperclassIsRejected() {
        ClassNode node = listNode(PalladiumSelectionLists1201.ACCESSORY_SLOT_LIST);
        node.superName = "net/minecraft/client/gui/components/AbstractWidget";
        assertThrows(IllegalStateException.class, () -> PalladiumSelectionLists1201.apply(node));
    }

    @Test
    void nonEmptyImplementationIsRejectedWithoutMutation() {
        ClassNode node = listNode(PalladiumSelectionLists1201.ACCESSORY_LIST);
        MethodNode method = new MethodNode(ACC_PUBLIC, "updateNarration", DESC, null, null);
        method.instructions.add(new InsnNode(NOP));
        method.instructions.add(new InsnNode(RETURN));
        node.methods.add(method);

        assertThrows(IllegalStateException.class, () -> PalladiumSelectionLists1201.apply(node));
        assertSame(method, find(node));
    }

    private static ClassNode listNode(String name) {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = name;
        node.superName = SUPER;
        return node;
    }

    private static MethodNode find(ClassNode node, String name) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && DESC.equals(method.desc)) return method;
        }
        return null;
    }
}
