package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Adapts only PalladiumCore 1.0.1's player-skin Consumer in the existing
 * EntityRendererRegistryImpl.renderLayers callback.
 *
 * Forge 1.20.1 exposes AddLayers.getSkins() as Set<String> and getSkin(String)
 * as LivingEntityRenderer. NeoForge 1.21.1 keeps the erased Set return type,
 * but its elements are PlayerSkin.Model and getSkin(PlayerSkin.Model) erases
 * to EntityRenderer. The old LambdaMetafactory target therefore casts each
 * PlayerSkin.Model to String before the renderer lookup can run.
 *
 * Keep the original callback intact. In particular, it has a per-entity
 * try/catch that deliberately prevents one renderer factory failure from
 * aborting all layer registration. Replacing the whole callback would discard
 * that behavior. Retarget the lambda's captured implementation method and
 * instantiated SAM type, update its getSkin call, and restore the old
 * LivingEntityRenderer result type with an explicit CHECKCAST.
 */
final class PalladiumCorePlayerSkin1201 implements Opcodes {
    static final String REGISTRY_IMPL =
            "net/threetag/palladiumcore/registry/client/forge/EntityRendererRegistryImpl";

    private static final String ADD_LAYERS =
            "net/neoforged/neoforge/client/event/EntityRenderersEvent$AddLayers";
    private static final String PLAYER_SKIN_MODEL =
            "net/minecraft/client/resources/PlayerSkin$Model";
    private static final String ENTITY_RENDERER =
            "net/minecraft/client/renderer/entity/EntityRenderer";
    private static final String LIVING_ENTITY_RENDERER =
            "net/minecraft/client/renderer/entity/LivingEntityRenderer";

    private static final String CALLBACK_DESC = "(L" + ADD_LAYERS + ";)V";
    private static final String GET_SKINS_DESC = "()Ljava/util/Set;";
    private static final String GET_SKIN_1201_DESC =
            "(Ljava/lang/String;)L" + LIVING_ENTITY_RENDERER + ";";
    private static final String GET_SKIN_1211_DESC =
            "(L" + PLAYER_SKIN_MODEL + ";)L" + ENTITY_RENDERER + ";";
    private static final String OLD_SKIN_PARAMETER = "Ljava/lang/String;";
    private static final String NEW_SKIN_PARAMETER = "L" + PLAYER_SKIN_MODEL + ";";
    private static final String FOREACH_DESC = "(Ljava/util/function/Consumer;)V";
    private static final String OLD_INSTANTIATED_SAM_DESC = "(Ljava/lang/String;)V";
    private static final String NEW_INSTANTIATED_SAM_DESC = "(" + NEW_SKIN_PARAMETER + ")V";
    private static final String LAMBDA_METAFACTORY = "java/lang/invoke/LambdaMetafactory";

    private PalladiumCorePlayerSkin1201() {
    }

    static int apply(ClassNode node) {
        if (!REGISTRY_IMPL.equals(node.name)) {
            return 0;
        }

        MethodNode callback = findCallback(node);
        if (callback == null || callback.instructions == null) {
            return 0;
        }

        MethodInsnNode getSkins = null;
        int getSkinsCalls = 0;
        for (AbstractInsnNode instruction : callback.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && ADD_LAYERS.equals(call.owner)
                    && "getSkins".equals(call.name)
                    && GET_SKINS_DESC.equals(call.desc)) {
                getSkins = call;
                getSkinsCalls++;
            }
        }
        if (getSkinsCalls == 0) {
            return 0;
        }
        if (getSkinsCalls != 1) {
            throw new IllegalStateException(
                    "PalladiumCore EntityRendererRegistryImpl expected one player getSkins() call; found "
                            + getSkinsCalls);
        }

        InvokeDynamicInsnNode consumer = findSkinConsumer(getSkins);
        if (consumer == null || consumer.bsmArgs == null || consumer.bsmArgs.length < 3
                || !LAMBDA_METAFACTORY.equals(consumer.bsm.getOwner())
                || !"metafactory".equals(consumer.bsm.getName())
                || !(consumer.bsmArgs[1] instanceof Handle implementation)
                || !(consumer.bsmArgs[2] instanceof Type instantiatedSam)) {
            throw new IllegalStateException(
                    "PalladiumCore player getSkins().forEach Consumer has an unexpected LambdaMetafactory shape");
        }
        if (implementation.getTag() != H_INVOKESTATIC
                || !node.name.equals(implementation.getOwner())) {
            throw new IllegalStateException(
                    "PalladiumCore player-skin lambda is not the expected static method in EntityRendererRegistryImpl");
        }

        MethodNode lambda = findMethod(node, implementation.getName(), implementation.getDesc());
        if (lambda == null || lambda.instructions == null) {
            throw new IllegalStateException(
                    "PalladiumCore player-skin lambda implementation method was not found");
        }

