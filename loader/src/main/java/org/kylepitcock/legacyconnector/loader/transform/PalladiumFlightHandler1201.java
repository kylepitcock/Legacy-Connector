package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Palladium 4.5.10 reads EntityDimensions.height as a field. In Minecraft
 * 1.21.1 EntityDimensions is a record and its components are private; the
 * public height() record accessor must be used.
 */
final class PalladiumFlightHandler1201 implements Opcodes {
    static final String FLIGHT_HANDLER = "net/threetag/palladium/entity/FlightHandler";
    static final String ENTITY_DIMENSIONS = "net/minecraft/world/entity/EntityDimensions";

    private PalladiumFlightHandler1201() {
    }

    static int apply(ClassNode node) {
        if (!FLIGHT_HANDLER.equals(node.name)) {
            return 0;
        }

        int changed = 0;
        for (var method : node.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (var instruction = method.instructions.getFirst(); instruction != null;) {
                var next = instruction.getNext();
                if (instruction instanceof FieldInsnNode field
                        && field.getOpcode() == GETFIELD
                        && ENTITY_DIMENSIONS.equals(field.owner)
                        && "F".equals(field.desc)
                        && ("height".equals(field.name) || "f_20378_".equals(field.name))) {
                    method.instructions.set(field,
                            new MethodInsnNode(INVOKEVIRTUAL, ENTITY_DIMENSIONS, "height", "()F", false));
                    changed++;
                }
                instruction = next;
            }
        }
        return changed;
    }
}
