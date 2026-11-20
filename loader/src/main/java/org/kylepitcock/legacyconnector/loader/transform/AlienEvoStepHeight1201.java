package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** Replaces AlienEvo's removed Entity.maxUpStep field accessor with STEP_HEIGHT attribute access. */
final class AlienEvoStepHeight1201 implements Opcodes {
    static final String LEGACY_ACCESSOR = "net/habb/alienevo/mixin/scale/EntityRawStepAccessor";
    static final String PLAYER = "net/minecraft/world/entity/player/Player";
    static final String HELPER = "org/kylepitcock/legacyconnector/v1_20_1/migrate/StepHeightCompat";

    private static final Set<String> USERS = Set.of(
            "net/habb/alienevo/aliens/arburian_pelarota/BallRollController",
            "net/habb/alienevo/aliens/galvanic_mechamorph/KartController",
            "net/habb/alienevo/aliens/petrosapien/CrystalSurfController");

    private AlienEvoStepHeight1201() {}

    static boolean isLegacyAccessor(String name) {
        return LEGACY_ACCESSOR.equals(name);
    }

    /** Rewrites all calls in the three verified surf controllers. */
    static int apply(ClassNode node) {
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
