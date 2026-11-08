package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Adapts the reviewed Forge menu-opening overload to the NeoForge 1.21.1 player menu API.
 *
 * <p>The Forge call is static and returns void. The target is an instance call on
 * ServerPlayer, returns OptionalInt, and accepts the registry-aware extra-data writer.
 * The returned OptionalInt is deliberately discarded to preserve the source call's
 * stack effect. Only the exact overload is considered, and the immediately preceding
 * LambdaMetafactory Consumer is adapted only when its instantiated parameter is
 * FriendlyByteBuf. Its implementation handle is retained because
 * RegistryFriendlyByteBuf extends FriendlyByteBuf.</p>
 *
 * <p>If the extra-data writer shape is not recognized, the call is left unchanged so
 * the remaining NetworkHooks reference stays visible to the API audit.</p>
 */
public final class Forge1201MenuAdapter {
    private static final String NETWORK_HOOKS = "net/minecraftforge/network/NetworkHooks";
    private static final String SERVER_PLAYER = "net/minecraft/server/level/ServerPlayer";
    private static final String MENU_PROVIDER = "net/minecraft/world/MenuProvider";
    private static final String CONSUMER = "java/util/function/Consumer";
    private static final String FRIENDLY_BYTE_BUF_DESC = "Lnet/minecraft/network/FriendlyByteBuf;";
    private static final String REGISTRY_FRIENDLY_BYTE_BUF_DESC =
            "Lnet/minecraft/network/RegistryFriendlyByteBuf;";
    private static final String LAMBDA_METAFACTORY_OWNER = "java/lang/invoke/LambdaMetafactory";
    private static final String LAMBDA_METAFACTORY_DESC =
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;"
                    + "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
    private static final String OPEN_SCREEN_SOURCE_DESC =
            "(L" + SERVER_PLAYER + ";L" + MENU_PROVIDER + ";L" + CONSUMER + ";)V";
    private static final String OPEN_MENU_TARGET_DESC =
            "(L" + MENU_PROVIDER + ";L" + CONSUMER + ";)Ljava/util/OptionalInt;";
    private static final String CONSUMER_SAM_DESC = "(Ljava/lang/Object;)V";
    private static final String WRITER_SOURCE_DESC = "(" + FRIENDLY_BYTE_BUF_DESC + ")V";
    private static final String WRITER_TARGET_DESC = "(" + REGISTRY_FRIENDLY_BYTE_BUF_DESC + ")V";

    private Forge1201MenuAdapter() {}

    public record Stats(
            int menuOpenCallsAdapted,
            int extraDataWriterLambdasAdapted,
            int menuOpenCallsSkipped) {
        public static Stats empty() {
            return new Stats(0, 0, 0);
        }

        public Stats plus(Stats other) {
            if (other == null) throw new IllegalArgumentException("Other menu adapter stats are required");
            return new Stats(
                    menuOpenCallsAdapted + other.menuOpenCallsAdapted,
                    extraDataWriterLambdasAdapted + other.extraDataWriterLambdasAdapted,
                    menuOpenCallsSkipped + other.menuOpenCallsSkipped);
        }
    }

    public static Stats apply(ClassNode classNode) {
        if (classNode == null) throw new IllegalArgumentException("Class node is required");

        int menuOpenCallsAdapted = 0;
        int extraDataWriterLambdasAdapted = 0;
        int menuOpenCallsSkipped = 0;

        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)
                        || call.getOpcode() != Opcodes.INVOKESTATIC
                        || !call.owner.equals(NETWORK_HOOKS)
                        || !call.name.equals("openScreen")
                        || !call.desc.equals(OPEN_SCREEN_SOURCE_DESC)) {
                    continue;
                }

                AbstractInsnNode previous = previousMeaningfulInstruction(call);
                if (!(previous instanceof InvokeDynamicInsnNode writer)
                        || !adaptExtraDataWriterLambda(writer)) {
                    menuOpenCallsSkipped++;
                    continue;
                }

                MethodInsnNode replacement = new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        SERVER_PLAYER,
                        "openMenu",
                        OPEN_MENU_TARGET_DESC,
                        false);
                method.instructions.set(call, replacement);
                // ServerPlayer#openMenu returns OptionalInt; NetworkHooks#openScreen returned void.
                method.instructions.insert(replacement, new InsnNode(Opcodes.POP));

                menuOpenCallsAdapted++;
                extraDataWriterLambdasAdapted++;
            }
        }

        return new Stats(
                menuOpenCallsAdapted,
                extraDataWriterLambdasAdapted,
                menuOpenCallsSkipped);
    }

    private static AbstractInsnNode previousMeaningfulInstruction(AbstractInsnNode instruction) {
        for (AbstractInsnNode previous = instruction.getPrevious();
                previous != null;
                previous = previous.getPrevious()) {
            if (previous.getOpcode() >= 0) return previous;
        }
        return null;
    }

    private static boolean adaptExtraDataWriterLambda(InvokeDynamicInsnNode lambda) {
        if (!lambda.name.equals("accept")
                || !Type.getReturnType(lambda.desc).getDescriptor().equals("L" + CONSUMER + ";")
                || !lambda.bsm.getOwner().equals(LAMBDA_METAFACTORY_OWNER)
                || !lambda.bsm.getName().equals("metafactory")
                || lambda.bsm.getTag() != Opcodes.H_INVOKESTATIC
                || !lambda.bsm.getDesc().equals(LAMBDA_METAFACTORY_DESC)
                || lambda.bsmArgs.length != 3
                || !(lambda.bsmArgs[0] instanceof Type samMethodType)
                || samMethodType.getSort() != Type.METHOD
                || !samMethodType.getDescriptor().equals(CONSUMER_SAM_DESC)
                || !(lambda.bsmArgs[1] instanceof Handle implementation)
                || implementation.getTag() != Opcodes.H_INVOKESTATIC
                || !hasCompatibleCapturedArguments(lambda.desc, implementation.getDesc())
                || !(lambda.bsmArgs[2] instanceof Type instantiatedMethodType)
                || instantiatedMethodType.getSort() != Type.METHOD
                || !instantiatedMethodType.getDescriptor().equals(WRITER_SOURCE_DESC)) {
            return false;
        }

        // The target supplies RegistryFriendlyByteBuf. The existing writer accepts its
        // FriendlyByteBuf superclass, so only the instantiated functional type changes.
        lambda.bsmArgs[2] = Type.getMethodType(WRITER_TARGET_DESC);
        return true;
    }

    private static boolean hasCompatibleCapturedArguments(
            String invokedynamicDescriptor, String implementationDescriptor) {
        Type invokedynamicType = Type.getMethodType(invokedynamicDescriptor);
        Type implementationType = Type.getMethodType(implementationDescriptor);
        Type[] capturedArguments = invokedynamicType.getArgumentTypes();
        Type[] implementationArguments = implementationType.getArgumentTypes();
        if (!implementationType.getReturnType().equals(Type.VOID_TYPE)
                || implementationArguments.length != capturedArguments.length + 1
                || !implementationArguments[implementationArguments.length - 1]
                        .getDescriptor().equals(FRIENDLY_BYTE_BUF_DESC)) {
            return false;
        }

        for (int i = 0; i < capturedArguments.length; i++) {
            if (!implementationArguments[i].equals(capturedArguments[i])) return false;
        }
        return true;
    }
}
