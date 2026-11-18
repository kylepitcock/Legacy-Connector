package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

final class Forge1201EventAdapterTest {
    @Test
    void plainEventRootGetsCompatibilityMethodsButNotTargetCancelability() throws Exception {
        ClassNode node = eventRoot("sample/PlainEvent");

        Forge1201EventAdapter.Stats stats = Forge1201EventAdapter.apply(node);

        assertEquals(1, stats.eventRoots());
        assertEquals(0, stats.cancelableClasses());
        assertTrue(node.interfaces.contains(Forge1201EventAdapter.COMPAT_INTERFACE));
        assertFalse(node.interfaces.contains(Forge1201EventAdapter.CANCELABLE_MARKER));
        assertFalse(node.interfaces.contains(Forge1201EventAdapter.TARGET_CANCELLABLE));
        assertTrue(hasMethod(node, "isCancelable", "()Z"));
        assertTrue(hasMethod(node, "isCanceled", "()Z"));
        assertTrue(hasMethod(node, "setCanceled", "(Z)V"));
    }


    @Test
    void eventBusApiUtilitySuperclassIsNotMistakenForForgeEventRoot() throws Exception {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "sample/NotAnEvent";
        node.superName = "net/minecraftforge/eventbus/api/EventPriority";

        Forge1201EventAdapter.apply(node);

        assertFalse(node.interfaces.contains(Forge1201EventAdapter.COMPAT_INTERFACE));
        assertFalse(hasMethod(node, "isCancelable", "()Z"));
    }

    @Test
    void directCancelableAnnotationBecomesMarkerAndTargetInterface() throws Exception {
        ClassNode node = eventRoot("sample/CancelableEvent");
        node.visibleAnnotations = new ArrayList<>();
        node.visibleAnnotations.add(new AnnotationNode(Forge1201EventAdapter.SOURCE_CANCELABLE_DESCRIPTOR));

        Forge1201EventAdapter.Stats stats = Forge1201EventAdapter.apply(node);

        assertEquals(1, stats.eventRoots());
        assertEquals(1, stats.cancelableClasses());
        assertTrue(node.interfaces.contains(Forge1201EventAdapter.COMPAT_INTERFACE));
        assertTrue(node.interfaces.contains(Forge1201EventAdapter.CANCELABLE_MARKER));
        assertTrue(node.interfaces.contains(Forge1201EventAdapter.TARGET_CANCELLABLE));
        assertFalse(hasAnnotation(node, Forge1201EventAdapter.SOURCE_CANCELABLE_DESCRIPTOR));
    }

    @Test
    void directCancelableChildOfPlainParentGetsOnlyItsOwnCancelableInterfaces() throws Exception {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "sample/CancelableChild";
        node.superName = "sample/PlainEvent";
        node.visibleAnnotations = new ArrayList<>();
        node.visibleAnnotations.add(new AnnotationNode(Forge1201EventAdapter.SOURCE_CANCELABLE_DESCRIPTOR));

        Forge1201EventAdapter.apply(node);

        assertTrue(node.interfaces.contains(Forge1201EventAdapter.COMPAT_INTERFACE));
        assertTrue(node.interfaces.contains(Forge1201EventAdapter.CANCELABLE_MARKER));
        assertTrue(node.interfaces.contains(Forge1201EventAdapter.TARGET_CANCELLABLE));
        assertFalse(hasAnnotation(node, Forge1201EventAdapter.SOURCE_CANCELABLE_DESCRIPTOR));
        assertFalse(hasMethod(node, "isCanceled", "()Z"),
                "The child inherits the source Event methods from its transformed parent.");
    }

