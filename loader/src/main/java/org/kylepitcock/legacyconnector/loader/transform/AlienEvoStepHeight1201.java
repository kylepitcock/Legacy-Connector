package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** Replaces AlienEvo's removed Entity.maxUpStep field accessor with STEP_HEIGHT attribute access. */
final class AlienEvoStepHeight1201 implements Opcodes {
    static final String LEGACY_ACCESSOR = "net/habb/alienevo/mixin/scale/EntityRawStepAccessor";
    static final String PLAYER = "net/minecraft/world/entity/player/Player";
    static final String HELPER = "org/kylepitcock/legacyconnector/v1_20_1/migrate/StepHeightCompat";
    static final String SCALE_MIXIN = "net/habb/alienevo/mixin/scale/EntityStepHeightScaleMixin";
    static final String ENTITY = "net/minecraft/world/entity/Entity";
    static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";

    private static final Set<String> USERS = Set.of(
            "net/habb/alienevo/aliens/arburian_pelarota/BallRollController",
            "net/habb/alienevo/aliens/galvanic_mechamorph/KartController",
            "net/habb/alienevo/aliens/petrosapien/CrystalSurfController");

    private AlienEvoStepHeight1201() {}

    static boolean isLegacyAccessor(String name) {
        return LEGACY_ACCESSOR.equals(name);
    }

    /**
     * In 1.21.1 the legacy maxUpStep() implementation lives on LivingEntity,
     * not Entity. Keep AlienEvo's ModifyReturnValue scaling logic but retarget
     * it to the class that still declares the method.
     */
    private static int retargetStepHeightScaleMixin(ClassNode node) {
        boolean foundMixin = false;
        boolean alreadyLivingEntity = false;
        int changes = 0;
        changes += retargetMixinAnnotation(node.visibleAnnotations);
        changes += retargetMixinAnnotation(node.invisibleAnnotations);
        if (changes > 0) return changes;
        for (java.util.List<AnnotationNode> annotations : java.util.List.of(
                node.visibleAnnotations == null ? java.util.Collections.emptyList() : node.visibleAnnotations,
                node.invisibleAnnotations == null ? java.util.Collections.emptyList() : node.invisibleAnnotations)) {
            for (AnnotationNode annotation : annotations) {
                if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")
                        || annotation.values == null) continue;
                foundMixin = true;
                for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                    if (!"value".equals(annotation.values.get(i))
                            || !(annotation.values.get(i + 1) instanceof java.util.List<?> values)) continue;
                    for (Object value : values) {
                        if (value instanceof Type type && type.getSort() == Type.OBJECT
                                && type.getInternalName().equals(LIVING_ENTITY)) alreadyLivingEntity = true;
                    }
                }
            }
        }
        if (foundMixin && alreadyLivingEntity) return 0;
        throw new IllegalStateException("AlienEvo EntityStepHeightScaleMixin no longer targets Entity through its inspected @Mixin annotation");
    }

    private static int retargetMixinAnnotation(java.util.List<AnnotationNode> annotations) {
        if (annotations == null) return 0;
        for (AnnotationNode annotation : annotations) {
            if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")
                    || annotation.values == null) continue;
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if (!"value".equals(annotation.values.get(i))
                        || !(annotation.values.get(i + 1) instanceof java.util.List<?> values)) continue;
                boolean changed = false;
                for (int j = 0; j < values.size(); j++) {
                    Object value = values.get(j);
                    if (value instanceof Type type && type.getSort() == Type.OBJECT
                            && type.getInternalName().equals(ENTITY)) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> mutable = (java.util.List<Object>) values;
                        mutable.set(j, Type.getObjectType(LIVING_ENTITY));
                        changed = true;
                    }
                }
                if (changed) return 1;
            }
        }
        return 0;
    }

    /** Rewrites all calls in the three verified surf controllers and retargets the step-scale mixin. */
    static int apply(ClassNode node) {
        if (SCALE_MIXIN.equals(node.name)) return retargetStepHeightScaleMixin(node);
        if (!USERS.contains(node.name)) return 0;
        int changes = 0;
        for (var method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof TypeInsnNode cast && cast.getOpcode() == CHECKCAST
                        && LEGACY_ACCESSOR.equals(cast.desc)) {
                    cast.desc = PLAYER;
                    changes++;
                } else if (instruction instanceof MethodInsnNode call
                        && call.getOpcode() == INVOKEINTERFACE
                        && LEGACY_ACCESSOR.equals(call.owner)) {
                    if (call.name.equals("alienevo$getRawMaxUpStep") && call.desc.equals("()F")) {
                        method.instructions.set(call, new MethodInsnNode(INVOKESTATIC, HELPER, "getRawMaxUpStep",
                                "(L" + PLAYER + ";)F", false));
                        changes++;
                    } else if (call.name.equals("alienevo$setRawMaxUpStep") && call.desc.equals("(F)V")) {
                        method.instructions.set(call, new MethodInsnNode(INVOKESTATIC, HELPER, "setRawMaxUpStep",
                                "(L" + PLAYER + ";F)V", false));
                        changes++;
                    }
                }
            }
        }
        return changes;
    }
}
