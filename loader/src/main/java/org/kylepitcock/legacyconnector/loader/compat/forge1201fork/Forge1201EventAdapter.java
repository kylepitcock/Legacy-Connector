package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Applies reusable class-level and call-site adaptations for Forge 1.20.1 Event cancellation.
 *
 * <p>The pass must run on source-named ClassNodes before ClassRemapper. It intentionally
 * injects target ICancellableEvent only when the source class is directly annotated
 * {@code @Cancelable} (or overrides isCancelable), not on every Forge Event subclass.</p>
 */
public final class Forge1201EventAdapter {
    public static final String SOURCE_EVENT = "net/minecraftforge/eventbus/api/Event";
    public static final String SOURCE_CANCELABLE_DESCRIPTOR =
            "Lnet/minecraftforge/eventbus/api/Cancelable;";
    public static final String TARGET_EVENT = "net/neoforged/bus/api/Event";
    public static final String TARGET_CANCELLABLE = "net/neoforged/bus/api/ICancellableEvent";
    public static final String COMPAT_INTERFACE =
            "legacyconnector/v1_20_1/net/minecraftforge/eventbus/api/Forge1201EventCompat";
    public static final String CANCELABLE_MARKER =
            "legacyconnector/v1_20_1/net/minecraftforge/eventbus/api/Forge1201CancelableEventMarker";
    public static final String BRIDGE =
            "legacyconnector/v1_20_1/net/minecraftforge/eventbus/api/Forge1201EventBridge";

    private static final String SOURCE_EVENT_DESCRIPTOR = "L" + SOURCE_EVENT + ";";
    private static final String IS_CANCELABLE_DESCRIPTOR = "()Z";
    private static final String IS_CANCELED_DESCRIPTOR = "()Z";
    private static final String SET_CANCELED_DESCRIPTOR = "(Z)V";

    private static final String METHOD_IS_CANCELABLE = "isCancelable";
    private static final String METHOD_IS_CANCELED = "isCanceled";
    private static final String METHOD_SET_CANCELED = "setCanceled";

    private static final String BASE_IS_CANCELABLE = "baseIsCancelable";
    private static final String BASE_IS_CANCELED = "baseIsCanceled";
    private static final String BASE_SET_CANCELED = "baseSetCanceled";

    private static final List<String> SOURCE_EVENT_BASE_PREFIXES = List.of(
            "net/minecraftforge/event/",
            "net/minecraftforge/eventbus/api/",
            "legacyconnector/v1_20_1/net/minecraftforge/event/");

    /**
     * Pinned Forge 47.3.0 classes whose cancellation methods are inherited from Event.
     * These may be safely rewritten to the non-virtual source-base helpers.
     *
     * <p>Do not expand this list based on package name alone. Each entry must be verified
     * against the source class bytecode for all three method signatures.</p>
     */
    private static final Set<String> SOURCE_EVENT_TYPES_INHERITING_CANCELLATION_METHODS = Set.of(
            "net/minecraftforge/event/entity/player/PlayerEvent",
            "net/minecraftforge/event/entity/player/PlayerInteractEvent",
            "net/minecraftforge/event/entity/player/PlayerInteractEvent$EntityInteract",
            "net/minecraftforge/event/entity/player/PlayerInteractEvent$EntityInteractSpecific",
            "net/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock",
            "net/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickItem");

    /**
     * Source owner/method pairs whose exact target equivalent has been audited and must
     * retain invokespecial dispatch. RightClickBlock overrides setCanceled to update its
     * useBlock/useItem result state; target 21.1.258 retains that override.
     */
    private static boolean hasAuditedTargetSuperImplementation(String owner, String name, String descriptor) {
        return "net/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock".equals(owner)
                && METHOD_SET_CANCELED.equals(name)
                && SET_CANCELED_DESCRIPTOR.equals(descriptor);
    }

    private Forge1201EventAdapter() {
    }

    public record Stats(
            int eventRoots,
            int cancelableClasses,
            int callSitesRewritten,
            int superCallsRewritten) {
        public static Stats empty() {
            return new Stats(0, 0, 0, 0);
        }

        public Stats plus(Stats other) {
            return new Stats(
                    eventRoots + other.eventRoots,
                    cancelableClasses + other.cancelableClasses,
                    callSitesRewritten + other.callSitesRewritten,
                    superCallsRewritten + other.superCallsRewritten);
        }
    }

