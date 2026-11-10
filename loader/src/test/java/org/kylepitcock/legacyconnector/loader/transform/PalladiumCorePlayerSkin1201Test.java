package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

class PalladiumCorePlayerSkin1201Test implements Opcodes {
    private static final String OWNER = PalladiumCorePlayerSkin1201.REGISTRY_IMPL;
    private static final String EVENT =
            "net/neoforged/neoforge/client/event/EntityRenderersEvent$AddLayers";
    private static final String PAIR = "com/mojang/datafixers/util/Pair";
    private static final String PLAYER_SKIN_MODEL = "net/minecraft/client/resources/PlayerSkin$Model";
    private static final String ENTITY_RENDERER = "net/minecraft/client/renderer/entity/EntityRenderer";
    private static final String LIVING_RENDERER =
            "net/minecraft/client/renderer/entity/LivingEntityRenderer";
    private static final String OLD_GET_SKIN =
            "(Ljava/lang/String;)L" + LIVING_RENDERER + ";";
    private static final String NEW_GET_SKIN =
            "(L" + PLAYER_SKIN_MODEL + ";)L" + ENTITY_RENDERER + ";";
    private static final String OLD_LAMBDA_DESC =
            "(L" + EVENT + ";L" + PAIR + ";Ljava/lang/String;)V";
    private static final String NEW_LAMBDA_DESC =
            "(L" + EVENT + ";L" + PAIR + ";L" + PLAYER_SKIN_MODEL + ";)V";
    private static final String METAFACTORY_DESC =
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;"
                    + "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";

    @Test
    void migratesOnlyPlayerSkinLambdaAndPreservesOriginalCallbackAndCatch() {
        ClassNode node = legacyRegistry();
        MethodNode callback = findMethod(node, "renderLayers", "(L" + EVENT + ";)V");
        TryCatchBlockNode originalCatch = callback.tryCatchBlocks.get(0);

        assertEquals(1, PalladiumCorePlayerSkin1201.apply(node));

        // The original callback remains present and keeps its exception policy.
        assertEquals(1, callback.tryCatchBlocks.size());
        assertSame(originalCatch, callback.tryCatchBlocks.get(0));
        assertTrue(containsCall(callback, EVENT, "getSkins", "()Ljava/util/Set;"));
        assertTrue(containsCall(callback, "java/util/Set", "forEach",
                "(Ljava/util/function/Consumer;)V"));

        MethodNode lambda = findMethod(node, "lambda$renderLayers$0", NEW_LAMBDA_DESC);
        assertNotNull(lambda);
        MethodInsnNode lookup = findCall(lambda, EVENT, "getSkin", NEW_GET_SKIN);
        assertNotNull(lookup);
        AbstractInsnNode next = nextReal(lookup);
        assertTrue(next instanceof TypeInsnNode cast
                && cast.getOpcode() == CHECKCAST
                && LIVING_RENDERER.equals(cast.desc));

        LocalVariableNode skinLocal = findLocal(lambda, 2);
        assertNotNull(skinLocal);
        assertEquals("L" + PLAYER_SKIN_MODEL + ";", skinLocal.desc);

        InvokeDynamicInsnNode indy = findConsumer(callback);
        assertNotNull(indy);
        Handle implementation = (Handle) indy.bsmArgs[1];
        assertEquals(NEW_LAMBDA_DESC, implementation.getDesc());
        assertEquals("(L" + PLAYER_SKIN_MODEL + ";)V", ((Type) indy.bsmArgs[2]).getDescriptor());

        assertEquals(0, PalladiumCorePlayerSkin1201.apply(node),
                "the migration must be idempotent");
        assertDoesNotThrow(() -> {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            writer.toByteArray();
        });
    }

    @Test
    void unrelatedClassesAreNotChanged() {
        ClassNode node = legacyRegistry();
        node.name = "example/OtherRendererRegistry";
        MethodNode lambda = findMethod(node, "lambda$renderLayers$0", OLD_LAMBDA_DESC);
        MethodInsnNode lookup = findCall(lambda, EVENT, "getSkin", OLD_GET_SKIN);

        assertEquals(0, PalladiumCorePlayerSkin1201.apply(node));
        assertEquals(OLD_LAMBDA_DESC, lambda.desc);
        assertEquals(OLD_GET_SKIN, lookup.desc);
    }

