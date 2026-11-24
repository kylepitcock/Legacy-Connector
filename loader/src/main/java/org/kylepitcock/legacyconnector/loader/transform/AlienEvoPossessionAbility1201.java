package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Migrates AlienEvo dev.24's possession hitbox reads to the public
 * EntityDimensions record accessors introduced by Minecraft 1.21.1.
 */
final class AlienEvoPossessionAbility1201 implements Opcodes {
    static final String ABILITY = "net/habb/alienevo/ability/PossessionAbility";
    static final String ENTITY_DIMENSIONS = "net/minecraft/world/entity/EntityDimensions";
    private static final String FLOAT_DESC = "()F";
    private static final int EXPECTED_WIDTH_SITES = 1;
    private static final int EXPECTED_HEIGHT_SITES = 1;

    private AlienEvoPossessionAbility1201() {}

    static int apply(ClassNode node) {
        if (!ABILITY.equals(node.name)) return 0;

        int changed = 0;
        int widthSites = 0;
        int heightSites = 0;
        for (var method : node.methods) {
            if (method.instructions == null) continue;
            for (var instruction = method.instructions.getFirst(); instruction != null;) {
                var next = instruction.getNext();
                if (instruction instanceof FieldInsnNode field
                        && field.getOpcode() == GETFIELD
                        && ENTITY_DIMENSIONS.equals(field.owner)
                        && "F".equals(field.desc)) {
                    if ("width".equals(field.name) || "f_20377_".equals(field.name)) {
                        method.instructions.set(field,
                                new MethodInsnNode(INVOKEVIRTUAL, ENTITY_DIMENSIONS, "width", FLOAT_DESC, false));
                        widthSites++;
                        changed++;
                    } else if ("height".equals(field.name) || "f_20378_".equals(field.name)) {
                        method.instructions.set(field,
                                new MethodInsnNode(INVOKEVIRTUAL, ENTITY_DIMENSIONS, "height", FLOAT_DESC, false));
                        heightSites++;
                        changed++;
                    }
                } else if (instruction instanceof MethodInsnNode call
                        && call.getOpcode() == INVOKEVIRTUAL
                        && ENTITY_DIMENSIONS.equals(call.owner)
                        && FLOAT_DESC.equals(call.desc)
                        && "width".equals(call.name)) {
                    widthSites++;
                } else if (instruction instanceof MethodInsnNode call
                        && call.getOpcode() == INVOKEVIRTUAL
                        && ENTITY_DIMENSIONS.equals(call.owner)
                        && FLOAT_DESC.equals(call.desc)
                        && "height".equals(call.name)) {
                    heightSites++;
                }
                instruction = next;
            }
        }

        if (widthSites != EXPECTED_WIDTH_SITES || heightSites != EXPECTED_HEIGHT_SITES) {
            throw new IllegalStateException(
                    "AlienEvo PossessionAbility EntityDimensions accessor counts changed: expected width="
                            + EXPECTED_WIDTH_SITES + ", height=" + EXPECTED_HEIGHT_SITES
                            + "; found width=" + widthSites + ", height=" + heightSites);
        }
        return changed;
    }
}
