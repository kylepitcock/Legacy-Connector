package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Palladium 4.5.10 reads EntityDimensions.width as a field while writing
 * CustomProjectile spawn data. Minecraft 1.21.1 makes record components
 * private, so use the public width() accessor instead.
 */
final class PalladiumCustomProjectile1201 implements Opcodes {
    static final String CUSTOM_PROJECTILE = "net/threetag/palladium/entity/CustomProjectile";
    static final String ENTITY_DIMENSIONS = "net/minecraft/world/entity/EntityDimensions";
    private static final String WIDTH_ACCESSOR_DESC = "()F";

    private PalladiumCustomProjectile1201() {}

    static int apply(ClassNode node) {
        if (!CUSTOM_PROJECTILE.equals(node.name)) return 0;

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
                        && ("width".equals(field.name) || "f_20377_".equals(field.name))) {
                    method.instructions.set(field,
                            new MethodInsnNode(INVOKEVIRTUAL, ENTITY_DIMENSIONS, "width", WIDTH_ACCESSOR_DESC, false));
                    changed++;
                    matchingSites++;
                } else if (instruction instanceof MethodInsnNode call
                        && call.getOpcode() == INVOKEVIRTUAL
                        && ENTITY_DIMENSIONS.equals(call.owner)
                        && "width".equals(call.name)
                        && WIDTH_ACCESSOR_DESC.equals(call.desc)) {
                    matchingSites++;
                }
                instruction = next;
            }
        }
        if (matchingSites != 1) {
            throw new IllegalStateException(
                    "Palladium CustomProjectile EntityDimensions.width access count changed: expected 1, found "
                            + matchingSites);
        }
        return changed;
    }
}
