package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

final class Forge1201MenuAdapterTest {
    private static final String NETWORK_HOOKS = "net/minecraftforge/network/NetworkHooks";
    private static final String SERVER_PLAYER = "net/minecraft/server/level/ServerPlayer";
    private static final String MENU_PROVIDER = "net/minecraft/world/MenuProvider";
    private static final String CONSUMER = "java/util/function/Consumer";
    private static final String FRIENDLY_BYTE_BUF = "net/minecraft/network/FriendlyByteBuf";
    private static final String REGISTRY_FRIENDLY_BYTE_BUF =
            "net/minecraft/network/RegistryFriendlyByteBuf";
    private static final String OPEN_SCREEN_DESC =
            "(L" + SERVER_PLAYER + ";L" + MENU_PROVIDER + ";L" + CONSUMER + ";)V";
    private static final String OPEN_MENU_DESC =
            "(L" + MENU_PROVIDER + ";L" + CONSUMER + ";)Ljava/util/OptionalInt;";
    private static final String LAMBDA_METAFACTORY_DESC =
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;"
                    + "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";

    @Test
    void adaptsExactOpenScreenCallAndItsRegistryAwareExtraDataWriter() {
        ClassNode classNode = new ClassNode();
        classNode.name = "example/MenuFixture";
        MethodNode method = new MethodNode(
                Opcodes.ACC_STATIC,
                "open",
                "(L" + SERVER_PLAYER + ";L" + MENU_PROVIDER + ";ILexample/Profile;)V",
                null,
                null);

        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));

        InvokeDynamicInsnNode writer = writerLambda();
        method.instructions.add(writer);
        MethodInsnNode oldCall = new MethodInsnNode(
                Opcodes.INVOKESTATIC, NETWORK_HOOKS, "openScreen", OPEN_SCREEN_DESC, false);
        method.instructions.add(oldCall);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        classNode.methods.add(method);

        Forge1201MenuAdapter.Stats stats = Forge1201MenuAdapter.apply(classNode);

        assertEquals(1, stats.menuOpenCallsAdapted());
        assertEquals(1, stats.extraDataWriterLambdasAdapted());
        assertEquals(0, stats.menuOpenCallsSkipped());

        MethodInsnNode adaptedCall = findMethodCall(method);
        assertNotNull(adaptedCall);
        assertEquals(Opcodes.INVOKEVIRTUAL, adaptedCall.getOpcode());
        assertEquals(SERVER_PLAYER, adaptedCall.owner);
        assertEquals("openMenu", adaptedCall.name);
        assertEquals(OPEN_MENU_DESC, adaptedCall.desc);
        assertFalse(adaptedCall.itf);
        assertEquals(Opcodes.POP, adaptedCall.getNext().getOpcode());
        assertEquals(Opcodes.RETURN, adaptedCall.getNext().getNext().getOpcode());

        assertEquals(
                "(" + "L" + REGISTRY_FRIENDLY_BYTE_BUF + ";)V",
                ((Type) writer.bsmArgs[2]).getDescriptor());
        Handle implementation = (Handle) writer.bsmArgs[1];
        assertEquals(
                "(ILexample/Profile;L" + FRIENDLY_BYTE_BUF + ";)V",
                implementation.getDesc());
    }

    @Test
    void leavesOpenScreenCallUnchangedWhenWriterLambdaIsNotImmediatelyAvailable() {
        ClassNode classNode = new ClassNode();
        classNode.name = "example/UnmatchedMenuFixture";
        MethodNode method = new MethodNode(
                Opcodes.ACC_STATIC,
                "open",
                "(L" + SERVER_PLAYER + ";L" + MENU_PROVIDER + ";Ljava/lang/Object;)V",
                null,
                null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        MethodInsnNode oldCall = new MethodInsnNode(
                Opcodes.INVOKESTATIC, NETWORK_HOOKS, "openScreen", OPEN_SCREEN_DESC, false);
        method.instructions.add(oldCall);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        classNode.methods.add(method);

        Forge1201MenuAdapter.Stats stats = Forge1201MenuAdapter.apply(classNode);

        assertEquals(0, stats.menuOpenCallsAdapted());
        assertEquals(0, stats.extraDataWriterLambdasAdapted());
        assertEquals(1, stats.menuOpenCallsSkipped());
        assertEquals(Opcodes.INVOKESTATIC, oldCall.getOpcode());
        assertEquals(NETWORK_HOOKS, oldCall.owner);
        assertEquals(OPEN_SCREEN_DESC, oldCall.desc);
        assertEquals(Opcodes.RETURN, oldCall.getNext().getOpcode());
    }

    private static InvokeDynamicInsnNode writerLambda() {
        Handle lambdaMetafactory = new Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                LAMBDA_METAFACTORY_DESC,
                false);
        Handle implementation = new Handle(
                Opcodes.H_INVOKESTATIC,
                "example/MenuFixture",
                "writeExtraData",
                "(ILexample/Profile;L" + FRIENDLY_BYTE_BUF + ";)V",
                false);
        return new InvokeDynamicInsnNode(
                "accept",
                "(ILexample/Profile;)L" + CONSUMER + ";",
                lambdaMetafactory,
                Type.getMethodType("(Ljava/lang/Object;)V"),
                implementation,
                Type.getMethodType("(L" + FRIENDLY_BYTE_BUF + ";)V"));
    }

    private static MethodInsnNode findMethodCall(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call) return call;
        }
        return null;
    }
}
