package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Opt-in runtime probes for the two unresolved AlienEvo runtime defects.
 *
 * These probes observe the existing result; they do not change animation,
 * swing, camera, or beam behavior. Enable them with
 * -Dlegacyconnector.debug.alienevoRuntime=true.
 */
final class AlienEvoRuntimeDiagnostics1201 implements Opcodes {
    private static final String HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoRuntimeDiagnostics1201Runtime";
    private static final String KINECELERAN_RUN =
            "net/habb/alienevo/client/model/animation/aliens/kineceleran/KineceleranRunAnimation";
    private static final String FIRST_PERSON_ARM =
            "net/habb/alienevo/client/model/animation/FirstPersonArmAnimator";
    private static final String CANCEL_FIRST_PERSON_SWING =
            "net/habb/alienevo/mixin/client/CancelFirstPersonSwingMixin";
    private static final String BONE_ANCHORS = "net/habb/alienevo/client/BoneAnchors";
    private static final String CHEST_SPIN_ANIMATION =
            "net/habb/alienevo/client/model/animation/aliens/arburian_pelarota/ChestSpinAnimation";
    private static final String ABILITY_UTIL = "net/threetag/palladium/power/ability/AbilityUtil";
    private static final String ABILITY_ENABLED_DESC =
            "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/resources/ResourceLocation;Ljava/lang/String;)Z";

    private static final String ANIMATE_DESC =
            "(Lnet/threetag/palladium/client/model/animation/PalladiumAnimation$Builder;"
                    + "Lnet/minecraft/client/player/AbstractClientPlayer;"
                    + "Lnet/minecraft/client/model/HumanoidModel;"
                    + "Lnet/threetag/palladium/client/model/animation/PalladiumAnimation$FirstPersonContext;F)V";
    private static final String APPLY_ARM_DESC =
            "(Lnet/minecraft/client/model/geom/ModelPart;Z)V";
    private static final String BONE_WORLD_DESC =
            "(ILjava/lang/String;ZFLorg/joml/Matrix3f;Lnet/minecraft/world/phys/Vec3;)"
                    + "Lnet/minecraft/world/phys/Vec3;";

    private AlienEvoRuntimeDiagnostics1201() {}

    static int apply(ClassNode node) {
        return switch (node.name) {
            case KINECELERAN_RUN -> traceKineceleranRun(node);
            case FIRST_PERSON_ARM -> traceFirstPersonArm(node);
            case CANCEL_FIRST_PERSON_SWING -> traceVanillaSwing(node);
            case BONE_ANCHORS -> traceBoneWorld(node);
            case CHEST_SPIN_ANIMATION -> traceCannonboltSpinGate(node);
            default -> 0;
        };
    }

