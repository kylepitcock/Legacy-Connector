package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Migrates AlienEvo 1.5.0-dev.24's colored projectile renderer away from the
 * private EntityDimensions record component in Minecraft 1.21.1.
 */
final class AlienEvoColoredProjectileRenderer1201 implements Opcodes {
    static final String RENDERER = "net/habb/alienevo/client/renderer/ColoredProjectileRenderer";
    static final String ENTITY_DIMENSIONS = "net/minecraft/world/entity/EntityDimensions";
    private static final String HEIGHT_ACCESSOR_DESC = "()F";
    private static final int EXPECTED_SITES = 1;

    private AlienEvoColoredProjectileRenderer1201() {}

    static int apply(org.objectweb.asm.tree.ClassNode node) {
        if (!RENDERER.equals(node.name)) return 0;

        int changed = 0;
        int matchingSites = 0;
        for (var method : node.methods) {
            if (method.instructions == null) continue;
            for (var instruction = method.instructions.getFirst(); instruction != null;) {
                var next = instruction.getNext();
                if (instruction instanceof FieldInsnNode field
                        && field.getOpcode() == GETFIELD
                        && ENTITY_DIMENSIONS.equals(field.owner)
                        && "F".equals(field.desc)
                        && ("height".equals(field.name) || "f_20378_".equals(field.name))) {
                    method.instructions.set(field,
                            new MethodInsnNode(INVOKEVIRTUAL, ENTITY_DIMENSIONS, "height",
                                    HEIGHT_ACCESSOR_DESC, false));
                    changed++;
                    matchingSites++;
                } else if (instruction instanceof MethodInsnNode call
                        && call.getOpcode() == INVOKEVIRTUAL
                        && ENTITY_DIMENSIONS.equals(call.owner)
                        && "height".equals(call.name)
                        && HEIGHT_ACCESSOR_DESC.equals(call.desc)) {
                    matchingSites++;
                }
                instruction = next;
            }
        }
        if (matchingSites != EXPECTED_SITES) {
            throw new IllegalStateException(
                    "AlienEvo ColoredProjectileRenderer EntityDimensions.height access count changed: expected "
                            + EXPECTED_SITES + ", found " + matchingSites);
        }
        return changed;
    }
}