    /**
     * Transforms one class node. Repeated application is idempotent.
     *
     * @throws IOException if the source class contains an unsupported method handle or
     *                     an impossible event-method invocation shape
     */
    public static Stats apply(ClassNode classNode) throws IOException {
        Objects.requireNonNull(classNode, "classNode");
        boolean eventRoot = isForgeEventBase(classNode.superName);
        boolean directlyCancelable = removeCancelableAnnotation(classNode);
        boolean declaresCancelableOverride = declaresInstanceMethod(
                classNode, METHOD_IS_CANCELABLE, IS_CANCELABLE_DESCRIPTOR);

        if (directlyCancelable && (classNode.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION)) != 0) {
            throw new IOException("Forge @Cancelable was found on non-class type " + classNode.name);
        }

        if (eventRoot || directlyCancelable) {
            addInterface(classNode, COMPAT_INTERFACE);
        }

        if (directlyCancelable) {
            addInterface(classNode, CANCELABLE_MARKER);
            addInterface(classNode, TARGET_CANCELLABLE);
        } else if (declaresCancelableOverride) {
            // A custom isCancelable override may return true without @Cancelable.
            // Implement the target contract so NeoForge can observe its cancel state,
            // while recording that this interface did not come from source annotation.
            addInterface(classNode,
                    "legacyconnector/v1_20_1/net/minecraftforge/eventbus/api/Forge1201CustomCancelableOverrideMarker");
            addInterface(classNode, TARGET_CANCELLABLE);
        }

        int roots = 0;
        if (eventRoot) {
            injectMissingSourceMethods(classNode);
            roots = 1;
        }

