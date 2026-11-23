package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for AlienEvo's named-bone coordinate-space conversion. */
class AlienEvoBoneAnchors1201Test implements Opcodes {
    private static ClassNode boneAnchors() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.name = AlienEvoBoneAnchors1201.TARGET;
        node.superName = "java/lang/Object";

        MethodNode inverseView = new MethodNode(ACC_PUBLIC | ACC_STATIC,
                "invView", "()Lorg/joml/Matrix3f;", null, null);
        inverseView.instructions.add(new MethodInsnNode(INVOKESTATIC,
                AlienEvoBoneAnchors1201.RENDER_SYSTEM,
                AlienEvoBoneAnchors1201.LEGACY_NAME,
                AlienEvoBoneAnchors1201.DESC,
                false));
        inverseView.instructions.add(new InsnNode(ARETURN));
        inverseView.maxStack = 1;
        node.methods.add(inverseView);

        MethodNode boneWorld = new MethodNode(ACC_PUBLIC | ACC_STATIC,
                AlienEvoBoneAnchors1201.BONE_WORLD,
                AlienEvoBoneAnchors1201.BONE_WORLD_DESC,
                null,
                null);
        boneWorld.instructions.add(new VarInsnNode(ALOAD, 7));
        boneWorld.instructions.add(new InsnNode(DUP));
        boneWorld.instructions.add(new FieldInsnNode(GETFIELD,
                AlienEvoBoneAnchors1201.VECTOR3F, "x", "F"));
        boneWorld.instructions.add(new VarInsnNode(FLOAD, 3));
        boneWorld.instructions.add(new InsnNode(FMUL));
        boneWorld.instructions.add(new FieldInsnNode(PUTFIELD,
                AlienEvoBoneAnchors1201.VECTOR3F, "x", "F"));
        boneWorld.instructions.add(new VarInsnNode(ALOAD, 7));
        boneWorld.instructions.add(new InsnNode(DUP));
        boneWorld.instructions.add(new FieldInsnNode(GETFIELD,
                AlienEvoBoneAnchors1201.VECTOR3F, "y", "F"));
        boneWorld.instructions.add(new VarInsnNode(FLOAD, 3));
        boneWorld.instructions.add(new InsnNode(FMUL));
        boneWorld.instructions.add(new FieldInsnNode(PUTFIELD,
                AlienEvoBoneAnchors1201.VECTOR3F, "y", "F"));
        boneWorld.instructions.add(new VarInsnNode(ALOAD, 4));
        boneWorld.instructions.add(new VarInsnNode(ALOAD, 7));
        boneWorld.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                AlienEvoBoneAnchors1201.MATRIX3F,
                "transform",
                AlienEvoBoneAnchors1201.VECTOR3F_TRANSFORM_DESC,
                false));
        boneWorld.instructions.add(new InsnNode(POP));
        boneWorld.instructions.add(new VarInsnNode(ALOAD, 5));
        boneWorld.instructions.add(new InsnNode(ARETURN));
        boneWorld.maxStack = 3;
        boneWorld.maxLocals = 8;
        node.methods.add(boneWorld);

        return node;
    }

    @Test
    void redirectsCameraGetterAndSkipsInverseViewForThirdPersonBoneMatrices() {
        ClassNode node = boneAnchors();

        assertEquals(1, AlienEvoBoneAnchors1201.apply(node));

        MethodInsnNode cameraCall = findCall(findMethod(node, "invView", AlienEvoBoneAnchors1201.DESC));
        assertEquals(INVOKESTATIC, cameraCall.getOpcode());
        assertEquals(AlienEvoBoneAnchors1201.HELPER, cameraCall.owner);
        assertEquals(AlienEvoBoneAnchors1201.HELPER_NAME, cameraCall.name);
        assertEquals(AlienEvoBoneAnchors1201.DESC, cameraCall.desc);

        MethodNode boneWorld = findMethod(node, AlienEvoBoneAnchors1201.BONE_WORLD,
                AlienEvoBoneAnchors1201.BONE_WORLD_DESC);
        MethodInsnNode transform = findTransform(boneWorld);
        assertNotNull(transform);

        AbstractInsnNode vectorLoad = previousMeaningful(transform);
        AbstractInsnNode matrixLoad = previousMeaningful(vectorLoad);
        AbstractInsnNode branch = previousMeaningful(matrixLoad);
        assertTrue(branch instanceof JumpInsnNode);
        JumpInsnNode skipThirdPerson = (JumpInsnNode)branch;

        AbstractInsnNode firstPersonTest = previousMeaningful(branch);
        assertTrue(firstPersonTest instanceof VarInsnNode);
        VarInsnNode firstPersonLoad = (VarInsnNode)firstPersonTest;
        assertEquals(ILOAD, firstPersonLoad.getOpcode());
        assertEquals(2, firstPersonLoad.var);

        assertEquals(IFEQ, skipThirdPerson.getOpcode());
        AbstractInsnNode afterTransform = nextMeaningful(transform);
        assertTrue(afterTransform instanceof InsnNode);
        InsnNode pop = (InsnNode)afterTransform;
        assertEquals(POP, pop.getOpcode());
        assertEquals(pop.getNext(), skipThirdPerson.label);

        assertEquals(0, AlienEvoBoneAnchors1201.apply(node));
    }

    @Test
    void failsClosedWhenTheLegacyCameraCallShapeChanges() {
        ClassNode node = boneAnchors();
        findCall(findMethod(node, "invView", AlienEvoBoneAnchors1201.DESC)).desc = "()Ljava/lang/Object;";

        assertThrows(IllegalStateException.class, () -> AlienEvoBoneAnchors1201.apply(node));
    }

    @Test
    void failsClosedWhenTheBoneTransformCallShapeChanges() {
        ClassNode node = boneAnchors();
        findTransform(findMethod(node, AlienEvoBoneAnchors1201.BONE_WORLD,
                AlienEvoBoneAnchors1201.BONE_WORLD_DESC)).setOpcode(INVOKESTATIC);

        assertThrows(IllegalStateException.class, () -> AlienEvoBoneAnchors1201.apply(node));
    }

    @Test
    void failsClosedWhenTheInspectedCameraCallIsMissing() {
        ClassNode node = boneAnchors();
        MethodNode inverseView = findMethod(node, "invView", AlienEvoBoneAnchors1201.DESC);
        inverseView.instructions.clear();
        inverseView.instructions.add(new InsnNode(ARETURN));

        assertThrows(IllegalStateException.class, () -> AlienEvoBoneAnchors1201.apply(node));
    }

    @Test
    void leavesOtherClassesUntouched() {
        ClassNode node = boneAnchors();
        node.name = "example/OtherClass";

        assertEquals(0, AlienEvoBoneAnchors1201.apply(node));
    }

    private static MethodNode findMethod(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return method;
        }
        throw new AssertionError("Method not found: " + name + descriptor);
    }

    private static MethodInsnNode findCall(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call) return call;
        }
        throw new AssertionError("No method call found");
    }

    private static MethodInsnNode findTransform(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && AlienEvoBoneAnchors1201.MATRIX3F.equals(call.owner)
                    && "transform".equals(call.name)) return call;
        }
        throw new AssertionError("Matrix3f.transform call not found");
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode value = instruction == null ? null : instruction.getPrevious();
        while (value != null && value.getOpcode() < 0) value = value.getPrevious();
        return value;
    }

    private static AbstractInsnNode nextMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode value = instruction == null ? null : instruction.getNext();
        while (value != null && value.getOpcode() < 0) value = value.getNext();
        return value;
    }
}
