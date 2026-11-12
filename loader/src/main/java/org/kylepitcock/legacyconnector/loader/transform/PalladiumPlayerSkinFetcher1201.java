package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Migrates Palladium's asynchronous name-to-player-skin lookup from the
 * removed SkinManager.SkinTextureCallback API to 1.21.1's getOrLoad future.
 *
 * The existing cache and PlayerSkinInfo result type remain unchanged. A
 * successful result stores the modern PlayerSkin model id and texture
 * ResourceLocation; an empty result marks the cached lookup failed.
 */
final class PalladiumPlayerSkinFetcher1201 implements Opcodes {
    static final String TARGET =
            "net/threetag/palladium/client/renderer/entity/PlayerSkinFetcher";

    private static final String GAME_PROFILE = "com/mojang/authlib/GameProfile";
    private static final String SKIN_MANAGER = "net/minecraft/client/resources/SkinManager";
    private static final String SKIN_CALLBACK =
            "net/minecraft/client/resources/SkinManager$SkinTextureCallback";
    private static final String PLAYER_SKIN = "net/minecraft/client/resources/PlayerSkin";
    private static final String PLAYER_SKIN_MODEL = PLAYER_SKIN + "$Model";
    private static final String SKIN_INFO =
            "net/threetag/palladium/client/renderer/entity/PlayerSkinInfo";
    private static final String MAP = "java/util/Map";
    private static final String THROWABLE = "java/lang/Throwable";
    private static final String COMPLETABLE_FUTURE = "java/util/concurrent/CompletableFuture";
    private static final String FUNCTION = "java/util/function/Function";

    private static final String OLD_CALLBACK_DESC =
            "(L" + GAME_PROFILE + ";L" + SKIN_CALLBACK + ";Z)V";
    private static final String OLD_CALLBACK_INDY_DESC =
            "(L" + GAME_PROFILE + ";)L" + SKIN_CALLBACK + ";";
    private static final String OLD_CALLBACK_IMPL_DESC =
            "(L" + GAME_PROFILE
                    + ";Lcom/mojang/authlib/minecraft/MinecraftProfileTexture$Type;"
                    + "Lnet/minecraft/resources/ResourceLocation;"
                    + "Lcom/mojang/authlib/minecraft/MinecraftProfileTexture;)V";
    private static final String NEW_CALLBACK_IMPL_DESC =
            "(L" + GAME_PROFILE + ";L" + PLAYER_SKIN + ";L" + THROWABLE + ";)V";
    private static final String GET_OR_LOAD_DESC =
            "(L" + GAME_PROFILE + ";)Ljava/util/concurrent/CompletableFuture;";
    private static final String WHEN_COMPLETE_DESC =
            "(Ljava/util/function/BiConsumer;)Ljava/util/concurrent/CompletableFuture;";
    private static final String NEW_CALLBACK_INDY_DESC =
            "(L" + GAME_PROFILE + ";)Ljava/util/function/BiConsumer;";

    private PalladiumPlayerSkinFetcher1201() {
    }

    /** Returns one when the legacy SkinManager callback is replaced. */
    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        MethodNode request = find(node, "lambda$getPlayerSkinInfo$4",
                "(Ljava/lang/String;Ljava/lang/String;)V");
        MethodNode callback = find(node, "lambda$getPlayerSkinInfo$2", OLD_CALLBACK_IMPL_DESC);
        if (request == null || callback == null) {
            MethodNode alreadyRequest = find(node, "lambda$getPlayerSkinInfo$4",
                    "(Ljava/lang/String;Ljava/lang/String;)V");
            MethodNode alreadyCallback = find(node, "lambda$getPlayerSkinInfo$2", NEW_CALLBACK_IMPL_DESC);
            if (alreadyRequest != null && alreadyCallback != null
                    && hasMethodCall(alreadyRequest, SKIN_MANAGER, "getOrLoad", GET_OR_LOAD_DESC)
                    && hasMethodCall(alreadyRequest, COMPLETABLE_FUTURE, "whenComplete", WHEN_COMPLETE_DESC)) {
                return 0;
            }
            throw new IllegalStateException(
                    "Palladium PlayerSkinFetcher no longer has the inspected 1.20.1 callback methods");
        }

