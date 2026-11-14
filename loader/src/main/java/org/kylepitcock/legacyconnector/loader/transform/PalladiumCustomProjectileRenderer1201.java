package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Palladium 4.5.10's projectile renderer reads EntityDimensions.height twice.
 * In Minecraft 1.21.1 EntityDimensions is a record with private components,
 * so both reads must use the public height() accessor.
 */
final class PalladiumCustomProjectileRenderer1201 implements Opcodes {
    static final String RENDERER = "net/threetag/palladium/client/renderer/entity/CustomProjectileRenderer";
    static final String ENTITY_DIMENSIONS = "net/minecraft/world/entity/EntityDimensions";
    private static final String ACCESSOR_DESC = "()F";
    private static final int EXPECTED_SITES = 2;

    private PalladiumCustomProjectileRenderer1201() {}

    static int apply(ClassNode node) {
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
                            new MethodInsnNode(INVOKEVIRTUAL, ENTITY_DIMENSIONS, "height", ACCESSOR_DESC, false));
                    changed++;
                    matchingSites++;
                } else if (instruction instanceof MethodInsnNode call
                        && call.getOpcode() == INVOKEVIRTUAL
                        && ENTITY_DIMENSIONS.equals(call.owner)
                        && "height".equals(call.name)
                        && ACCESSOR_DESC.equals(call.desc)) {
                    matchingSites++;
                }
                instruction = next;
            }
        }
        if (matchingSites != EXPECTED_SITES) {
            throw new IllegalStateException(
                    "Palladium CustomProjectileRenderer EntityDimensions.height access count changed: expected "
                            + EXPECTED_SITES + ", found " + matchingSites);
        }
        return changed;
    }
}
