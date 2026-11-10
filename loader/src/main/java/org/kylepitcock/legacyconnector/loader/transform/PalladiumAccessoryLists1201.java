package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Palladium 4.5.10's two AccessoryScreen list widgets override
 * AbstractSelectionList.updateNarration with empty bodies. Minecraft 1.21.1
 * makes AbstractWidget.updateNarration final, so those obsolete no-op methods
 * make the nested classes fail JVM verification. Removing the no-op methods
 * lets both lists inherit the final implementation.
 */
final class PalladiumAccessoryLists1201 implements Opcodes {
    static final String ACCESSORY_SLOT_LIST =
            "net/threetag/palladium/client/screen/AccessoryScreen$AccessorySlotList";
    static final String ACCESSORY_LIST =
            "net/threetag/palladium/client/screen/AccessoryScreen$AccessoryList";

    private static final Set<String> TARGETS = Set.of(ACCESSORY_SLOT_LIST, ACCESSORY_LIST);
    private static final String ABSTRACT_SELECTION_LIST =
            "net/minecraft/client/gui/components/AbstractSelectionList";
    private static final String UPDATE_NARRATION = "updateNarration";
    private static final String UPDATE_NARRATION_DESC =
            "(Lnet/minecraft/client/gui/narration/NarrationElementOutput;)V";

    private PalladiumAccessoryLists1201() {
    }

    /** Returns one when the exact legacy no-op narration override is removed. */
    static int apply(ClassNode node) {
        if (!TARGETS.contains(node.name)) return 0;
        if (!ABSTRACT_SELECTION_LIST.equals(node.superName)) {
            throw new IllegalStateException(
                    "Palladium AccessoryScreen list no longer extends the expected AbstractSelectionList: " + node.name);
        }

        MethodNode target = null;
        for (MethodNode method : node.methods) {
            if (!UPDATE_NARRATION.equals(method.name) || !UPDATE_NARRATION_DESC.equals(method.desc)) continue;
            if (target != null) {
                throw new IllegalStateException("Palladium AccessoryScreen list has duplicate updateNarration overrides: " + node.name);
            }
            target = method;
        }
        if (target == null) return 0;
        if ((target.access & (ACC_STATIC | ACC_ABSTRACT | ACC_NATIVE)) != 0
                || (target.access & (ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE)) != ACC_PUBLIC) {
            throw new IllegalStateException(
                    "Palladium AccessoryScreen list updateNarration access flags changed: " + node.name);
        }

        int executableInstructions = 0;
        for (AbstractInsnNode instruction : target.instructions) {
            if (instruction.getOpcode() < 0) continue;
            executableInstructions++;
            if (instruction.getOpcode() != RETURN) {
                throw new IllegalStateException(
                        "Palladium AccessoryScreen list updateNarration is no longer the verified empty no-op: " + node.name);
            }
        }
        if (executableInstructions != 1) {
            throw new IllegalStateException(
                    "Palladium AccessoryScreen list updateNarration no longer contains exactly one RETURN: " + node.name);
        }

        node.methods.remove(target);
        return 1;
    }
}