        MethodInsnNode registerSkins = null;
        for (AbstractInsnNode instruction : request.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKEVIRTUAL
                    && SKIN_MANAGER.equals(call.owner)
                    && OLD_CALLBACK_DESC.equals(call.desc)) {
                if (registerSkins != null) {
                    throw new IllegalStateException("Palladium PlayerSkinFetcher has multiple legacy registerSkins calls");
                }
                registerSkins = call;
            }
        }
        if (registerSkins == null) {
            throw new IllegalStateException(
                    "Palladium PlayerSkinFetcher no longer calls the expected SkinManager callback method");
        }

        InvokeDynamicInsnNode oldCallback = findIndyBefore(registerSkins, OLD_CALLBACK_INDY_DESC);
        if (oldCallback == null || oldCallback.bsmArgs == null || oldCallback.bsmArgs.length != 3
                || !(oldCallback.bsmArgs[1] instanceof Handle implementation)
                || !implementation.getOwner().equals(node.name)
                || !implementation.getName().equals(callback.name)
                || !implementation.getDesc().equals(OLD_CALLBACK_IMPL_DESC)) {
            throw new IllegalStateException(
                    "Palladium PlayerSkinFetcher skin callback lambda has an unexpected bootstrap shape");
        }

        InvokeDynamicInsnNode mapFunction = findIndy(callback, "apply", "()Ljava/util/function/Function;");
        if (mapFunction == null) {
            throw new IllegalStateException(
                    "Palladium PlayerSkinFetcher cache callback no longer uses the inspected computeIfAbsent mapper");
        }

        // The old expression is:
        // skinManager.registerSkins(profile, (type, location, texture) -> ..., true)
        // The new expression is:
        // skinManager.getOrLoad(profile).whenComplete((skin, error) -> ...).
        AbstractInsnNode captureProfile = previousReal(oldCallback);
        if (!(captureProfile instanceof VarInsnNode capturedProfile)
                || capturedProfile.getOpcode() != ALOAD || capturedProfile.var != 2) {
            throw new IllegalStateException(
                    "Palladium PlayerSkinFetcher no longer captures the expected GameProfile local");
        }
        AbstractInsnNode lookupProfile = previousReal(captureProfile);
        if (!(lookupProfile instanceof VarInsnNode requestProfile)
                || requestProfile.getOpcode() != ALOAD || requestProfile.var != 2) {
            throw new IllegalStateException(
                    "Palladium PlayerSkinFetcher no longer passes its GameProfile to SkinManager");
        }

        oldCallback.name = "accept";
        oldCallback.desc = NEW_CALLBACK_INDY_DESC;
        oldCallback.bsmArgs = new Object[] {
                Type.getMethodType("(Ljava/lang/Object;Ljava/lang/Object;)V"),
                new Handle(H_INVOKESTATIC, node.name, callback.name, NEW_CALLBACK_IMPL_DESC, false),
                Type.getMethodType("(L" + PLAYER_SKIN + ";L" + THROWABLE + ";)V")
        };

        // getOrLoad consumes the manager and the first profile operand. The
        // second profile operand remains the captured value for whenComplete.
        request.instructions.insert(lookupProfile, new MethodInsnNode(
                INVOKEVIRTUAL, SKIN_MANAGER, "getOrLoad", GET_OR_LOAD_DESC, false));

        AbstractInsnNode flag = nextReal(oldCallback);
        if (flag == null || flag.getOpcode() != ICONST_1) {
            throw new IllegalStateException(
                    "Palladium PlayerSkinFetcher legacy registerSkins call no longer has its expected secure flag");
        }
        request.instructions.remove(flag);

        registerSkins.setOpcode(INVOKEVIRTUAL);
        registerSkins.owner = COMPLETABLE_FUTURE;
        registerSkins.name = "whenComplete";
        registerSkins.desc = WHEN_COMPLETE_DESC;
        registerSkins.itf = false;
        request.instructions.insert(registerSkins, new InsnNode(POP));

        // Rebuild only the small callback body. Preserve the same map,
        // cache-miss factory, PlayerSkinInfo.set(), and failure behavior.
        callback.instructions.clear();
        callback.tryCatchBlocks.clear();
        callback.localVariables = null;
        callback.signature = null;

        LabelNode hasSkin = new LabelNode();
        LabelNode done = new LabelNode();
        callback.instructions.add(new VarInsnNode(ALOAD, 2));
        callback.instructions.add(new JumpInsnNode(IFNONNULL, hasSkin));
        callback.instructions.add(new VarInsnNode(ALOAD, 1));
        callback.instructions.add(new JumpInsnNode(IFNONNULL, hasSkin));

        // An exceptional or empty future result -> the same cached failure
        // state used when a name cannot be resolved to a GameProfile.
        callback.instructions.add(new FieldInsnNode(GETSTATIC, node.name, "CACHED_NAME_SKINS", "Ljava/util/Map;"));
        callback.instructions.add(new VarInsnNode(ALOAD, 0));
        callback.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, GAME_PROFILE, "getName", "()Ljava/lang/String;", false));
        callback.instructions.add(new FieldInsnNode(GETSTATIC, "java/util/Locale", "ROOT", "Ljava/util/Locale;"));
        callback.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toLowerCase",
                "(Ljava/util/Locale;)Ljava/lang/String;", false));
        callback.instructions.add(cloneIndy(mapFunction));
        callback.instructions.add(new MethodInsnNode(INVOKEINTERFACE, MAP, "computeIfAbsent",
                "(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;", true));
        callback.instructions.add(new TypeInsnNode(CHECKCAST, SKIN_INFO));
        callback.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, SKIN_INFO, "setFailed", "()V", false));
        callback.instructions.add(new JumpInsnNode(GOTO, done));

        // Successful PlayerSkin -> retain Palladium's "slim"/"default"
        // model label and texture location.
        callback.instructions.add(new JumpInsnNode(GOTO, done));
        callback.instructions.add(hasSkin);
        callback.instructions.add(new FieldInsnNode(GETSTATIC, node.name, "CACHED_NAME_SKINS", "Ljava/util/Map;"));
        callback.instructions.add(new VarInsnNode(ALOAD, 0));
        callback.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, GAME_PROFILE, "getName", "()Ljava/lang/String;", false));
        callback.instructions.add(new FieldInsnNode(GETSTATIC, "java/util/Locale", "ROOT", "Ljava/util/Locale;"));
        callback.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toLowerCase",
                "(Ljava/util/Locale;)Ljava/lang/String;", false));
        callback.instructions.add(cloneIndy(mapFunction));
        callback.instructions.add(new MethodInsnNode(INVOKEINTERFACE, MAP, "computeIfAbsent",
                "(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;", true));
        callback.instructions.add(new TypeInsnNode(CHECKCAST, SKIN_INFO));
        callback.instructions.add(new VarInsnNode(ALOAD, 1));
        callback.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, PLAYER_SKIN, "model", "()L" + PLAYER_SKIN_MODEL + ";", false));
        callback.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, PLAYER_SKIN_MODEL, "id", "()Ljava/lang/String;", false));
        callback.instructions.add(new VarInsnNode(ALOAD, 1));
        callback.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, PLAYER_SKIN, "texture", "()Lnet/minecraft/resources/ResourceLocation;", false));
        callback.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, SKIN_INFO, "set",
                "(Ljava/lang/String;Lnet/minecraft/resources/ResourceLocation;)V", false));
        callback.instructions.add(new JumpInsnNode(GOTO, done));

        callback.instructions.add(done);
        callback.instructions.add(new InsnNode(RETURN));
        callback.maxStack = 5;
        callback.maxLocals = 3;

        return 1;
    }

    private static InvokeDynamicInsnNode cloneIndy(InvokeDynamicInsnNode source) {
        return new InvokeDynamicInsnNode(source.name, source.desc, source.bsm, source.bsmArgs.clone());
    }

    private static boolean hasMethodCall(MethodNode method, String owner, String name, String desc) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && owner.equals(call.owner)
                    && name.equals(call.name) && desc.equals(call.desc)) return true;
        }
        return false;
    }

    private static InvokeDynamicInsnNode findIndyBefore(AbstractInsnNode instruction, String desc) {
        for (AbstractInsnNode previous = instruction.getPrevious(); previous != null; previous = previous.getPrevious()) {
            if (previous instanceof InvokeDynamicInsnNode indy && desc.equals(indy.desc)) return indy;
        }
        return null;
    }

    private static InvokeDynamicInsnNode findIndy(MethodNode method, String name, String desc) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof InvokeDynamicInsnNode indy
                    && name.equals(indy.name) && desc.equals(indy.desc)) return indy;
        }
        return null;
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        return next;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && desc.equals(method.desc)) return method;
        }
        return null;
    }
}