    @Test
    void sourceSuperCancellationCallsBecomeNonVirtualBridgeCalls() throws Exception {
        ClassNode node = eventRoot("sample/OverrideEvent");
        MethodNode method = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC, "setCanceled", "(Z)V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                Forge1201EventAdapter.SOURCE_EVENT,
                "setCanceled",
                "(Z)V",
                false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 2;
        method.maxLocals = 2;
        node.methods.add(method);

        Forge1201EventAdapter.Stats stats = Forge1201EventAdapter.apply(node);

        assertEquals(1, stats.superCallsRewritten());
        MethodInsnNode rewritten = firstMethodCall(method);
        assertEquals(Opcodes.INVOKESTATIC, rewritten.getOpcode());
        assertEquals(Forge1201EventAdapter.BRIDGE, rewritten.owner);
        assertEquals("baseSetCanceled", rewritten.name);
        assertEquals("(L" + Forge1201EventAdapter.SOURCE_EVENT + ";Z)V", rewritten.desc);
        assertFalse(rewritten.itf);
    }



    @Test
    void auditedPlayerInteractBaseSuperCallBecomesSourceBaseBridge() throws Exception {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "sample/CustomInteractEvent";
        node.superName = "net/minecraftforge/event/entity/player/PlayerInteractEvent";
        MethodNode method = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC, "setCanceled", "(Z)V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "net/minecraftforge/event/entity/player/PlayerInteractEvent",
                "setCanceled",
                "(Z)V",
                false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 2;
        method.maxLocals = 2;
        node.methods.add(method);

        Forge1201EventAdapter.Stats stats = Forge1201EventAdapter.apply(node);

        assertEquals(1, stats.superCallsRewritten());
        MethodInsnNode rewritten = firstMethodCall(method);
        assertEquals(Opcodes.INVOKESTATIC, rewritten.getOpcode());
        assertEquals(Forge1201EventAdapter.BRIDGE, rewritten.owner);
        assertEquals("baseSetCanceled", rewritten.name);
    }

    @Test
    void auditedRightClickBlockSetCanceledSuperCallKeepsTargetOverrideDispatch() throws Exception {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "sample/CustomRightClickBlockEvent";
        node.superName = "net/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock";
        MethodNode method = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC, "setCanceled", "(Z)V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "net/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock",
                "setCanceled",
                "(Z)V",
                false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 2;
        method.maxLocals = 2;
        node.methods.add(method);

        Forge1201EventAdapter.Stats stats = Forge1201EventAdapter.apply(node);

        assertEquals(0, stats.superCallsRewritten());
        MethodInsnNode preserved = firstMethodCall(method);
        assertEquals(Opcodes.INVOKESPECIAL, preserved.getOpcode());
        assertEquals(
                "net/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock",
                preserved.owner);
        assertEquals("setCanceled", preserved.name);
        assertEquals("(Z)V", preserved.desc);
    }

    @Test
    void unverifiedIntermediateForgeEventSuperCallFailsClosed() {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "sample/IntermediateOverrideEvent";
        node.superName = "net/minecraftforge/event/entity/EntityEvent";
        MethodNode method = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC, "setCanceled", "(Z)V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "net/minecraftforge/event/entity/EntityEvent",
                "setCanceled",
                "(Z)V",
                false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 2;
        method.maxLocals = 2;
        node.methods.add(method);

        IOException failure = assertThrows(IOException.class, () -> Forge1201EventAdapter.apply(node));
        assertTrue(failure.getMessage().contains("Unverified Forge event superclass override"));
    }