        Type[] lambdaArguments = Type.getArgumentTypes(lambda.desc);
        if (lambdaArguments.length < 2
                || !Type.getObjectType(ADD_LAYERS).equals(lambdaArguments[0])
                || !Type.VOID_TYPE.equals(Type.getReturnType(lambda.desc))) {
            throw new IllegalStateException(
                    "PalladiumCore player-skin lambda has an unexpected method descriptor: " + lambda.desc);
        }
        Type lastArgument = lambdaArguments[lambdaArguments.length - 1];
        boolean alreadyAdapted = NEW_SKIN_PARAMETER.equals(lastArgument.getDescriptor())
                && NEW_INSTANTIATED_SAM_DESC.equals(instantiatedSam.getDescriptor())
                && hasOnlyCall(lambda, "getSkin", GET_SKIN_1211_DESC);
        if (alreadyAdapted) {
            return 0;
        }
        if (!OLD_SKIN_PARAMETER.equals(lastArgument.getDescriptor())
                || !OLD_INSTANTIATED_SAM_DESC.equals(instantiatedSam.getDescriptor())) {
            throw new IllegalStateException(
                    "PalladiumCore player-skin lambda still uses an unexpected skin parameter: " + lambda.desc
                            + " / " + instantiatedSam.getDescriptor());
        }

        MethodInsnNode skinLookup = null;
        int oldSkinLookups = 0;
        for (AbstractInsnNode instruction : lambda.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && ADD_LAYERS.equals(call.owner)
                    && "getSkin".equals(call.name)
                    && GET_SKIN_1201_DESC.equals(call.desc)) {
                skinLookup = call;
                oldSkinLookups++;
            }
        }
        if (oldSkinLookups != 1) {
            throw new IllegalStateException(
                    "PalladiumCore player-skin lambda expected one legacy getSkin(String) call; found "
                            + oldSkinLookups);
        }

        Type[] newArguments = lambdaArguments.clone();
        newArguments[newArguments.length - 1] = Type.getObjectType(PLAYER_SKIN_MODEL);
        String newLambdaDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, newArguments);
        Handle newImplementation = new Handle(
                implementation.getTag(), implementation.getOwner(), implementation.getName(),
                newLambdaDescriptor, implementation.isInterface());

        // All shape checks are complete. Now update the lambda implementation,
        // its invokedynamic bootstrap target, and the type instantiated by the
        // Consumer SAM adapter. The callback itself and its try/catch blocks are
        // deliberately not rewritten.
        lambda.desc = newLambdaDescriptor;
        lambda.signature = null;
        updateSkinParameterLocal(lambda, lambdaArguments);
        skinLookup.desc = GET_SKIN_1211_DESC;

        AbstractInsnNode next = nextRealInstruction(skinLookup);
        if (!(next instanceof TypeInsnNode cast
                && cast.getOpcode() == CHECKCAST
                && LIVING_ENTITY_RENDERER.equals(cast.desc))) {
            lambda.instructions.insert(skinLookup, new TypeInsnNode(CHECKCAST, LIVING_ENTITY_RENDERER));
        }

        consumer.bsmArgs[1] = newImplementation;
        consumer.bsmArgs[2] = Type.getMethodType(NEW_INSTANTIATED_SAM_DESC);
        return 1;
    }

    private static MethodNode findCallback(ClassNode node) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (!"renderLayers".equals(method.name) || !CALLBACK_DESC.equals(method.desc)) {
                continue;
            }
            if (found != null) {
                throw new IllegalStateException(
                        "PalladiumCore EntityRendererRegistryImpl has duplicate renderLayers callbacks");
            }
            found = method;
        }
        return found;
    }

    private static MethodNode findMethod(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Finds the Consumer passed to the forEach immediately following getSkins().
     * The exact source expression is Set<String>.forEach(skin -> ...).
     */
    private static InvokeDynamicInsnNode findSkinConsumer(MethodInsnNode getSkins) {
        InvokeDynamicInsnNode found = null;
        for (AbstractInsnNode instruction = getSkins.getNext();
                instruction != null;
                instruction = instruction.getNext()) {
            if (instruction instanceof InvokeDynamicInsnNode indy && "accept".equals(indy.name)) {
                if (found != null) {
                    throw new IllegalStateException(
                            "PalladiumCore player getSkins() chain has multiple Consumer lambdas");
                }
                found = indy;
            }
            if (instruction instanceof MethodInsnNode call
                    && "forEach".equals(call.name)
                    && FOREACH_DESC.equals(call.desc)) {
                return found;
            }
        }
        return null;
    }

    private static boolean hasOnlyCall(MethodNode method, String name, String descriptor) {
        int matches = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && ADD_LAYERS.equals(call.owner)
                    && name.equals(call.name)) {
                if (!descriptor.equals(call.desc)) {
                    return false;
                }
                matches++;
            }
        }
        return matches == 1;
    }

    private static void updateSkinParameterLocal(MethodNode lambda, Type[] oldArguments) {
        if (lambda.localVariables == null) {
            return;
        }
        int skinLocal = 0;
        for (int i = 0; i < oldArguments.length - 1; i++) {
            skinLocal += oldArguments[i].getSize();
        }
        for (LocalVariableNode local : lambda.localVariables) {
            if (local.index == skinLocal && OLD_SKIN_PARAMETER.equals(local.desc)) {
                local.desc = NEW_SKIN_PARAMETER;
            }
        }
    }

    private static AbstractInsnNode nextRealInstruction(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction.getNext();
        while (next != null && next.getOpcode() < 0) {
            next = next.getNext();
        }
        return next;
    }
}
