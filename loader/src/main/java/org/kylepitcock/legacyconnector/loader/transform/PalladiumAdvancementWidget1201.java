package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Palladium 4.5.10's TreeAbilityWidget uses AdvancementWidgetType#getIndex()
 * to select one of the obtained/unobtained tooltip sprite rows. Minecraft
 * 1.21.1 keeps those two enum constants in the same order but removes the
 * integer accessor. Enum.ordinal() preserves the old 0/1 row index.
 */
final class PalladiumAdvancementWidget1201 implements Opcodes {
    static final String TREE_ABILITY_WIDGET =
            "net/threetag/palladium/client/screen/power/TreeAbilityWidget";

    private static final String ADVANCEMENT_WIDGET_TYPE =
            "net/minecraft/client/gui/screens/advancements/AdvancementWidgetType";
    private static final String DRAW_HOVER = "drawHover";
    private static final String DRAW_HOVER_DESC =
            "(Lnet/minecraft/client/gui/GuiGraphics;IIFII)V";
    private static final int EXPECTED_CALLS = 3;

    private PalladiumAdvancementWidget1201() {
    }

    /**
     * @return number of removed getIndex() call sites
     */
    static int apply(ClassNode node) {
        if (!TREE_ABILITY_WIDGET.equals(node.name)) {
            return 0;
        }

        MethodNode drawHover = null;
        for (MethodNode method : node.methods) {
            if (DRAW_HOVER.equals(method.name) && DRAW_HOVER_DESC.equals(method.desc)) {
                if (drawHover != null) {
                    throw new IllegalStateException("Palladium TreeAbilityWidget has duplicate drawHover methods");
                }
                drawHover = method;
            }
        }
        if (drawHover == null) {
            throw new IllegalStateException("Palladium TreeAbilityWidget is missing drawHover(GuiGraphics, ...)");
        }

        int legacyCalls = 0;
        int migratedCalls = 0;
        for (var instruction : drawHover.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) {
                continue;
            }
            if (ADVANCEMENT_WIDGET_TYPE.equals(call.owner)
                    && "getIndex".equals(call.name)
                    && "()I".equals(call.desc)) {
                if (call.getOpcode() != INVOKEVIRTUAL || call.itf) {
                    throw new IllegalStateException(
                            "Palladium AdvancementWidgetType#getIndex call has an unexpected invocation kind");
                }
                legacyCalls++;
            } else if ("java/lang/Enum".equals(call.owner)
                    && "ordinal".equals(call.name)
                    && "()I".equals(call.desc)) {
                migratedCalls++;
            }
        }

        if (legacyCalls == 0 && migratedCalls == EXPECTED_CALLS) {
            return 0; // Already adapted.
        }
        if (legacyCalls != EXPECTED_CALLS || migratedCalls != 0) {
            throw new IllegalStateException(
                    "Palladium TreeAbilityWidget tooltip index calls changed: expected "
                            + EXPECTED_CALLS + " legacy getIndex() call(s), found "
                            + legacyCalls + " legacy and " + migratedCalls + " migrated");
        }

        for (var instruction : drawHover.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && ADVANCEMENT_WIDGET_TYPE.equals(call.owner)
                    && "getIndex".equals(call.name)
                    && "()I".equals(call.desc)) {
                call.owner = "java/lang/Enum";
                call.name = "ordinal";
                call.desc = "()I";
                call.itf = false;
            }
        }
        return EXPECTED_CALLS;
    }
}