        int callSites = 0;
        int superCalls = 0;
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                        && isSourceEventCancellationMethod(call.owner, call.name, call.desc)) {
                    if (call.getOpcode() == Opcodes.INVOKESPECIAL) {
                        if (hasAuditedTargetSuperImplementation(call.owner, call.name, call.desc)) {
                            // Preserve the target's corresponding override (currently the
                            // PlayerInteractEvent.RightClickBlock setCanceled special case).
                            continue;
                        }
                        if (!(SOURCE_EVENT.equals(call.owner)
                                || SOURCE_EVENT_TYPES_INHERITING_CANCELLATION_METHODS.contains(call.owner))
                                || !eventRoot) {
                            throw new IOException("Unverified Forge event superclass override for non-virtual "
                                    + "cancellation call in " + classNode.name + "." + method.name + method.desc
                                    + ": owner=" + call.owner + "." + call.name + call.desc
                                    + ". The owner is not in the pinned source/target superclass policy.");
                        }
                        rewriteSuperCall(call);
                        superCalls++;
                    } else if (call.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        rewriteVirtualCall(call);
                        callSites++;
                    } else {
                        throw new IOException("Unsupported opcode " + call.getOpcode()
                                + " for Forge Event cancellation call in "
                                + classNode.name + "." + method.name + method.desc);
                    }
                } else if (containsUnsupportedEventHandle(instruction)) {
                    throw new IOException("Method-handle reference to a Forge Event cancellation method "
                            + "requires a dedicated LambdaMetafactory adapter: "
                            + classNode.name + "." + method.name + method.desc);
                }
            }
        }

        return new Stats(roots, directlyCancelable ? 1 : 0, callSites, superCalls);
    }

    private static boolean isForgeEventBase(String internalName) {
        return internalName != null && (SOURCE_EVENT.equals(internalName)
                || SOURCE_EVENT_BASE_PREFIXES.stream().anyMatch(internalName::startsWith));
    }

    private static boolean isSourceEventCancellationMethod(String owner, String name, String descriptor) {
        return isForgeEventBase(owner)
                && ((METHOD_IS_CANCELABLE.equals(name) && IS_CANCELABLE_DESCRIPTOR.equals(descriptor))
                || (METHOD_IS_CANCELED.equals(name) && IS_CANCELED_DESCRIPTOR.equals(descriptor))
                || (METHOD_SET_CANCELED.equals(name) && SET_CANCELED_DESCRIPTOR.equals(descriptor)));
    }

    private static boolean removeCancelableAnnotation(ClassNode classNode) {
        int removed = removeCancelableAnnotation(classNode.visibleAnnotations);
        removed += removeCancelableAnnotation(classNode.invisibleAnnotations);
        return removed > 0;
    }

    private static int removeCancelableAnnotation(List<AnnotationNode> annotations) {
        if (annotations == null || annotations.isEmpty()) return 0;
        int before = annotations.size();
        annotations.removeIf(annotation -> SOURCE_CANCELABLE_DESCRIPTOR.equals(annotation.desc));
        return before - annotations.size();
    }

    private static void addInterface(ClassNode classNode, String interfaceName) {
        if (!classNode.interfaces.contains(interfaceName)) {
            classNode.interfaces.add(interfaceName);
        }
    }

    private static boolean declaresInstanceMethod(ClassNode classNode, String name, String descriptor) {
        return classNode.methods.stream().anyMatch(method ->
                name.equals(method.name)
                        && descriptor.equals(method.desc)
                        && (method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC)) == 0);
    }

    private static void injectMissingSourceMethods(ClassNode classNode) {
        if (!declaresAnyMethod(classNode, METHOD_IS_CANCELABLE, IS_CANCELABLE_DESCRIPTOR)) {
            MethodNode method = new MethodNode(
                    Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, METHOD_IS_CANCELABLE, IS_CANCELABLE_DESCRIPTOR, null, null);
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC, BRIDGE, BASE_IS_CANCELABLE,
                    "(" + SOURCE_EVENT_DESCRIPTOR + ")Z", false));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));
            method.maxStack = 1;
            method.maxLocals = 1;
            classNode.methods.add(method);
        }

        if (!declaresAnyMethod(classNode, METHOD_IS_CANCELED, IS_CANCELED_DESCRIPTOR)) {
            MethodNode method = new MethodNode(
                    Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, METHOD_IS_CANCELED, IS_CANCELED_DESCRIPTOR, null, null);
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC, BRIDGE, BASE_IS_CANCELED,
                    "(" + SOURCE_EVENT_DESCRIPTOR + ")Z", false));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));
            method.maxStack = 1;
            method.maxLocals = 1;
            classNode.methods.add(method);
        }

        if (!declaresAnyMethod(classNode, METHOD_SET_CANCELED, SET_CANCELED_DESCRIPTOR)) {
            MethodNode method = new MethodNode(
                    Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, METHOD_SET_CANCELED, SET_CANCELED_DESCRIPTOR, null, null);
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
            method.instructions.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC, BRIDGE, BASE_SET_CANCELED,
                    "(" + SOURCE_EVENT_DESCRIPTOR + "Z)V", false));
            method.instructions.add(new InsnNode(Opcodes.RETURN));
            method.maxStack = 2;
            method.maxLocals = 2;
            classNode.methods.add(method);
        }
    }

    private static boolean declaresAnyMethod(ClassNode classNode, String name, String descriptor) {
        return classNode.methods.stream().anyMatch(method ->
                name.equals(method.name) && descriptor.equals(method.desc));
    }

    private static void rewriteSuperCall(MethodInsnNode call) {
        String bridgeName = switch (call.name) {
            case METHOD_IS_CANCELABLE -> BASE_IS_CANCELABLE;
            case METHOD_IS_CANCELED -> BASE_IS_CANCELED;
            case METHOD_SET_CANCELED -> BASE_SET_CANCELED;
            default -> throw new AssertionError(call.name);
        };
        String bridgeDescriptor = "(" + SOURCE_EVENT_DESCRIPTOR
                + (METHOD_SET_CANCELED.equals(call.name) ? "Z)V" : ")Z");
        call.setOpcode(Opcodes.INVOKESTATIC);
        call.owner = BRIDGE;
        call.name = bridgeName;
        call.desc = bridgeDescriptor;
        call.itf = false;
    }

    private static void rewriteVirtualCall(MethodInsnNode call) {
        String bridgeDescriptor = "(" + SOURCE_EVENT_DESCRIPTOR + call.desc.substring(1);
        call.setOpcode(Opcodes.INVOKESTATIC);
        call.owner = BRIDGE;
        call.desc = bridgeDescriptor;
        call.itf = false;
    }

    private static boolean containsUnsupportedEventHandle(AbstractInsnNode instruction) {
        if (instruction instanceof org.objectweb.asm.tree.LdcInsnNode ldc) {
            return containsUnsupportedEventHandleValue(ldc.cst);
        }
        if (instruction instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode dynamic) {
            if (containsEventMethodHandle(dynamic.bsm)) return true;
            for (Object argument : dynamic.bsmArgs) {
                if (containsUnsupportedEventHandleValue(argument)) return true;
            }
        }
        return false;
    }

    private static boolean containsUnsupportedEventHandleValue(Object value) {
        if (value instanceof Handle handle) return containsEventMethodHandle(handle);
        if (value instanceof ConstantDynamic dynamic) {
            if (containsEventMethodHandle(dynamic.getBootstrapMethod())) return true;
            for (int index = 0; index < dynamic.getBootstrapMethodArgumentCount(); index++) {
                if (containsUnsupportedEventHandleValue(dynamic.getBootstrapMethodArgument(index))) return true;
            }
        }
        return false;
    }

    private static boolean containsEventMethodHandle(Handle handle) {
        return isSourceEventCancellationMethod(handle.getOwner(), handle.getName(), handle.getDesc());
    }
}
