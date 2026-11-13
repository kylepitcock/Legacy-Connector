package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

class PalladiumSelectionLists1201Test implements Opcodes {
    private static final String DESC =
            "(Lnet/minecraft/client/gui/narration/NarrationElementOutput;)V";
    private static final String SUPER =
            "net/minecraft/client/gui/components/AbstractSelectionList";
    private static final String[] NARRATION_TARGETS = {
            PalladiumSelectionLists1201.ACCESSORY_SLOT_LIST,
            PalladiumSelectionLists1201.ACCESSORY_LIST,
            PalladiumSelectionLists1201.ADDON_PACK_DEV_LOG_LIST,
            PalladiumSelectionLists1201.POWER_ABILITY_LIST
    };

    @Test
    void renamesOnlyTheEmptyNarrationOverrideToTheNewHookForEveryAffectedPalladiumList() {
        for (String name : NARRATION_TARGETS) {
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
    void rewritesLegacyX0ReadsAndWritesToModernWidgetPositionAccessors() {
        String[] targets = {
                PalladiumSelectionLists1201.TAILORING_RECIPE_LIST,
                PalladiumSelectionLists1201.ADDON_LIST_WIDGET,
                PalladiumSelectionLists1201.POWER_ABILITY_LIST
        };
        for (String name : targets) {
            ClassNode node = listNode(name);
            if (PalladiumSelectionLists1201.ADDON_LIST_WIDGET.equals(name)) {
                node.superName = "net/minecraft/client/gui/components/ObjectSelectionList";
            }
            MethodNode method = new MethodNode(ACC_PUBLIC, "layout", "()V", null, null);
            method.instructions.add(new VarInsnNode(ALOAD, 0));
            // Original 1.20.1 SRG bytecode stores AbstractSelectionList.x0 as f_93393_.
            method.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(GETFIELD, name, "f_93393_", "I"));
            method.instructions.add(new InsnNode(POP));
            method.instructions.add(new VarInsnNode(ALOAD, 0));
            method.instructions.add(new InsnNode(ICONST_4));
            method.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(PUTFIELD, name, "f_93393_", "I"));
            method.instructions.add(new InsnNode(RETURN));
            node.methods.add(method);

            assertEquals(2, PalladiumSelectionLists1201.apply(node), name);
            int reads = 0;
            int writes = 0;
            for (var instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode call
                        && call.owner.equals("net/minecraft/client/gui/components/AbstractWidget")) {
                    if (call.name.equals("getX") && call.desc.equals("()I")) reads++;
                    if (call.name.equals("setX") && call.desc.equals("(I)V")) writes++;
                }
                assertFalse(instruction instanceof org.objectweb.asm.tree.FieldInsnNode field
                        && field.owner.equals(name)
                        && (field.name.equals("x0") || field.name.equals("f_93393_")), name);
            }
            assertEquals(1, reads, name);
            assertEquals(1, writes, name);
        }
    }

    @Test
    void preservesLegacyListBackgroundSeparatorsAndSelectionFlags() {
        String[] targets = {
                PalladiumSelectionLists1201.TAILORING_RECIPE_LIST,
                PalladiumSelectionLists1201.ADDON_LIST_WIDGET,
                PalladiumSelectionLists1201.POWER_ABILITY_LIST
        };
        for (String name : targets) {
            ClassNode node = listNode(name);
            if (PalladiumSelectionLists1201.ADDON_LIST_WIDGET.equals(name)) {
                node.superName = "net/minecraft/client/gui/components/ObjectSelectionList";
            }
            MethodNode init = new MethodNode(ACC_PUBLIC, "configure", "()V", null, null);
            String setterOwner = PalladiumSelectionLists1201.ADDON_LIST_WIDGET.equals(name)
                    ? node.superName : name;
            init.instructions.add(new VarInsnNode(ALOAD, 0));
            init.instructions.add(new InsnNode(ICONST_0));
            init.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, setterOwner, "setRenderBackground", "(Z)V", false));
            init.instructions.add(new VarInsnNode(ALOAD, 0));
            init.instructions.add(new InsnNode(ICONST_0));
            init.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, setterOwner, "setRenderTopAndBottom", "(Z)V", false));
            boolean abilityList = PalladiumSelectionLists1201.POWER_ABILITY_LIST.equals(name);
            if (abilityList) {
                init.instructions.add(new VarInsnNode(ALOAD, 0));
                init.instructions.add(new InsnNode(ICONST_0));
                init.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, setterOwner, "setRenderSelection", "(Z)V", false));
            }
            init.instructions.add(new InsnNode(RETURN));
            init.maxStack = 2;
            init.maxLocals = 1;
            node.methods.add(init);

            assertEquals(abilityList ? 6 : 4, PalladiumSelectionLists1201.apply(node), name);
            assertTrue(hasStaticCall(init, "setRenderBackground"));
            assertTrue(hasStaticCall(init, "setRenderTopAndBottom"));
            assertEquals(abilityList, hasStaticCall(init, "setRenderSelection"), name);
            assertNotNull(findMethod(node, "renderListBackground",
                    "(Lnet/minecraft/client/gui/GuiGraphics;)V"));
            assertNotNull(findMethod(node, "renderListSeparators",
                    "(Lnet/minecraft/client/gui/GuiGraphics;)V"));
            assertEquals(abilityList, findMethod(node, "renderSelection",
                    "(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V") != null, name);
            assertEquals(0, PalladiumSelectionLists1201.apply(node), "second application is idempotent: " + name);
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                ClassWriter writer = new ClassWriter(0);
                node.accept(writer);
                writer.toByteArray();
            });
        }
    }

    private static boolean hasStaticCall(MethodNode method, String name) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && call.owner.equals("legacyconnector/v1_20_1/PalladiumSelectionListCompat")
                    && call.name.equals(name)
                    && call.desc.equals("(Ljava/lang/Object;Z)V")) return true;
        }
        return false;
    }

    private static MethodNode findMethod(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return method;
        }
        return null;
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
        assertSame(method, find(node, "updateNarration"));
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