    @Test
    void unexpectedLambdaShapeIsRejectedBeforeMutation() {
        ClassNode node = legacyRegistry();
        MethodNode callback = findMethod(node, "renderLayers", "(L" + EVENT + ";)V");
        InvokeDynamicInsnNode indy = findConsumer(callback);
        indy.bsmArgs[2] = Type.getMethodType("(Ljava/lang/Object;)V");
        MethodNode lambda = findMethod(node, "lambda$renderLayers$0", OLD_LAMBDA_DESC);

        assertThrows(IllegalStateException.class, () -> PalladiumCorePlayerSkin1201.apply(node));
        assertEquals(OLD_LAMBDA_DESC, lambda.desc);
        assertEquals(OLD_GET_SKIN,
                findCall(lambda, EVENT, "getSkin", OLD_GET_SKIN).desc);
    }

    private static ClassNode legacyRegistry() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = OWNER;
        node.superName = "java/lang/Object";

        MethodNode callback = new MethodNode(ACC_PUBLIC | ACC_STATIC,
                "renderLayers", "(L" + EVENT + ";)V", null, null);
        callback.visibleAnnotations = new ArrayList<>();
        callback.visibleAnnotations.add(new org.objectweb.asm.tree.AnnotationNode(
                "Lnet/neoforged/bus/api/SubscribeEvent;"));
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        callback.instructions.add(start);
        callback.instructions.add(new VarInsnNode(ALOAD, 0));
        callback.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, EVENT, "getSkins",
                "()Ljava/util/Set;", false));
        callback.instructions.add(new VarInsnNode(ALOAD, 0));
        callback.instructions.add(new org.objectweb.asm.tree.InsnNode(ACONST_NULL));
        callback.instructions.add(new InvokeDynamicInsnNode(
                "accept",
                "(L" + EVENT + ";L" + PAIR + ";)Ljava/util/function/Consumer;",
                new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                        METAFACTORY_DESC, false),
                Type.getMethodType("(Ljava/lang/Object;)V"),
                new Handle(H_INVOKESTATIC, OWNER, "lambda$renderLayers$0", OLD_LAMBDA_DESC, false),
                Type.getMethodType("(Ljava/lang/String;)V")));
        callback.instructions.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/Set", "forEach",
                "(Ljava/util/function/Consumer;)V", true));
        callback.instructions.add(new InsnNode(POP));
        callback.instructions.add(end);
        callback.instructions.add(new InsnNode(RETURN));
        callback.instructions.add(handler);
        callback.instructions.add(new InsnNode(POP));
        callback.instructions.add(new InsnNode(RETURN));
        callback.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, "java/lang/Exception"));
        callback.maxStack = 4;
        callback.maxLocals = 1;
        node.methods.add(callback);

        MethodNode lambda = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                "lambda$renderLayers$0", OLD_LAMBDA_DESC, null, null);
        LabelNode lambdaStart = new LabelNode();
        LabelNode lambdaEnd = new LabelNode();
        lambda.instructions.add(lambdaStart);
        lambda.instructions.add(new VarInsnNode(ALOAD, 0));
        lambda.instructions.add(new VarInsnNode(ALOAD, 2));
        MethodInsnNode lookup = new MethodInsnNode(INVOKEVIRTUAL, EVENT, "getSkin", OLD_GET_SKIN, false);
        lambda.instructions.add(lookup);
        lambda.instructions.add(new VarInsnNode(ASTORE, 3));
        lambda.instructions.add(new VarInsnNode(ALOAD, 3));
        lambda.instructions.add(new InsnNode(POP));
        lambda.instructions.add(lambdaEnd);
        lambda.instructions.add(new InsnNode(RETURN));
        lambda.localVariables = new ArrayList<>();
        lambda.localVariables.add(new LocalVariableNode("event", "L" + EVENT + ";", null,
                lambdaStart, lambdaEnd, 0));
        lambda.localVariables.add(new LocalVariableNode("pair", "L" + PAIR + ";", null,
                lambdaStart, lambdaEnd, 1));
        lambda.localVariables.add(new LocalVariableNode("skin", "Ljava/lang/String;", null,
                lambdaStart, lambdaEnd, 2));
        lambda.maxStack = 2;
        lambda.maxLocals = 4;
        node.methods.add(lambda);
        return node;
    }

    private static MethodNode findMethod(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return method;
        }
        return null;
    }

    private static MethodInsnNode findCall(MethodNode method, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && owner.equals(call.owner)
                    && name.equals(call.name)
                    && descriptor.equals(call.desc)) return call;
        }
        return null;
    }

    private static boolean containsCall(MethodNode method, String owner, String name, String descriptor) {
        return findCall(method, owner, name, descriptor) != null;
    }

    private static InvokeDynamicInsnNode findConsumer(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof InvokeDynamicInsnNode indy && "accept".equals(indy.name)) return indy;
        }
        return null;
    }

    private static LocalVariableNode findLocal(MethodNode method, int index) {
        if (method.localVariables == null) return null;
        for (LocalVariableNode local : method.localVariables) {
            if (local.index == index) return local;
        }
        return null;
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        return next;
    }
}
