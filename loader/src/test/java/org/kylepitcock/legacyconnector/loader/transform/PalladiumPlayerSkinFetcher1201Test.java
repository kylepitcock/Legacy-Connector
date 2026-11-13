package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

class PalladiumPlayerSkinFetcher1201Test implements Opcodes {
    private static final String OWNER = PalladiumPlayerSkinFetcher1201.TARGET;
    private static final String GAME_PROFILE = "com/mojang/authlib/GameProfile";
    private static final String SKIN_MANAGER = "net/minecraft/client/resources/SkinManager";
    private static final String SKIN_CALLBACK =
            "net/minecraft/client/resources/SkinManager$SkinTextureCallback";
    private static final String PLAYER_SKIN = "net/minecraft/client/resources/PlayerSkin";
    private static final String PLAYER_SKIN_MODEL = PLAYER_SKIN + "$Model";
    private static final String SKIN_INFO =
            "net/threetag/palladium/client/renderer/entity/PlayerSkinInfo";
    private static final String RESOURCE_LOCATION = "net/minecraft/resources/ResourceLocation";
    private static final String PROFILE_TEXTURE =
            "com/mojang/authlib/minecraft/MinecraftProfileTexture";
    private static final String PROFILE_TEXTURE_TYPE =
            "com/mojang/authlib/minecraft/MinecraftProfileTexture$Type";
    private static final String OLD_CALLBACK_DESC =
            "(L" + GAME_PROFILE + ";L" + SKIN_CALLBACK + ";Z)V";
    private static final String OLD_CALLBACK_INDY_DESC =
            "(L" + GAME_PROFILE + ";)L" + SKIN_CALLBACK + ";";
    private static final String OLD_CALLBACK_IMPL_DESC =
            "(L" + GAME_PROFILE + ";L" + PROFILE_TEXTURE_TYPE + ";L" + RESOURCE_LOCATION
                    + ";L" + PROFILE_TEXTURE + ";)V";
    private static final String NEW_CALLBACK_IMPL_DESC =
            "(L" + GAME_PROFILE + ";L" + PLAYER_SKIN + ";Ljava/lang/Throwable;)V";
    private static final String LMF_DESC =
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;"
                    + "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";

    @Test
    void migratesLegacyCallbackToFutureAndModernPlayerSkinValues() {
        ClassNode node = fixture();
        MethodNode request = find(node, "lambda$getPlayerSkinInfo$4", "(Ljava/lang/String;Ljava/lang/String;)V");

        assertEquals(1, PalladiumPlayerSkinFetcher1201.apply(node));

        assertNotNull(findCall(request, SKIN_MANAGER, "getOrLoad",
                "(L" + GAME_PROFILE + ";)Ljava/util/concurrent/CompletableFuture;"));
        assertNotNull(findCall(request, "java/util/concurrent/CompletableFuture", "whenComplete",
                "(Ljava/util/function/BiConsumer;)Ljava/util/concurrent/CompletableFuture;"));
        assertNull(findCall(request, SKIN_MANAGER, "registerSkins", OLD_CALLBACK_DESC));

        InvokeDynamicInsnNode consumer = findIndy(request, "accept");
        assertNotNull(consumer);
        assertEquals("(L" + GAME_PROFILE + ";)Ljava/util/function/BiConsumer;", consumer.desc);
        Handle target = (Handle) consumer.bsmArgs[1];
        assertEquals(NEW_CALLBACK_IMPL_DESC, target.getDesc());
        assertEquals("(L" + PLAYER_SKIN + ";Ljava/lang/Throwable;)V",
                ((Type) consumer.bsmArgs[2]).getDescriptor());

        MethodNode callback = find(node, "lambda$getPlayerSkinInfo$2", NEW_CALLBACK_IMPL_DESC);
        assertNotNull(callback);
        assertNotNull(findCall(callback, PLAYER_SKIN, "model", "()L" + PLAYER_SKIN_MODEL + ";"));
        assertNotNull(findCall(callback, PLAYER_SKIN_MODEL, "id", "()Ljava/lang/String;"));
        assertNotNull(findCall(callback, PLAYER_SKIN, "texture", "()L" + RESOURCE_LOCATION + ";"));
        assertNotNull(findCall(callback, SKIN_INFO, "setFailed", "()V"));
        assertEquals(1, countJumpOpcode(callback, IFNONNULL),
                "a failed future is routed to the cached failure path");
        assertEquals(1, countJumpOpcode(callback, IFNULL),
                "an unexpectedly null skin is routed to the cached failure path");
        assertNotNull(findCall(callback, SKIN_INFO, "set",
                "(Ljava/lang/String;L" + RESOURCE_LOCATION + ";)V"));

        assertEquals(0, PalladiumPlayerSkinFetcher1201.apply(node), "the adapter must be idempotent");
        assertDoesNotContainMissingCallback(node);
        assertTrue(ClassWriterCanEmit(node));
    }

    @Test
    void doesNotChangeUnrelatedClasses() {
        ClassNode node = fixture();
        node.name = "example/OtherClass";
        MethodNode request = find(node, "lambda$getPlayerSkinInfo$4", "(Ljava/lang/String;Ljava/lang/String;)V");
        assertEquals(0, PalladiumPlayerSkinFetcher1201.apply(node));
        assertNotNull(findCall(request, SKIN_MANAGER, "m_118817_", OLD_CALLBACK_DESC));
    }

