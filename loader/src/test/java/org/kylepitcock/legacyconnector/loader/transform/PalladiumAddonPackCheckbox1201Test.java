package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Verifies that Palladium's three log filters use the 1.21.1 Checkbox builder. */
class PalladiumAddonPackCheckbox1201Test implements Opcodes {
    private static final String SCREEN = PalladiumAddonPackCheckbox1201.SCREEN;
    private static final String BUTTON = PalladiumAddonPackCheckbox1201.LEGACY_CHECKBOX;
    private static final String COMPONENT = "net/minecraft/network/chat/Component";
    private static final String LIST = PalladiumAddonPackCheckbox1201.DEV_LOG_LIST;
    private static final String OLD_CTOR =
            "(L" + SCREEN + ";IIIIL" + COMPONENT + ";ZLjava/util/function/Consumer;)V";

    private static ClassNode screen() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = SCREEN;
        node.superName = "net/minecraft/client/gui/screens/Screen";
        node.fields.add(new FieldNode(ACC_PUBLIC, "list", "L" + LIST + ";", null, null));

        MethodNode init = new MethodNode(ACC_PUBLIC, "init", "()V", null, null);
        node.methods.add(init);
        for (int i = 0; i < 3; i++) {
            init.instructions.add(new VarInsnNode(ALOAD, 0));
            init.instructions.add(new TypeInsnNode(NEW, BUTTON));
            init.instructions.add(new InsnNode(DUP));
            init.instructions.add(new VarInsnNode(ALOAD, 0));
            for (int value = 0; value < 4; value++) init.instructions.add(new InsnNode(ICONST_1));
            init.instructions.add(new InsnNode(ACONST_NULL)); // Component
            init.instructions.add(new InsnNode(ICONST_1));    // selected
            init.instructions.add(new InsnNode(ACONST_NULL)); // Consumer
            init.instructions.add(new MethodInsnNode(INVOKESPECIAL, BUTTON, "<init>", OLD_CTOR, false));
            init.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                    "net/minecraft/client/gui/screens/Screen", "addRenderableWidget",
                    "(Lnet/minecraft/client/gui/components/events/Renderable;)Lnet/minecraft/client/gui/components/events/Renderable;",
                    false));
            init.instructions.add(new InsnNode(POP));
        }
        init.instructions.add(new InsnNode(RETURN));
        init.maxLocals = 1;
        init.maxStack = 12;
        return node;
    }

    @Test
    void allThreeCheckboxButtonsBecomeNativeBuilderFactoryCalls() {
        ClassNode node = screen();

        assertEquals(4, PalladiumAddonPackCheckbox1201.apply(node));
        MethodNode init = find(node, "init", "()V");
        assertEquals(0, countNew(init, BUTTON));
        assertEquals(0, countCall(init, INVOKESPECIAL, BUTTON, "<init>", OLD_CTOR));
        assertEquals(3, countCall(init, INVOKESTATIC, PalladiumAddonPackCheckbox1201.HELPER,
                "create", "(Lnet/minecraft/client/gui/screens/Screen;IIIIL" + COMPONENT
                        + ";ZLjava/util/function/Consumer;Ljava/lang/Runnable;)L"
                        + PalladiumAddonPackCheckbox1201.CHECKBOX + ";"));
        assertEquals(3, countRefreshLambdas(init));
        assertTrue(hasCall(find(node, PalladiumAddonPackCheckbox1201.REFRESH_BRIDGE, "()V"),
                INVOKEVIRTUAL, LIST, "refreshList", "()V"));

        MethodNode refresh = find(node, PalladiumAddonPackCheckbox1201.REFRESH_BRIDGE, "()V");
        assertNotNull(refresh);
        assertNotNull(findCall(refresh, INVOKEVIRTUAL, LIST, "refreshList", "()V"));
        assertEquals(0, PalladiumAddonPackCheckbox1201.apply(node),
                "the conversion and refresh bridge must be idempotent");

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
    }

    @Test
    void theObsoleteNestedCheckboxSubclassIsOmitted() {
        assertTrue(PalladiumAddonPackCheckbox1201.dropLegacyClass(BUTTON));
        assertFalse(PalladiumAddonPackCheckbox1201.dropLegacyClass(SCREEN));
    }

    @Test
    void unexpectedCheckboxConstructorCountIsRejected() {
        ClassNode node = screen();
        MethodNode init = find(node, "init", "()V");
        MethodInsnNode call = findCall(init, INVOKESPECIAL, BUTTON, "<init>", OLD_CTOR);
        init.instructions.remove(call);
        assertThrows(IllegalStateException.class, () -> PalladiumAddonPackCheckbox1201.apply(node));
        assertEquals(2, countCall(init, INVOKESPECIAL, BUTTON, "<init>", OLD_CTOR));
    }

    private static int countRefreshLambdas(MethodNode method) {
        int count = 0;
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof InvokeDynamicInsnNode dynamic
                    && dynamic.name.equals("run")
                    && dynamic.desc.equals("(L" + SCREEN + ";)Ljava/lang/Runnable;")
                    && dynamic.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) count++;
        }
        return count;
    }

    private static boolean hasCall(MethodNode method, int opcode, String owner, String name, String desc) {
        if (method == null) return false;
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == opcode
                    && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc)) return true;
        }
        return false;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) if (method.name.equals(name) && method.desc.equals(desc)) return method;
        return null;
    }

    private static int countNew(MethodNode method, String type) {
        int count = 0;
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof TypeInsnNode node && node.getOpcode() == NEW && node.desc.equals(type)) count++;
        }
        return count;
    }

    private static int countCall(MethodNode method, int opcode, String owner, String name, String desc) {
        int count = 0;
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == opcode
                    && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc)) count++;
        }
        return count;
    }

    private static MethodInsnNode findCall(MethodNode method, int opcode, String owner, String name, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == opcode
                    && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc)) return call;
        }
        return null;
    }
}
