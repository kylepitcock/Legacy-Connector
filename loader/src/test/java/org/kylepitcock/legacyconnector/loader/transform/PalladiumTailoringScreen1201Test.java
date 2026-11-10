package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

class PalladiumTailoringScreen1201Test implements Opcodes {
    private static final String DESC =
            "(Lnet/minecraft/client/gui/narration/NarrationElementOutput;)V";

    @Test
    void removesOnlyTheEmptyLegacyNarrationOverride() {
        ClassNode node = legacyRecipeList();
        MethodNode method = new MethodNode(ACC_PUBLIC, "updateNarration", DESC, null, null);
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 0;
        method.maxLocals = 2;
        node.methods.add(method);

        assertEquals(1, PalladiumTailoringScreen1201.apply(node));
        assertNull(find(node));
        assertEquals(0, PalladiumTailoringScreen1201.apply(node));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            writer.toByteArray();
        });
    }

    @Test
    void unrelatedClassesAreNotChanged() {
        ClassNode node = legacyRecipeList();
        node.name = "example/OtherRecipeList";
        MethodNode method = new MethodNode(ACC_PUBLIC, "updateNarration", DESC, null, null);
        method.instructions.add(new InsnNode(RETURN));
        node.methods.add(method);

        assertEquals(0, PalladiumTailoringScreen1201.apply(node));
        org.junit.jupiter.api.Assertions.assertSame(method, find(node));
    }

    @Test
    void unexpectedSuperclassIsRejected() {
        ClassNode node = legacyRecipeList();
        node.superName = "net/minecraft/client/gui/components/AbstractWidget";
        assertThrows(IllegalStateException.class, () -> PalladiumTailoringScreen1201.apply(node));
    }

    @Test
    void nonEmptyImplementationIsRejectedWithoutMutation() {
        ClassNode node = legacyRecipeList();
        MethodNode method = new MethodNode(ACC_PUBLIC, "updateNarration", DESC, null, null);
        method.instructions.add(new InsnNode(NOP));
        method.instructions.add(new InsnNode(RETURN));
        node.methods.add(method);

        assertThrows(IllegalStateException.class, () -> PalladiumTailoringScreen1201.apply(node));
        org.junit.jupiter.api.Assertions.assertSame(method, find(node));
    }

    private static ClassNode legacyRecipeList() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumTailoringScreen1201.RECIPE_LIST;
        node.superName = "net/minecraft/client/gui/components/AbstractSelectionList";
        return node;
    }

    private static MethodNode find(ClassNode node) {
        for (MethodNode method : node.methods) {
            if ("updateNarration".equals(method.name) && DESC.equals(method.desc)) return method;
        }
        return null;
    }
}