    private static ClassNode fixture() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = OWNER;
        node.superName = "java/lang/Object";

        MethodNode request = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                "lambda$getPlayerSkinInfo$4", "(Ljava/lang/String;Ljava/lang/String;)V", null, null);
        request.instructions.add(new InsnNode(ACONST_NULL));
        request.instructions.add(new VarInsnNode(ASTORE, 2));
        request.instructions.add(new MethodInsnNode(INVOKESTATIC,
                "net/minecraft/client/Minecraft", "getInstance",
                "()Lnet/minecraft/client/Minecraft;", false));
        request.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "net/minecraft/client/Minecraft", "getSkinManager",
                "()L" + SKIN_MANAGER + ";", false));
        request.instructions.add(new VarInsnNode(ALOAD, 2));
        request.instructions.add(new VarInsnNode(ALOAD, 2));
        request.instructions.add(new InvokeDynamicInsnNode(
                "m_118856_", OLD_CALLBACK_INDY_DESC,
                metafactory(),
                Type.getMethodType("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"),
                new Handle(H_INVOKESTATIC, OWNER, "lambda$getPlayerSkinInfo$2", OLD_CALLBACK_IMPL_DESC, false),
                Type.getMethodType("(L" + PROFILE_TEXTURE_TYPE + ";L" + RESOURCE_LOCATION
                        + ";L" + PROFILE_TEXTURE + ";)V")));
        request.instructions.add(new InsnNode(ICONST_1));
        request.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, SKIN_MANAGER,
                "m_118817_", OLD_CALLBACK_DESC, false));
        LabelNode done = new LabelNode();
        request.instructions.add(new JumpInsnNode(GOTO, done));
        request.instructions.add(done);
        request.instructions.add(new InsnNode(RETURN));
        request.maxStack = 5;
        request.maxLocals = 3;
        node.methods.add(request);

        MethodNode callback = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                "lambda$getPlayerSkinInfo$2", OLD_CALLBACK_IMPL_DESC, null, null);
        callback.instructions.add(new VarInsnNode(ALOAD, 0));
        callback.instructions.add(new InsnNode(POP));
        callback.instructions.add(new InsnNode(RETURN));
        callback.maxStack = 1;
        callback.maxLocals = 4;
        callback.instructions.insert(callback.instructions.getLast().getPrevious(),
                cacheMapperIndy());
        node.methods.add(callback);

        MethodNode mapper = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                "lambda$getPlayerSkinInfo$1", "(Ljava/lang/String;)L" + SKIN_INFO + ";", null, null);
        mapper.instructions.add(new InsnNode(ACONST_NULL));
        mapper.instructions.add(new InsnNode(ARETURN));
        mapper.maxStack = 1;
        mapper.maxLocals = 1;
        node.methods.add(mapper);
        return node;
    }

    private static InvokeDynamicInsnNode cacheMapperIndy() {
        return new InvokeDynamicInsnNode(
                "apply", "()Ljava/util/function/Function;", metafactory(),
                Type.getMethodType("(Ljava/lang/Object;)Ljava/lang/Object;"),
                new Handle(H_INVOKESTATIC, OWNER, "lambda$getPlayerSkinInfo$1",
                        "(Ljava/lang/String;)L" + SKIN_INFO + ";", false),
                Type.getMethodType("(Ljava/lang/String;)L" + SKIN_INFO + ";"));
    }

    private static Handle metafactory() {
        return new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", LMF_DESC, false);
    }

    private static int countJumpOpcode(MethodNode method, int opcode) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof JumpInsnNode jump && jump.getOpcode() == opcode) count++;
        }
        return count;
    }

    private static InvokeDynamicInsnNode findIndy(MethodNode method, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof InvokeDynamicInsnNode indy && name.equals(indy.name)) return indy;
        }
        return null;
    }

    private static MethodInsnNode findCall(MethodNode method, String owner, String name, String desc) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && owner.equals(call.owner) && name.equals(call.name) && desc.equals(call.desc)) return call;
        }
        return null;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && desc.equals(method.desc)) return method;
        }
        return null;
    }

    private static void assertDoesNotContainMissingCallback(ClassNode node) {
        for (MethodNode method : node.methods) {
            assertTrue(!method.desc.contains(SKIN_CALLBACK), method.name + method.desc);
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call) {
                    assertTrue(!call.owner.equals(SKIN_CALLBACK), call.owner + "." + call.name + call.desc);
                    assertTrue(!call.desc.contains(SKIN_CALLBACK), call.owner + "." + call.name + call.desc);
                }
                if (instruction instanceof InvokeDynamicInsnNode indy) {
                    assertTrue(!indy.desc.contains(SKIN_CALLBACK), indy.desc);
                    for (Object argument : indy.bsmArgs) {
                        assertTrue(!(argument instanceof Handle handle
                                && (handle.getOwner().equals(SKIN_CALLBACK)
                                || handle.getDesc().contains(SKIN_CALLBACK))), indy.toString());
                    }
                }
            }
        }
    }

    private static boolean ClassWriterCanEmit(ClassNode node) {
        try {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            writer.toByteArray();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
