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

    static final String GECKO_MODEL_STORAGE = "net/habb/alienevo/client/GeckoModelStorage";
    static final String GET_NAMED_BONE_POSE_FP = "getNamedBonePoseFP";
    static final String GET_NAMED_BONE_POSE_FP_DESC =
            "(ILjava/lang/String;)Lorg/joml/Matrix4f;";
    static final String FIRST_PERSON_BONE_KEY_HELPER = "firstPersonArmBoneKey";
    static final String FIRST_PERSON_BONE_KEY_DESC =
            "(Ljava/lang/String;)Ljava/lang/String;";

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
        int firstPersonPoseCalls = 0;
        int firstPersonKeyCalls = 0;
        MethodInsnNode firstPersonPoseCall = null;
        MethodInsnNode firstPersonKeyCall = null;
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

                if (method == boneWorld
                        && call.owner.equals(GECKO_MODEL_STORAGE)
                        && call.name.equals(GET_NAMED_BONE_POSE_FP)) {
                    firstPersonPoseCalls++;
                    if (firstPersonPoseCall == null) firstPersonPoseCall = call;
                    if (call.getOpcode() != INVOKESTATIC
                            || !call.desc.equals(GET_NAMED_BONE_POSE_FP_DESC)
                            || call.itf) {
                        throw new IllegalStateException("AlienEvo BoneAnchors first-person named-bone lookup has an unknown bytecode shape");
                    }
                } else if (method == boneWorld
                        && call.owner.equals(HELPER)
                        && call.name.equals(FIRST_PERSON_BONE_KEY_HELPER)) {
                    firstPersonKeyCalls++;
                    firstPersonKeyCall = call;
                    if (call.getOpcode() != INVOKESTATIC
                            || !call.desc.equals(FIRST_PERSON_BONE_KEY_DESC)
                            || call.itf) {
                        throw new IllegalStateException("AlienEvo BoneAnchors first-person arm-key helper has an unknown bytecode shape");
                    }
                }
            }
        }

        boolean cameraOriginal = legacyCalls == 1 && helperCalls == 0 && !thirdPersonGuardPresent;
        boolean cameraAdapted = legacyCalls == 0 && helperCalls == 1 && thirdPersonGuardPresent;
        if (!cameraOriginal && !cameraAdapted) {
            throw new IllegalStateException("AlienEvo BoneAnchors expected the original or adapted camera shape; found "
                    + legacyCalls + " legacy calls, " + helperCalls + " bridge calls, and third-person guard="
                    + thirdPersonGuardPresent);
        }

        boolean firstPersonLookupOriginal = firstPersonPoseCalls == 1 && firstPersonKeyCalls == 0;
        boolean firstPersonLookupAdapted = firstPersonPoseCalls == 2 && firstPersonKeyCalls == 1
                && hasFirstPersonArmKeyFallback(boneWorld, firstPersonPoseCall, firstPersonKeyCall);
        if (!firstPersonLookupOriginal && !firstPersonLookupAdapted) {
            throw new IllegalStateException("AlienEvo BoneAnchors expected the original or arm-qualified first-person lookup; found "
                    + firstPersonPoseCalls + " getNamedBonePoseFP calls and "
                    + firstPersonKeyCalls + " arm-key helper calls");
        }

        if (cameraAdapted && firstPersonLookupAdapted) return 0;
        if (cameraOriginal && firstPersonLookupAdapted) {
            throw new IllegalStateException("AlienEvo BoneAnchors has a first-person arm-key fallback without the expected camera adapter");
        }

        int changed = 0;
        if (firstPersonLookupOriginal) {
            AbstractInsnNode boneNameLoad = previousMeaningful(firstPersonPoseCall);
            AbstractInsnNode playerIdLoad = previousMeaningful(boneNameLoad);
            AbstractInsnNode branch = previousMeaningful(playerIdLoad);
            AbstractInsnNode firstPersonFlag = previousMeaningful(branch);
            AbstractInsnNode afterFirstPersonLookup = nextMeaningful(firstPersonPoseCall);
            if (!(boneNameLoad instanceof VarInsnNode boneLoad
                    && boneLoad.getOpcode() == ALOAD && boneLoad.var == 1)
                    || !(playerIdLoad instanceof VarInsnNode idLoad
                    && idLoad.getOpcode() == ILOAD && idLoad.var == 0)
                    || !(branch instanceof JumpInsnNode firstPersonJump && firstPersonJump.getOpcode() == IFEQ)
                    || !(firstPersonFlag instanceof VarInsnNode flagLoad
                    && flagLoad.getOpcode() == ILOAD && flagLoad.var == 2)
                    || !(afterFirstPersonLookup instanceof JumpInsnNode jump
                    && jump.getOpcode() == GOTO)) {
                throw new IllegalStateException("AlienEvo BoneAnchors first-person named-bone lookup prelude changed");
            }

            LabelNode useArmQualifiedBone = new LabelNode();
            InsnList fallback = new InsnList();
            fallback.add(new InsnNode(DUP));
            fallback.add(new JumpInsnNode(IFNONNULL, useArmQualifiedBone));
            fallback.add(new InsnNode(POP));
            fallback.add(new VarInsnNode(ILOAD, 0));
            fallback.add(new VarInsnNode(ALOAD, 1));
            fallback.add(new MethodInsnNode(INVOKESTATIC, HELPER,
                    FIRST_PERSON_BONE_KEY_HELPER, FIRST_PERSON_BONE_KEY_DESC, false));
            fallback.add(new MethodInsnNode(INVOKESTATIC, GECKO_MODEL_STORAGE,
                    GET_NAMED_BONE_POSE_FP, GET_NAMED_BONE_POSE_FP_DESC, false));
            fallback.add(useArmQualifiedBone);
            boneWorld.instructions.insert(firstPersonPoseCall, fallback);
            changed++;
        }

        if (cameraOriginal) {
            LabelNode skipThirdPersonRotation = new LabelNode();
            InsnList guard = new InsnList();
            guard.add(new VarInsnNode(ILOAD, 2));
            guard.add(new JumpInsnNode(IFEQ, skipThirdPersonRotation));
            boneWorld.instructions.insertBefore(matrixLoad, guard);
            boneWorld.instructions.insert(afterTransform, skipThirdPersonRotation);

            legacy.owner = HELPER;
            legacy.name = HELPER_NAME;
            legacy.itf = false;
            changed++;
        }
        return changed;
    }

    private static boolean hasFirstPersonArmKeyFallback(
            MethodNode method, MethodInsnNode firstPoseCall, MethodInsnNode firstKeyCall) {
        AbstractInsnNode duplicate = nextMeaningful(firstPoseCall);
        AbstractInsnNode nonNullBranch = nextMeaningful(duplicate);
        AbstractInsnNode discardMissing = nextMeaningful(nonNullBranch);
        AbstractInsnNode playerIdLoad = nextMeaningful(discardMissing);
        AbstractInsnNode boneNameLoad = nextMeaningful(playerIdLoad);
        AbstractInsnNode keyHelper = nextMeaningful(boneNameLoad);
        AbstractInsnNode secondPoseCall = nextMeaningful(keyHelper);
        if (!(duplicate instanceof InsnNode dup && dup.getOpcode() == DUP)
                || !(nonNullBranch instanceof JumpInsnNode branch && branch.getOpcode() == IFNONNULL)
                || !(discardMissing instanceof InsnNode pop && pop.getOpcode() == POP)
                || !(playerIdLoad instanceof VarInsnNode idLoad
                && idLoad.getOpcode() == ILOAD && idLoad.var == 0)
                || !(boneNameLoad instanceof VarInsnNode boneLoad
                && boneLoad.getOpcode() == ALOAD && boneLoad.var == 1)
                || !(keyHelper instanceof MethodInsnNode helper && helper == firstKeyCall
                && helper.getOpcode() == INVOKESTATIC
                && helper.owner.equals(HELPER)
                && helper.name.equals(FIRST_PERSON_BONE_KEY_HELPER)
                && helper.desc.equals(FIRST_PERSON_BONE_KEY_DESC))
                || !(secondPoseCall instanceof MethodInsnNode second
                && second.getOpcode() == INVOKESTATIC
                && second.owner.equals(GECKO_MODEL_STORAGE)
                && second.name.equals(GET_NAMED_BONE_POSE_FP)
                && second.desc.equals(GET_NAMED_BONE_POSE_FP_DESC)
                && !second.itf)
                || !isAfter(branch.label, secondPoseCall)) {
            return false;
        }
        return method.instructions.indexOf(firstPoseCall) < method.instructions.indexOf(secondPoseCall);
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
