package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Verifies all additional AlienEvo 1.20.1 inverse-view getter callers. */
class AlienEvoCameraRotationCallers1201Test implements Opcodes {
    private static ClassNode node(String name) {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        return node;
    }

    private static MethodNode method(ClassNode node, String name, String descriptor) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, name, descriptor, null, null);
        node.methods.add(method);
        return method;
    }

    private static void call(MethodNode method, String owner) {
        method.instructions.add(new MethodInsnNode(
                INVOKESTATIC,
                owner,
                AlienEvoCameraRotationCallers1201.LEGACY_NAME,
                AlienEvoCameraRotationCallers1201.DESCRIPTOR,
                false));
        method.instructions.add(new InsnNode(ARETURN));
        method.maxStack = 1;
    }

    private static ClassNode grapple() {
        ClassNode node = node(AlienEvoCameraRotationCallers1201.GRAPPLE_ABILITY);
        call(method(node,
                AlienEvoCameraRotationCallers1201.GRAPPLE_METHOD,
                AlienEvoCameraRotationCallers1201.GRAPPLE_DESCRIPTOR),
                AlienEvoCameraRotationCallers1201.LEGACY_OWNER);
        return node;
    }

    private static ClassNode grabPlacement() {
        ClassNode node = node(AlienEvoCameraRotationCallers1201.GRAB_RENDER_PLACEMENT);
        call(method(node,
                AlienEvoCameraRotationCallers1201.GRAB_WORLD_ANCHOR,
                AlienEvoCameraRotationCallers1201.GRAB_WORLD_ANCHOR_DESCRIPTOR),
                AlienEvoCameraRotationCallers1201.LEGACY_OWNER);
        call(method(node,
                AlienEvoCameraRotationCallers1201.CAPTURE_CARRIED_PLAYER_EYE,
                AlienEvoCameraRotationCallers1201.CAPTURE_CARRIED_PLAYER_EYE_DESCRIPTOR),
                AlienEvoCameraRotationCallers1201.LEGACY_OWNER);
        return node;
    }

    private static ClassNode plumberScan() {
        ClassNode node = node(AlienEvoCameraRotationCallers1201.PLUMBER_SCAN_HOLOGRAM);
        call(method(node,
                AlienEvoCameraRotationCallers1201.PLUMBER_RENDER_WORLD,
                AlienEvoCameraRotationCallers1201.PLUMBER_RENDER_WORLD_DESCRIPTOR),
                AlienEvoCameraRotationCallers1201.LEGACY_OWNER);
        return node;
    }

    private static int countCalls(ClassNode node, String owner) {
        int count = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (var instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode call
                        && owner.equals(call.owner)
                        && AlienEvoCameraRotationCallers1201.LEGACY_NAME.equals(call.name)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void bridgesGrappleVisualOriginCameraRotationAndIsIdempotent() {
        ClassNode node = grapple();

        assertEquals(1, AlienEvoCameraRotationCallers1201.apply(node));
        assertEquals(0, countCalls(node, AlienEvoCameraRotationCallers1201.LEGACY_OWNER));
        assertEquals(1, countCalls(node, AlienEvoCameraRotationCallers1201.HELPER_OWNER));
        assertEquals(0, AlienEvoCameraRotationCallers1201.apply(node));
    }

    @Test
    void bridgesBothGrabRenderPlacementCameraRotationCallSites() {
        ClassNode node = grabPlacement();

        assertEquals(2, AlienEvoCameraRotationCallers1201.apply(node));
        assertEquals(0, countCalls(node, AlienEvoCameraRotationCallers1201.LEGACY_OWNER));
        assertEquals(2, countCalls(node, AlienEvoCameraRotationCallers1201.HELPER_OWNER));
        assertEquals(0, AlienEvoCameraRotationCallers1201.apply(node));
    }

    @Test
    void bridgesPlumberScanWorldHologramCameraRotationAndIsIdempotent() {
        ClassNode node = plumberScan();

        assertEquals(1, AlienEvoCameraRotationCallers1201.apply(node));
        assertEquals(0, countCalls(node, AlienEvoCameraRotationCallers1201.LEGACY_OWNER));
        assertEquals(1, countCalls(node, AlienEvoCameraRotationCallers1201.HELPER_OWNER));
        assertEquals(0, AlienEvoCameraRotationCallers1201.apply(node));
    }

    @Test
    void rejectsPartialAndUnexpectedCameraGetterReferences() {
        ClassNode partial = grabPlacement();
        MethodNode second = partial.methods.get(1);
        second.instructions.clear();
        assertThrows(IllegalStateException.class,
                () -> AlienEvoCameraRotationCallers1201.apply(partial));

        ClassNode wrongOwner = grapple();
        firstCall(wrongOwner).owner = "example/UnknownCamera";
        assertThrows(IllegalStateException.class,
                () -> AlienEvoCameraRotationCallers1201.apply(wrongOwner));

        ClassNode wrongOpcode = plumberScan();
        firstCall(wrongOpcode).setOpcode(INVOKEVIRTUAL);
        assertThrows(IllegalStateException.class,
                () -> AlienEvoCameraRotationCallers1201.apply(wrongOpcode));
    }

    @Test
    void rejectsAdditionalCallOutsideTheInspectedMethods() {
        ClassNode node = grapple();
        MethodNode unexpected = method(node, "unexpected", "()V");
        unexpected.instructions.add(new MethodInsnNode(
                INVOKESTATIC,
                AlienEvoCameraRotationCallers1201.LEGACY_OWNER,
                AlienEvoCameraRotationCallers1201.LEGACY_NAME,
                AlienEvoCameraRotationCallers1201.DESCRIPTOR,
                false));
        unexpected.instructions.add(new InsnNode(RETURN));
        assertThrows(IllegalStateException.class,
                () -> AlienEvoCameraRotationCallers1201.apply(node));
    }

    @Test
    void ignoresUnrelatedClassesWithoutChangingTheirMethods() {
        ClassNode node = node("example/OtherClass");
        MethodNode unrelated = method(node, "other", "()V");
        unrelated.instructions.add(new InsnNode(RETURN));

        assertEquals(0, AlienEvoCameraRotationCallers1201.apply(node));
        assertEquals(0, countCalls(node, AlienEvoCameraRotationCallers1201.LEGACY_OWNER));
        assertFalse(unrelated.instructions.getFirst().getOpcode() == INVOKESTATIC);
        assertTrue(unrelated.instructions.getFirst().getOpcode() == RETURN);
    }

    private static MethodInsnNode firstCall(ClassNode node) {
        for (MethodNode method : node.methods) {
            for (var instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode call) return call;
            }
        }
        throw new AssertionError("Camera getter call not found");
    }
}
