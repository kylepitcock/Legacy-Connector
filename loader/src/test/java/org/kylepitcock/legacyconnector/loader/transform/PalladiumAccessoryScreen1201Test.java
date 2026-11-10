package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Regression tests for Palladium 4.5.10's 1.21.1 OptionsSubScreen lifecycle. */
class PalladiumAccessoryScreen1201Test implements Opcodes {
    private static final String SCREEN = "net/minecraft/client/gui/screens/Screen";
    private static final String OPTIONS = "net/minecraft/client/Options";
    private static final String MINECRAFT = "net/minecraft/client/Minecraft";
    private static final String OPTIONS_SUB_SCREEN =
            "net/minecraft/client/gui/screens/options/OptionsSubScreen";
    private static final String COMPONENT = "net/minecraft/network/chat/Component";
    private static final String MUTABLE_COMPONENT = "net/minecraft/network/chat/MutableComponent";

    @Test
    void accessoryScreenGetsConcreteHookAndCurrentMinecraftOptions() {
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

        MethodNode constructor = find(node, "<init>", "(L" + SCREEN + ";)V");
        assertNotNull(constructor);
        assertTrue(hasCall(constructor, INVOKESTATIC, MINECRAFT, "getInstance",
                "()L" + MINECRAFT + ";"));
        assertTrue(hasCall(constructor, GETFIELD, MINECRAFT, "options", "L" + OPTIONS + ";"));
        assertFalse(hasOpcode(constructor, ACONST_NULL),
                "the OptionsSubScreen constructor must not receive null Options");
        assertTrue(hasCall(constructor, INVOKESPECIAL, OPTIONS_SUB_SCREEN, "<init>",
                "(L" + SCREEN + ";L" + OPTIONS + ";L" + COMPONENT + ";)V"));

        assertEquals(0, PalladiumAccessoryScreen1201.apply(node),
                "the adapter must be idempotent");
        assertEquals(1, count(node, "addOptions", "()V"));
    }

    @Test
    void existingConcreteAddOptionsImplementationIsPreserved() {
        ClassNode node = accessoryScreenNode();
        MethodNode existing = new MethodNode(ACC_PROTECTED, "addOptions", "()V", null, null);
        existing.instructions.add(new InsnNode(RETURN));
        existing.maxStack = 0;
        existing.maxLocals = 1;
        node.methods.add(existing);

        // The constructor still needs its null-Options migration.
        assertEquals(1, PalladiumAccessoryScreen1201.apply(node));
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

    @Test
    void unexpectedOptionsConstructorArgumentIsRejected() {
        ClassNode node = accessoryScreenNode();
        MethodNode constructor = find(node, "<init>", "(L" + SCREEN + ";)V");
        AbstractInsnNode nullOptions = findOpcode(constructor, ACONST_NULL);
        constructor.instructions.set(nullOptions, new InsnNode(ICONST_0));

        assertThrows(IllegalStateException.class, () -> PalladiumAccessoryScreen1201.apply(node));
    }

    private static ClassNode accessoryScreenNode() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumAccessoryScreen1201.ACCESSORY_SCREEN;
        node.superName = OPTIONS_SUB_SCREEN;

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "(L" + SCREEN + ";)V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new VarInsnNode(ALOAD, 1));
        constructor.instructions.add(new InsnNode(ACONST_NULL));
        constructor.instructions.add(new LdcInsnNode("gui.palladium.accessories"));
        constructor.instructions.add(new MethodInsnNode(INVOKESTATIC, COMPONENT, "translatable",
                "(Ljava/lang/String;)L" + MUTABLE_COMPONENT + ";", true));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, OPTIONS_SUB_SCREEN, "<init>",
                "(L" + SCREEN + ";L" + OPTIONS + ";L" + COMPONENT + ";)V", false));
        constructor.instructions.add(new InsnNode(RETURN));
        constructor.maxStack = 3;
        constructor.maxLocals = 2;
        node.methods.add(constructor);
        return node;
    }

    private static AbstractInsnNode findOpcode(MethodNode method, int opcode) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() == opcode) return instruction;
        }
        return null;
    }

    private static boolean hasOpcode(MethodNode method, int opcode) {
        return findOpcode(method, opcode) != null;
    }

    private static boolean hasCall(MethodNode method, int opcode, String owner, String name, String desc) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == opcode
                    && call.owner.equals(owner)
                    && call.name.equals(name)
                    && call.desc.equals(desc)) return true;
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == opcode
                    && field.owner.equals(owner)
                    && field.name.equals(name)
                    && field.desc.equals(desc)) return true;
        }
        return false;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return method;
        }
        return null;
    }

    private static int count(ClassNode node, String name, String descriptor) {
        int count = 0;
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) count++;
        }
        return count;
    }
}
