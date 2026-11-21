package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Regression tests for AlienEvo's removed RenderSystem inverse-view getter. */
class AlienEvoBoneAnchors1201Test implements Opcodes {
    private static ClassNode boneAnchors() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.name = AlienEvoBoneAnchors1201.TARGET;
        node.superName = "java/lang/Object";

        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "invView", "()Lorg/joml/Matrix3f;", null, null);
        method.instructions.add(new MethodInsnNode(INVOKESTATIC, AlienEvoBoneAnchors1201.RENDER_SYSTEM,
                AlienEvoBoneAnchors1201.LEGACY_NAME, AlienEvoBoneAnchors1201.DESC, false));
        method.instructions.add(new InsnNode(ARETURN));
        method.maxStack = 1;
        node.methods.add(method);
        return node;
    }

    @Test
    void redirectsTheRemovedGetterToTheCameraRotationRuntimeBridge() {
        ClassNode node = boneAnchors();

        assertEquals(1, AlienEvoBoneAnchors1201.apply(node));

        MethodInsnNode call = findCall(node.methods.get(0));
        assertEquals(INVOKESTATIC, call.getOpcode());
        assertEquals(AlienEvoBoneAnchors1201.HELPER, call.owner);
        assertEquals(AlienEvoBoneAnchors1201.HELPER_NAME, call.name);
        assertEquals(AlienEvoBoneAnchors1201.DESC, call.desc);
        assertEquals(0, AlienEvoBoneAnchors1201.apply(node));
    }

    @Test
    void failsClosedWhenTheLegacyCallShapeChanges() {
        ClassNode node = boneAnchors();
        MethodInsnNode call = findCall(node.methods.get(0));
        call.desc = "()Ljava/lang/Object;";

        assertThrows(IllegalStateException.class, () -> AlienEvoBoneAnchors1201.apply(node));
    }

    @Test
    void failsClosedWhenTheInspectedCallIsMissing() {
        ClassNode node = boneAnchors();
        node.methods.get(0).instructions.clear();
        node.methods.get(0).instructions.add(new InsnNode(ARETURN));

        assertThrows(IllegalStateException.class, () -> AlienEvoBoneAnchors1201.apply(node));
    }

    @Test
    void leavesOtherClassesUntouched() {
        ClassNode node = boneAnchors();
        node.name = "example/OtherClass";

        assertEquals(0, AlienEvoBoneAnchors1201.apply(node));
    }

    private static MethodInsnNode findCall(MethodNode method) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call) return call;
        }
        throw new AssertionError("No method call found");
    }
}
