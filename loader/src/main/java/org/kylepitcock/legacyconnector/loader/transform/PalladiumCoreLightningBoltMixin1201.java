package org.kylepitcock.legacyconnector.loader.transform;

import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/** Declares the already-intended one-instruction shift in PalladiumCore's LightningBolt strike hook. */
final class PalladiumCoreLightningBoltMixin1201 implements Opcodes {
    static final String MIXIN = "net/threetag/palladiumcore/mixin/LightningBoltMixin";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String AT = "Lorg/spongepowered/asm/mixin/injection/At;";
    private static final String TARGET =
            "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;";
    private static final String SHIFT_ENUM = "Lorg/spongepowered/asm/mixin/injection/At$Shift;";

    private PalladiumCoreLightningBoltMixin1201() {}

    static int apply(ClassNode node) {
        if (!MIXIN.equals(node.name)) return 0;

        int matchingSites = 0;
        int changed = 0;
        for (MethodNode method : node.methods) {
            for (AnnotationNode inject : annotations(method)) {
                if (!INJECT.equals(inject.desc)) continue;
                Object methodNames = value(inject, "method");
                if (!(methodNames instanceof List<?> names) || !names.contains("tick")) continue;
                Object atValues = value(inject, "at");
                if (!(atValues instanceof List<?> ats)) continue;
                for (Object candidate : ats) {
                    if (!(candidate instanceof AnnotationNode at) || !matchesLightningStrikeAt(at)) continue;
                    matchingSites++;

                    // Mixin's maxShiftBy belongs to @Inject, not its nested @At.
                    // Migrate the misplaced value emitted by older Connector builds,
                    // then set the contract on the enclosing injection annotation.
                    Object misplacedMaximum = value(at, "maxShiftBy");
                    if (misplacedMaximum != null) {
                        if (!(misplacedMaximum instanceof Integer max) || max != 1) {
                            throw new IllegalStateException(
                                    "PalladiumCore LightningBoltMixin has an unexpected nested maxShiftBy value: "
                                            + misplacedMaximum);
                        }
                        removeValue(at, "maxShiftBy");
                        changed++;
                    }

                    Object maximum = value(inject, "maxShiftBy");
                    if (maximum == null) {
                        putValue(inject, "maxShiftBy", 1);
                        changed++;
                    } else if (!(maximum instanceof Integer max) || max != 1) {
                        throw new IllegalStateException(
                                "PalladiumCore LightningBoltMixin has an unexpected @Inject maxShiftBy value: "
                                        + maximum);
                    }
                }
            }
        }
        if (matchingSites != 1) {
            throw new IllegalStateException("PalladiumCore LightningBoltMixin expected one tick injection after the second "
                    + "Level.getEntities assignment, found " + matchingSites);
        }
        return changed;
    }

    private static List<AnnotationNode> annotations(MethodNode method) {
        java.util.ArrayList<AnnotationNode> all = new java.util.ArrayList<>();
        if (method.visibleAnnotations != null) all.addAll(method.visibleAnnotations);
        if (method.invisibleAnnotations != null) all.addAll(method.invisibleAnnotations);
        return all;
    }

    private static Object value(AnnotationNode annotation, String name) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (name.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }

    private static void putValue(AnnotationNode annotation, String name, Object value) {
        if (annotation.values == null) annotation.values = new java.util.ArrayList<>();
        annotation.values.add(name);
        annotation.values.add(value);
    }

    private static void removeValue(AnnotationNode annotation, String name) {
        if (annotation.values == null) return;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (name.equals(annotation.values.get(i))) {
                annotation.values.remove(i + 1);
                annotation.values.remove(i);
                return;
            }
        }
    }

    private static boolean matchesLightningStrikeAt(AnnotationNode at) {
        Object shift = value(at, "shift");
        return AT.equals(at.desc)
                && "INVOKE_ASSIGN".equals(value(at, "value"))
                && TARGET.equals(value(at, "target"))
                && Integer.valueOf(1).equals(value(at, "ordinal"))
                && shift instanceof String[] direction
                && direction.length == 2
                && SHIFT_ENUM.equals(direction[0])
                && "BY".equals(direction[1])
                && Integer.valueOf(1).equals(value(at, "by"));
    }
}
