package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Adapts AlienEvo's named-bone coordinate conversion to the 1.21.1 render spaces.
 *
 * Third-person entity PoseStacks contain camera-relative translations in world axes:
 * the view rotation is applied separately by Minecraft's model-view matrix. First-person
 * arm PoseStacks are view-model-relative, so only those bone translations are rotated
 * through the inverse-view matrix before the camera position is added.
 */
final class AlienEvoBoneAnchors1201 implements Opcodes {
    static final String TARGET = "net/habb/alienevo/client/BoneAnchors";
    static final String RENDER_SYSTEM = "com/mojang/blaze3d/systems/RenderSystem";
    static final String HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoCameraRotation1201Runtime";
    static final String DESC = "()Lorg/joml/Matrix3f;";
    static final String LEGACY_NAME = "getInverseViewRotationMatrix";
    static final String HELPER_NAME = "getInverseViewRotationMatrix";

    static final String BONE_WORLD = "boneWorld";
    static final String BONE_WORLD_DESC =
            "(ILjava/lang/String;ZFLorg/joml/Matrix3f;Lnet/minecraft/world/phys/Vec3;)"
                    + "Lnet/minecraft/world/phys/Vec3;";
    static final String MATRIX3F = "org/joml/Matrix3f";
    static final String VECTOR3F = "org/joml/Vector3f";
    static final String VECTOR3F_TRANSFORM_DESC = "(L" + VECTOR3F + ";)L" + VECTOR3F + ";";

    private AlienEvoBoneAnchors1201() {}

    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        MethodNode boneWorld = findUniqueMethod(node, BONE_WORLD, BONE_WORLD_DESC);
        if (boneWorld == null || (boneWorld.access & ACC_STATIC) == 0) {
            throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld has an unexpected method shape");
        }

        MethodInsnNode transform = findUniqueTransform(boneWorld);
        if (transform == null || transform.getOpcode() != INVOKEVIRTUAL
                || transform.itf || !VECTOR3F_TRANSFORM_DESC.equals(transform.desc)) {
            throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld inverse-view transform has an unknown bytecode shape");
        }

        AbstractInsnNode vectorLoad = previousMeaningful(transform);
        AbstractInsnNode matrixLoad = previousMeaningful(vectorLoad);
        AbstractInsnNode afterTransform = nextMeaningful(transform);
        if (!(vectorLoad instanceof VarInsnNode vector && vector.getOpcode() == ALOAD && vector.var == 7)
                || !(matrixLoad instanceof VarInsnNode matrix && matrix.getOpcode() == ALOAD && matrix.var == 4)
                || afterTransform == null || afterTransform.getOpcode() != POP) {
            throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld transform operands changed");
        }

        AbstractInsnNode beforeMatrixLoad = previousMeaningful(matrixLoad);
        boolean thirdPersonGuardPresent;
        if (beforeMatrixLoad instanceof JumpInsnNode branch) {
            AbstractInsnNode condition = previousMeaningful(branch);
            if (branch.getOpcode() != IFEQ
                    || !(condition instanceof VarInsnNode firstPerson && firstPerson.getOpcode() == ILOAD
                    && firstPerson.var == 2)
                    || !isAfter(branch.label, afterTransform)) {
                throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld has a partial or unknown first-person transform guard");
            }
            thirdPersonGuardPresent = true;
        } else {
            if (beforeMatrixLoad instanceof VarInsnNode load && load.getOpcode() == ILOAD && load.var == 2) {
                throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld has a partial first-person transform guard");
            }
            if (!(beforeMatrixLoad instanceof FieldInsnNode field
                    && field.getOpcode() == PUTFIELD
                    && VECTOR3F.equals(field.owner)
                    && "y".equals(field.name)
                    && "F".equals(field.desc))) {
                throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld coordinate-scale prelude changed");
            }
            thirdPersonGuardPresent = false;
        }

        int legacyCalls = 0;
        int helperCalls = 0;
        MethodInsnNode legacy = null;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                if (call.owner.equals(RENDER_SYSTEM) && call.name.equals(LEGACY_NAME)) {
                    legacyCalls++;
                    if (call.getOpcode() != INVOKESTATIC || !call.desc.equals(DESC) || call.itf) {
                        throw new IllegalStateException("AlienEvo BoneAnchors inverse-view call has an unknown bytecode shape");
                    }
                    legacy = call;
                } else if (call.owner.equals(HELPER) && call.name.equals(HELPER_NAME)) {
                    helperCalls++;
                    if (call.getOpcode() != INVOKESTATIC || !call.desc.equals(DESC) || call.itf) {
                        throw new IllegalStateException("AlienEvo BoneAnchors camera bridge call has an unknown bytecode shape");
                    }
                }
            }
        }

        boolean originalState = legacyCalls == 1 && helperCalls == 0 && !thirdPersonGuardPresent;
        boolean adaptedState = legacyCalls == 0 && helperCalls == 1 && thirdPersonGuardPresent;
        if (!originalState && !adaptedState) {
            throw new IllegalStateException("AlienEvo BoneAnchors expected the original or fully adapted shape; found "
                    + legacyCalls + " legacy calls, " + helperCalls + " bridge calls, and third-person guard="
                    + thirdPersonGuardPresent);
        }

        if (adaptedState) return 0;

        LabelNode skipThirdPersonRotation = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(ILOAD, 2));
        guard.add(new JumpInsnNode(IFEQ, skipThirdPersonRotation));
        boneWorld.instructions.insertBefore(matrixLoad, guard);
        boneWorld.instructions.insert(afterTransform, skipThirdPersonRotation);

        legacy.owner = HELPER;
        legacy.name = HELPER_NAME;
        legacy.itf = false;
        return 1;
    }

    private static MethodNode findUniqueMethod(ClassNode node, String name, String descriptor) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (!name.equals(method.name) || !descriptor.equals(method.desc)) continue;
            if (found != null) throw new IllegalStateException("Duplicate AlienEvo BoneAnchors." + name + descriptor);
            found = method;
        }
        return found;
    }

    private static MethodInsnNode findUniqueTransform(MethodNode method) {
        MethodInsnNode found = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode call)
                    || !MATRIX3F.equals(call.owner) || !"transform".equals(call.name)) continue;
            if (found != null) throw new IllegalStateException("Duplicate Matrix3f.transform call in AlienEvo BoneAnchors.boneWorld");
            found = call;
        }
        return found;
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

    private static boolean isAfter(AbstractInsnNode candidate, AbstractInsnNode anchor) {
        for (AbstractInsnNode instruction = anchor == null ? null : anchor.getNext();
                instruction != null; instruction = instruction.getNext()) {
            if (instruction == candidate) return true;
        }
        return false;
    }
}
