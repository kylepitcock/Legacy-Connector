package org.kylepitcock.legacyconnector.loader.transform;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Compatibility guard for Palladium 4.5.10 command abilities.
 *
 * The generic Minecraft migration owns this API change. This narrow adapter is
 * a second line of defense for the two Palladium classes that contain the
 * command-function execution sites used by command abilities and energy beams.
 * It only rewrites the known legacy call when its integer result is discarded.
 */
final class PalladiumCommandFunction1201 implements Opcodes {
    static final String COMMAND_ABILITY =
            "net/threetag/palladium/power/ability/CommandAbility";
    static final String ENERGY_BEAM_ABILITY =
            "net/threetag/palladium/power/ability/EnergyBeamAbility";

    static final String SERVER_FUNCTION_MANAGER =
            "net/minecraft/server/ServerFunctionManager";
    static final String OLD_DESC =
            "(Lnet/minecraft/commands/functions/CommandFunction;"
                    + "Lnet/minecraft/commands/CommandSourceStack;)I";

    static final String HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/CommandFunctionCompat";
    static final String HELPER_DESC =
            "(Lnet/minecraft/server/ServerFunctionManager;"
                    + "Lnet/minecraft/commands/functions/CommandFunction;"
                    + "Lnet/minecraft/commands/CommandSourceStack;)V";

    private PalladiumCommandFunction1201() {}

    static int apply(ClassNode node) {
        Map<String, Integer> expected = expectedMethods(node.name);
        if (expected == null) return 0;

        Map<String, Integer> seen = new HashMap<>();
        int legacy = 0;
        int alreadyAdapted = 0;

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)) continue;

                if (SERVER_FUNCTION_MANAGER.equals(call.owner) && "execute".equals(call.name)) {
                    if (!OLD_DESC.equals(call.desc) || call.getOpcode() != INVOKEVIRTUAL) {
                        throw new IllegalStateException(
                                "Palladium command-function call has an unexpected descriptor/opcode in "
                                        + node.name + "#" + method.name + ": " + call.desc);
                    }
                    requireExpectedMethod(node.name, method.name, expected);
                    AbstractInsnNode discarded = nextExecutable(call.getNext());
                    if (discarded == null || discarded.getOpcode() != POP) {
                        throw new IllegalStateException(
                                "Palladium command-function result is unexpectedly used in "
                                        + node.name + "#" + method.name);
                    }

                    call.setOpcode(INVOKESTATIC);
                    call.owner = HELPER;
                    call.name = "execute";
                    call.desc = HELPER_DESC;
                    call.itf = false;
                    method.instructions.remove(discarded);
                    seen.merge(method.name, 1, Integer::sum);
                    legacy++;
                } else if (HELPER.equals(call.owner) && "execute".equals(call.name)) {
                    if (!HELPER_DESC.equals(call.desc) || call.getOpcode() != INVOKESTATIC) {
                        throw new IllegalStateException(
                                "Palladium command-function helper call has an unexpected shape in "
                                        + node.name + "#" + method.name);
                    }
                    requireExpectedMethod(node.name, method.name, expected);
                    seen.merge(method.name, 1, Integer::sum);
                    alreadyAdapted++;
                }
            }
        }

        int expectedTotal = expected.values().stream().mapToInt(Integer::intValue).sum();
        int seenTotal = seen.values().stream().mapToInt(Integer::intValue).sum();
        if (seenTotal != expectedTotal) {
            throw new IllegalStateException(
                    "Palladium command-function call count changed in " + node.name
                            + ": expected " + expectedTotal + ", found " + seenTotal
                            + " (legacy=" + legacy + ", already adapted=" + alreadyAdapted + ")");
        }
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            int found = seen.getOrDefault(entry.getKey(), 0);
            if (found != entry.getValue()) {
                throw new IllegalStateException(
                        "Palladium command-function calls in " + node.name + "#" + entry.getKey()
                                + ": expected " + entry.getValue() + ", found " + found);
            }
        }

        return legacy;
    }

    private static Map<String, Integer> expectedMethods(String className) {
        if (COMMAND_ABILITY.equals(className)) {
            return Map.of("firstTick", 1, "tick", 1, "lastTick", 1);
        }
        if (ENERGY_BEAM_ABILITY.equals(className)) {
            return Map.of("tick", 2);
        }
        return null;
    }

    private static void requireExpectedMethod(
            String className, String methodName, Map<String, Integer> expected) {
        if (!expected.containsKey(methodName)) {
            throw new IllegalStateException(
                    "Palladium command-function call is in an unexpected method: "
                            + className + "#" + methodName);
        }
    }

    private static AbstractInsnNode nextExecutable(AbstractInsnNode instruction) {
        AbstractInsnNode current = instruction;
        while (current != null && current.getOpcode() < 0) current = current.getNext();
        return current;
    }
}
