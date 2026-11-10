package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Removes Palladium 4.5.10's empty RecipeList.updateNarration override.
 * Minecraft 1.21.1 makes AbstractWidget.updateNarration final, and this old
 * override performs no work; inheriting the final implementation is safe.
 */
final class PalladiumTailoringScreen1201 implements Opcodes {
    static final String RECIPE_LIST =
            "net/threetag/palladium/client/screen/TailoringScreen$RecipeList";
    private static final String ABSTRACT_SELECTION_LIST =
            "net/minecraft/client/gui/components/AbstractSelectionList";
    private static final String UPDATE_NARRATION = "updateNarration";
    private static final String UPDATE_NARRATION_DESC =
            "(Lnet/minecraft/client/gui/narration/NarrationElementOutput;)V";

    private PalladiumTailoringScreen1201() {
    }

    /** Returns one when the exact legacy no-op override is removed. */
    static int apply(ClassNode node) {
        if (!RECIPE_LIST.equals(node.name)) return 0;
        if (!ABSTRACT_SELECTION_LIST.equals(node.superName)) {
            throw new IllegalStateException(
                    "Palladium TailoringScreen$RecipeList no longer extends the expected AbstractSelectionList");
        }

        MethodNode target = null;
        for (MethodNode method : node.methods) {
            if (!UPDATE_NARRATION.equals(method.name) || !UPDATE_NARRATION_DESC.equals(method.desc)) continue;
            if (target != null) throw new IllegalStateException("Palladium RecipeList has duplicate updateNarration overrides");
            target = method;
        }
        if (target == null) return 0;
        if ((target.access & (ACC_STATIC | ACC_ABSTRACT | ACC_NATIVE)) != 0
                || (target.access & (ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE)) != ACC_PUBLIC) {
            throw new IllegalStateException("Palladium RecipeList updateNarration method access flags changed");
        }

        int executableInstructions = 0;
        for (AbstractInsnNode instruction : target.instructions) {
            if (instruction.getOpcode() < 0) continue;
            executableInstructions++;
            if (instruction.getOpcode() != RETURN) {
                throw new IllegalStateException(
                        "Palladium RecipeList updateNarration is no longer the verified empty no-op");
            }
        }
        if (executableInstructions != 1) {
            throw new IllegalStateException(
                    "Palladium RecipeList updateNarration no longer contains exactly one RETURN");
        }

        node.methods.remove(target);
        return 1;
    }
}