    private static int traceCannonboltSpinGate(ClassNode node) {
        MethodNode animate = find(node, "animate", ANIMATE_DESC);
        if (animate == null || animate.instructions == null) {
            throw new IllegalStateException("AlienEvo ChestSpinAnimation.animate has an unexpected method shape");
        }

        int existingTraces = countCalls(animate, "traceCannonboltSpinGate");
        if (existingTraces == 1) return 0;
        if (existingTraces != 0) {
            throw new IllegalStateException("AlienEvo ChestSpinAnimation has a partial Cannonbolt roll-gate trace");
        }

        MethodInsnNode abilityCheck = null;
        VarInsnNode enabledStore = null;
        int matches = 0;
        for (AbstractInsnNode instruction : animate.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode call)
                    || call.getOpcode() != INVOKESTATIC
                    || !ABILITY_UTIL.equals(call.owner)
                    || !"isEnabled".equals(call.name)
                    || !ABILITY_ENABLED_DESC.equals(call.desc)) continue;
            AbstractInsnNode next = nextMeaningful(call);
            if (!(next instanceof VarInsnNode store)
                    || store.getOpcode() != ISTORE || store.var != 7) {
                throw new IllegalStateException(
                        "AlienEvo ChestSpinAnimation ball_roll gate no longer stores its result in local 7");
            }
            abilityCheck = call;
            enabledStore = store;
            matches++;
        }

        if (matches != 1 || abilityCheck == null || enabledStore == null) {
            throw new IllegalStateException(
                    "AlienEvo ChestSpinAnimation expected exactly one Palladium ball_roll ability gate; found "
                            + matches);
        }

        InsnList trace = new InsnList();
        trace.add(new VarInsnNode(ALOAD, 2)); // player
        trace.add(new VarInsnNode(ILOAD, 7)); // AbilityUtil.isEnabled result
        trace.add(new VarInsnNode(ALOAD, 6)); // ChestSpinAnimation.State
        trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceCannonboltSpinGate",
                "(Ljava/lang/Object;ZLjava/lang/Object;)V", false));
        animate.instructions.insert(enabledStore, trace);
        animate.maxStack = Math.max(animate.maxStack, 3);
        return 1;
    }

    private static AbstractInsnNode nextMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode value = instruction == null ? null : instruction.getNext();
        while (value != null && value.getOpcode() < 0) value = value.getNext();
        return value;
    }

    private static int traceKineceleranRun(ClassNode node) {
        MethodNode method = find(node, "animate", ANIMATE_DESC);
        if (method == null || method.instructions == null) {
            throw new IllegalStateException("AlienEvo KineceleranRunAnimation.animate has an unexpected method shape");
        }

        int returnCount = countOpcode(method, RETURN);
        int existingTraces = countCalls(method, "traceKineceleranRun");
        if (returnCount == 0) throw new IllegalStateException("AlienEvo KineceleranRunAnimation.animate has no return sites to trace");
        if (existingTraces == returnCount) return 0;
        if (existingTraces != 0) throw new IllegalStateException("AlienEvo KineceleranRunAnimation has a partial runtime trace");
        int changed = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != RETURN) continue;
            InsnList trace = new InsnList();
            trace.add(new VarInsnNode(ALOAD, 1)); // Palladium animation builder
            trace.add(new VarInsnNode(ALOAD, 2)); // player
            trace.add(new VarInsnNode(ALOAD, 4)); // first-person context
            trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceKineceleranRun",
                    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false));
            method.instructions.insertBefore(instruction, trace);
            changed++;
        }
        method.maxStack = Math.max(method.maxStack, 3);
        return changed;
    }

    private static int traceFirstPersonArm(ClassNode node) {
        MethodNode method = find(node, "apply", APPLY_ARM_DESC);
        if (method == null || method.instructions == null) {
            throw new IllegalStateException("AlienEvo FirstPersonArmAnimator.apply has an unexpected method shape");
        }

        int returns = countOpcode(method, RETURN);
        int existingTraces = countCalls(method, "traceFirstPersonArmApply");
        if (returns == 0) throw new IllegalStateException("AlienEvo FirstPersonArmAnimator.apply has no return site to trace");
        if (existingTraces == returns) return 0;
        if (existingTraces != 0) throw new IllegalStateException("AlienEvo FirstPersonArmAnimator has a partial runtime trace");
        int changed = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != RETURN) continue;

            InsnList trace = new InsnList();
            trace.add(new VarInsnNode(ALOAD, 2)); // resolved PlayerModelPart
            trace.add(new VarInsnNode(ALOAD, 3)); // pending/tuned AnimData (may be null)
            trace.add(new VarInsnNode(ALOAD, 0)); // rendered ModelPart method argument
            trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceFirstPersonArmApply",
                    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false));
            method.instructions.insertBefore(instruction, trace);
            changed++;
        }
        if (returns == 0) {
            throw new IllegalStateException("AlienEvo FirstPersonArmAnimator.apply has no return site to trace");
        }
        method.maxStack = Math.max(method.maxStack, 3);
        return changed;
    }

    private static int traceVanillaSwing(ClassNode node) {
        MethodNode method = find(node, "alienevo$cancelSwingProgress", "(F)F");
        if (method == null || method.instructions == null) {
            throw new IllegalStateException("AlienEvo CancelFirstPersonSwingMixin handler has an unexpected method shape");
        }

        int returns = countOpcode(method, FRETURN);
        int existingTraces = countCalls(method, "traceVanillaSwing");
        if (returns == 0) throw new IllegalStateException("AlienEvo CancelFirstPersonSwingMixin has no FRETURN site to trace");
        if (existingTraces == returns) return 0;
        if (existingTraces != 0) throw new IllegalStateException("AlienEvo CancelFirstPersonSwingMixin has a partial runtime trace");
        int changed = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != FRETURN) continue;

            InsnList trace = new InsnList();
            trace.add(new InsnNode(DUP)); // retain the result for FRETURN
            trace.add(new VarInsnNode(FLOAD, 1)); // original vanilla swing progress
            trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceVanillaSwing", "(FF)V", false));
            method.instructions.insertBefore(instruction, trace);
            changed++;
        }
        if (returns == 0) {
            throw new IllegalStateException("AlienEvo CancelFirstPersonSwingMixin has no FRETURN site to trace");
        }
        // DUP retains the FRETURN value, and FLOAD adds the original progress:
        // the diagnostic call reaches three stack values before consuming two.
        method.maxStack = Math.max(method.maxStack, 3);
        return changed;
    }

    private static int traceBoneWorld(ClassNode node) {
        MethodNode method = find(node, "boneWorld", BONE_WORLD_DESC);
        if (method == null || method.instructions == null || (method.access & ACC_STATIC) == 0) {
            throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld has an unexpected method shape");
        }

        AbstractInsnNode[] returnSites = java.util.Arrays.stream(method.instructions.toArray())
                .filter(instruction -> instruction.getOpcode() == ARETURN)
                .toArray(AbstractInsnNode[]::new);
        int returns = returnSites.length;
        int originTraces = countCalls(method, "traceBeamAnchor");
        int missingTraces = countCalls(method, "traceMissingBeamAnchor");
        if (returns < 2) {
            throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld expected early missing-pose return(s) and a final origin return");
        }
        if (originTraces == 1 && missingTraces == returns) return 0;
        if (originTraces != 0 || missingTraces != 0) {
            throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld has a partial runtime trace");
        }

        int changed = 0;
        for (int index = 0; index < returnSites.length; index++) {
            AbstractInsnNode instruction = returnSites[index];

            if (index < returnSites.length - 1) {
                // AlienEvo returns null before allocating local 7 when no named-bone
                // pose exists. Only inspect the method parameters at these early exits;
                // touching locals 6/7 here makes the JVM reject the transformed class.
                AbstractInsnNode returnedValue = previousMeaningful(instruction);
                if (returnedValue == null || returnedValue.getOpcode() != ACONST_NULL) {
                    throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld early return is no longer a null missing-pose exit");
                }

                InsnList trace = new InsnList();
                trace.add(new VarInsnNode(ILOAD, 0));
                trace.add(new VarInsnNode(ALOAD, 1));
                trace.add(new VarInsnNode(ILOAD, 2));
                trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceMissingBeamAnchor",
                        "(ILjava/lang/String;Z)V", false));
                method.instructions.insertBefore(instruction, trace);
                changed++;
                continue;
            }

            // The final ARETURN is reached only after the named-bone pose and
            // transformed local translation have been initialized in locals 6/7.
            LabelNode missing = new LabelNode();
            LabelNode done = new LabelNode();
            InsnList trace = new InsnList();
            trace.add(new InsnNode(DUP));
            trace.add(new JumpInsnNode(IFNULL, missing));
            trace.add(new VarInsnNode(ASTORE, 8)); // preserve non-null world origin
            trace.add(new VarInsnNode(ILOAD, 0));
            trace.add(new VarInsnNode(ALOAD, 1));
            trace.add(new VarInsnNode(ILOAD, 2));
            trace.add(new VarInsnNode(FLOAD, 3));
            trace.add(new VarInsnNode(ALOAD, 4)); // inverse-view matrix passed by AlienEvo
            trace.add(new VarInsnNode(ALOAD, 5)); // camera position passed by AlienEvo
            trace.add(new VarInsnNode(ALOAD, 6)); // captured named-bone pose
            trace.add(new VarInsnNode(ALOAD, 7)); // scaled/rotated local translation
            trace.add(new VarInsnNode(ALOAD, 8)); // final world origin
            trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceBeamAnchor",
                    "(ILjava/lang/String;ZFLorg/joml/Matrix3f;Lnet/minecraft/world/phys/Vec3;"
                            + "Lorg/joml/Matrix4f;Lorg/joml/Vector3f;Lnet/minecraft/world/phys/Vec3;)V",
                    false));
            trace.add(new VarInsnNode(ALOAD, 8));
            trace.add(new JumpInsnNode(GOTO, done));
            trace.add(missing);
            trace.add(new InsnNode(POP));
            trace.add(new VarInsnNode(ILOAD, 0));
            trace.add(new VarInsnNode(ALOAD, 1));
            trace.add(new VarInsnNode(ILOAD, 2));
            trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceMissingBeamAnchor",
                    "(ILjava/lang/String;Z)V", false));
            trace.add(new InsnNode(ACONST_NULL));
            trace.add(done);
            method.instructions.insertBefore(instruction, trace);
            changed++;
        }
        method.maxLocals = Math.max(method.maxLocals, 9);
        method.maxStack = Math.max(method.maxStack, 9);
        return changed;
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode value = instruction == null ? null : instruction.getPrevious();
        while (value != null && value.getOpcode() < 0) value = value.getPrevious();
        return value;
    }

    private static int countOpcode(MethodNode method, int opcode) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() == opcode) count++;
        }
        return count;
    }

    private static int countCalls(MethodNode method, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && HELPER.equals(call.owner) && name.equals(call.name)) count++;
        }
        return count;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (!name.equals(method.name) || !descriptor.equals(method.desc)) continue;
            if (found != null) throw new IllegalStateException("Duplicate AlienEvo diagnostic target "
                    + node.name + "." + name + descriptor);
            found = method;
        }
        return found;
    }
}