    @Test
    void ordinarySourceEventCallsBecomeStaticVirtualDispatchBridges() throws Exception {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "sample/EventCaller";
        node.superName = "java/lang/Object";
        MethodNode method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, "probe", "()Z", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                Forge1201EventAdapter.SOURCE_EVENT,
                "isCanceled",
                "()Z",
                false));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.maxStack = 1;
        method.maxLocals = 2;
        node.methods.add(method);

        Forge1201EventAdapter.Stats stats = Forge1201EventAdapter.apply(node);

        assertEquals(1, stats.callSitesRewritten());
        MethodInsnNode rewritten = firstMethodCall(method);
        assertEquals(Opcodes.INVOKESTATIC, rewritten.getOpcode());
        assertEquals(Forge1201EventAdapter.BRIDGE, rewritten.owner);
        assertEquals("isCanceled", rewritten.name);
        assertEquals("(L" + Forge1201EventAdapter.SOURCE_EVENT + ";)Z", rewritten.desc);
    }

    @Test
    void preservesUserOverridesAndUnrelatedAnnotations() throws Exception {
        ClassNode node = eventRoot("sample/CustomEvent");
        node.visibleAnnotations = new ArrayList<>();
        node.visibleAnnotations.add(new AnnotationNode("Lsample/KeepMe;"));
        MethodNode custom = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, "isCancelable", "()Z", null, null);
        custom.instructions.add(new InsnNode(Opcodes.ICONST_0));
        custom.instructions.add(new InsnNode(Opcodes.IRETURN));
        custom.maxStack = 1;
        custom.maxLocals = 1;
        node.methods.add(custom);

        Forge1201EventAdapter.apply(node);

        assertEquals(1, matchingMethods(node, "isCancelable", "()Z").size());
        assertEquals("Lsample/KeepMe;", node.visibleAnnotations.get(0).desc);
        assertTrue(node.interfaces.contains(Forge1201EventAdapter.COMPAT_INTERFACE));
        assertTrue(node.interfaces.contains(Forge1201EventAdapter.TARGET_CANCELLABLE),
                "A custom isCancelable override needs target cancellation state support.");
    }

    @Test
    void repeatedApplicationDoesNotDuplicateInterfacesOrMethods() throws Exception {
        ClassNode node = eventRoot("sample/IdempotentEvent");
        node.visibleAnnotations = new ArrayList<>();
        node.visibleAnnotations.add(new AnnotationNode(Forge1201EventAdapter.SOURCE_CANCELABLE_DESCRIPTOR));

        Forge1201EventAdapter.apply(node);
        int methodCount = node.methods.size();
        Forge1201EventAdapter.apply(node);

        assertEquals(methodCount, node.methods.size());
        assertEquals(1, node.interfaces.stream()
                .filter(Forge1201EventAdapter.COMPAT_INTERFACE::equals).count());
        assertEquals(1, node.interfaces.stream()
                .filter(Forge1201EventAdapter.CANCELABLE_MARKER::equals).count());
        assertEquals(1, node.interfaces.stream()
                .filter(Forge1201EventAdapter.TARGET_CANCELLABLE::equals).count());
    }

    @Test
    void methodHandleReferencesFailClosedUntilLambdaAdaptationExists() {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "sample/EventMethodReference";
        node.superName = "java/lang/Object";
        MethodNode method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "create", "()V", null, null);
        method.instructions.add(new LdcInsnNode(new Handle(
                Opcodes.H_INVOKEVIRTUAL,
                Forge1201EventAdapter.SOURCE_EVENT,
                "isCancelable",
                "()Z",
                false)));
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 1;
        method.maxLocals = 0;
        node.methods.add(method);

        assertThrows(IOException.class, () -> Forge1201EventAdapter.apply(node));
    }

    private static ClassNode eventRoot(String name) {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = name;
        node.superName = Forge1201EventAdapter.SOURCE_EVENT;
        node.interfaces = new ArrayList<>();
        node.methods = new ArrayList<>();
        return node;
    }

    private static boolean hasMethod(ClassNode node, String name, String descriptor) {
        return !matchingMethods(node, name, descriptor).isEmpty();
    }

    private static List<MethodNode> matchingMethods(ClassNode node, String name, String descriptor) {
        return node.methods.stream()
                .filter(method -> name.equals(method.name) && descriptor.equals(method.desc))
                .toList();
    }

    private static boolean hasAnnotation(ClassNode node, String descriptor) {
        return (node.visibleAnnotations != null && node.visibleAnnotations.stream()
                .anyMatch(annotation -> descriptor.equals(annotation.desc)))
                || (node.invisibleAnnotations != null && node.invisibleAnnotations.stream()
                .anyMatch(annotation -> descriptor.equals(annotation.desc)));
    }

    private static MethodInsnNode firstMethodCall(MethodNode method) {
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) return call;
        }
        throw new AssertionError("Expected a method invocation");
    }
}
