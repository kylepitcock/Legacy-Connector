package org.kylepitcock.legacyconnector.loader.transform;

import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.transform.compat.IllageAndSpillageRagno1201;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static org.junit.jupiter.api.Assertions.*;

class IllageAndSpillageRagno1201Test implements Opcodes {
    @Test
    void replacesOnlyConstructorTimeInitialStunHealthRead() {
        ClassNode node = ragno();

        assertEquals(1, IllageAndSpillageRagno1201.apply(node));
        MethodNode define = method(node, "defineSynchedData", "()V");
        assertEquals(0, countCalls(define, "getMaxStunHealth"));
        assertTrue(hasInt(define, BIPUSH, 50));
        assertEquals(1, countCalls(method(node, "runtimeHealthCheck", "()I"), "getMaxStunHealth"));
        assertEquals(0, IllageAndSpillageRagno1201.apply(node));
        assertDoesNotThrow(() -> {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            writer.toByteArray();
        });
    }

    @Test
    void unrelatedClassIsUntouched() {
        ClassNode node = ragno();
        node.name = "example/OtherEntity";
        assertEquals(0, IllageAndSpillageRagno1201.apply(node));
        assertEquals(1, countCalls(method(node, "defineSynchedData", "()V"), "getMaxStunHealth"));
    }

    @Test
    void unexpectedCallCountFailsInsteadOfSilentlySkipping() {
        ClassNode node = ragno();
        MethodNode define = method(node, "defineSynchedData", "()V");
        define.instructions.remove(findCall(define, "getMaxStunHealth"));
        assertThrows(IllegalStateException.class, () -> IllageAndSpillageRagno1201.apply(node));
    }

    private static ClassNode ragno() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = IllageAndSpillageRagno1201.RAGNO;
        node.superName = "net/minecraft/world/entity/raid/Raider";

        MethodNode define = new MethodNode(ACC_PROTECTED, "defineSynchedData", "()V", null, null);
        define.instructions.add(new VarInsnNode(ALOAD, 0));
        define.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.superName, "defineSynchedData", "()V", false));
        define.instructions.add(new VarInsnNode(ALOAD, 0));
        define.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, node.name, "getMaxStunHealth", "()I", false));
        define.instructions.add(new InsnNode(POP));
        define.instructions.add(new InsnNode(RETURN));
        define.maxStack = 1;
        define.maxLocals = 1;
        node.methods.add(define);

        MethodNode runtime = new MethodNode(ACC_PUBLIC, "runtimeHealthCheck", "()I", null, null);
        runtime.instructions.add(new VarInsnNode(ALOAD, 0));
        runtime.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, node.name, "getMaxStunHealth", "()I", false));
        runtime.instructions.add(new InsnNode(IRETURN));
        runtime.maxStack = 1;
        runtime.maxLocals = 1;
        node.methods.add(runtime);
        return node;
    }

    private static MethodNode method(ClassNode node, String name, String desc) {
        return node.methods.stream().filter(method -> method.name.equals(name) && method.desc.equals(desc)).findFirst().orElseThrow();
    }

    private static int countCalls(MethodNode method, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals(name)) count++;
        }
        return count;
    }

    private static MethodInsnNode findCall(MethodNode method, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals(name)) return call;
        }
        return null;
    }

    private static boolean hasInt(MethodNode method, int opcode, int value) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof IntInsnNode integer && integer.getOpcode() == opcode && integer.operand == value) return true;
        }
        return false;
    }
}