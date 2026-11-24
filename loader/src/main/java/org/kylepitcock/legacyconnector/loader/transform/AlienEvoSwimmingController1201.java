package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Migrates only SwimmingController.updateHitboxScaling's legacy
 * EntityDimensions.height field reads to the 1.21.1 record accessor.
 */
final class AlienEvoSwimmingController1201 implements Opcodes {
    static final String TARGET = "net/habb/alienevo/ability/handler/SwimmingController";
    static final String ENTITY_DIMENSIONS = "net/minecraft/world/entity/EntityDimensions";
    private static final String METHOD = "updateHitboxScaling";
    private static final String HEIGHT_DESC = "()F";

    private AlienEvoSwimmingController1201() {}

    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        MethodNode target = null;
        int matchingMethods = 0;
        for (MethodNode method : node.methods) {
            if (!METHOD.equals(method.name)) continue;
            target = method;
            matchingMethods++;
        }
        if (matchingMethods != 1 || target == null || target.instructions == null) {
            throw new IllegalStateException("AlienEvo SwimmingController expected exactly one updateHitboxScaling method; found "
                    + matchingMethods);
        }

        int legacyReads = 0;
        int accessorCalls = 0;
        for (var instruction : target.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == GETFIELD
                    && ENTITY_DIMENSIONS.equals(field.owner)
                    && "F".equals(field.desc)
                    && ("height".equals(field.name) || "f_20378_".equals(field.name))) {
                legacyReads++;
            } else if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKEVIRTUAL
                    && ENTITY_DIMENSIONS.equals(call.owner)
                    && "height".equals(call.name)
                    && HEIGHT_DESC.equals(call.desc)) {
                accessorCalls++;
            }
        }

        if (legacyReads == 0 && accessorCalls > 0) return 0;
        if (legacyReads == 0 || accessorCalls != 0) {
            throw new IllegalStateException("AlienEvo SwimmingController.updateHitboxScaling height access changed shape "
                    + "(legacyReads=" + legacyReads + ", accessorCalls=" + accessorCalls + ")");
        }

        for (var instruction : target.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == GETFIELD
                    && ENTITY_DIMENSIONS.equals(field.owner)
                    && "F".equals(field.desc)
                    && ("height".equals(field.name) || "f_20378_".equals(field.name))) {
                target.instructions.set(field,
                        new MethodInsnNode(INVOKEVIRTUAL, ENTITY_DIMENSIONS, "height", HEIGHT_DESC, false));
            }
        }
        return legacyReads;
    }
}
