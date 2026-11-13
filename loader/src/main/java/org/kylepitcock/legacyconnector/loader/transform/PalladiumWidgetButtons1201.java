package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Palladium 4.5.10's IconButton and TextWithIconButton use Button.WIDGETS_LOCATION
 * and the 1.20.1 widgets.png row offsets for their default backgrounds. The field
 * was removed in 1.21.1 when button backgrounds moved to GUI-atlas sprites.
 *
 * Mc1201Migration has already redirected the old instance blit to
 * MiddleClientCompat.blitNineSliced when this pass runs. Replace only the
 * affected inherited-field operand with a null sentinel and retarget its paired
 * blit to the sprite-aware helper, which deliberately ignores that sentinel.
 * Other Palladium texture fields (notably FlatIconButton.WIDGETS_LOCATION) are
 * left intact.
 */
final class PalladiumWidgetButtons1201 implements Opcodes {
    static final String ICON_BUTTON =
            "net/threetag/palladium/client/screen/components/IconButton";
    static final String TEXT_WITH_ICON_BUTTON =
            "net/threetag/palladium/client/screen/components/TextWithIconButton";

    private static final String MINECRAFT_BUTTON =
            "net/minecraft/client/gui/components/Button";
    private static final String RESOURCE_LOCATION =
            "Lnet/minecraft/resources/ResourceLocation;";
    private static final String BLIT_HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/MiddleClientCompat";
    private static final String LEGACY_BLIT_DESCRIPTOR =
            "(Lnet/minecraft/client/gui/GuiGraphics;"
                    + "Lnet/minecraft/resources/ResourceLocation;"
                    + "IIIIIIIIII)V";

    private PalladiumWidgetButtons1201() {
    }

    /**
     * Returns one for a migrated button class, zero when the class is not one
     * of the two inspected Palladium classes or does not contain the old field.
     * A recognized field without its paired nine-slice call is rejected rather
     * than producing bytecode that silently renders a null texture.
     */
    static int apply(ClassNode node) {
        if (!ICON_BUTTON.equals(node.name) && !TEXT_WITH_ICON_BUTTON.equals(node.name)) {
            return 0;
        }

        boolean migrated = false;
        for (MethodNode method : node.methods) {
            java.util.List<FieldInsnNode> oldWidgetFields = new java.util.ArrayList<>();
            java.util.List<MethodInsnNode> oldNineSliceCalls = new java.util.ArrayList<>();

            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null; instruction = instruction.getNext()) {
                if (instruction instanceof FieldInsnNode field
                        && field.getOpcode() == GETSTATIC
                        && "WIDGETS_LOCATION".equals(field.name)
                        && RESOURCE_LOCATION.equals(field.desc)
                        && (node.name.equals(field.owner) || MINECRAFT_BUTTON.equals(field.owner))) {
                    oldWidgetFields.add(field);
                } else if (instruction instanceof MethodInsnNode call
                        && call.getOpcode() == INVOKESTATIC
                        && BLIT_HELPER.equals(call.owner)
                        && "blitNineSliced".equals(call.name)
                        && LEGACY_BLIT_DESCRIPTOR.equals(call.desc)) {
                    oldNineSliceCalls.add(call);
                }
            }

            if (oldWidgetFields.isEmpty()) {
                continue;
            }
            if (oldNineSliceCalls.size() != 1) {
                throw new IllegalStateException("Palladium button " + node.name + "." + method.name
                        + " reads the removed WIDGETS_LOCATION field but has "
                        + oldNineSliceCalls.size() + " matching legacy nine-slice calls");
            }

            for (FieldInsnNode field : oldWidgetFields) {
                method.instructions.set(field, new InsnNode(ACONST_NULL));
            }
            oldNineSliceCalls.get(0).name = "blitPalladiumDefaultButton";
            migrated = true;
        }

        return migrated ? 1 : 0;
    }
}
