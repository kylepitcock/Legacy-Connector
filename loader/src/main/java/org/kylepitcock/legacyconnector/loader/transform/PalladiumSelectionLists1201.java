package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Palladium 4.5.10's legacy 1.20.1 list widgets include empty overrides of
 * AbstractSelectionList.updateNarration(). Minecraft 1.21.1 makes the
 * inherited AbstractWidget.updateNarration() final, so those old overrides
 * make the nested classes fail JVM verification. Removing only the verified
 * no-op methods lets the lists inherit the final implementation.
 */
final class PalladiumSelectionLists1201 implements Opcodes {
    static final String ACCESSORY_SLOT_LIST =
            "net/threetag/palladium/client/screen/AccessoryScreen$AccessorySlotList";
    static final String ACCESSORY_LIST =
            "net/threetag/palladium/client/screen/AccessoryScreen$AccessoryList";
    static final String ADDON_PACK_DEV_LOG_LIST =
            "net/threetag/palladium/client/screen/AddonPackLogScreen$DevLogList";
    static final String POWER_ABILITY_LIST =
            "net/threetag/palladium/client/screen/power/ListPowerTab$AbilityList";
    static final String TAILORING_RECIPE_LIST =
            "net/threetag/palladium/client/screen/TailoringScreen$RecipeList";
    static final String ADDON_LIST_WIDGET =
            "net/threetag/palladium/client/screen/hub/AddonListWidget";

    private static final Map<String, String> TARGET_SUPERS = Map.of(
            ACCESSORY_SLOT_LIST, "net/minecraft/client/gui/components/AbstractSelectionList",
            ACCESSORY_LIST, "net/minecraft/client/gui/components/AbstractSelectionList",
            ADDON_PACK_DEV_LOG_LIST, "net/minecraft/client/gui/components/AbstractSelectionList",
            POWER_ABILITY_LIST, "net/minecraft/client/gui/components/AbstractSelectionList",
            TAILORING_RECIPE_LIST, "net/minecraft/client/gui/components/AbstractSelectionList",
            ADDON_LIST_WIDGET, "net/minecraft/client/gui/components/ObjectSelectionList");
    private static final String ABSTRACT_WIDGET =
            "net/minecraft/client/gui/components/AbstractWidget";
    private static final String UPDATE_NARRATION = "updateNarration";
    private static final String UPDATE_WIDGET_NARRATION = "updateWidgetNarration";
    private static final String UPDATE_NARRATION_DESC =
            "(Lnet/minecraft/client/gui/narration/NarrationElementOutput;)V";

    private PalladiumSelectionLists1201() {
    }

    /**
     * Migrates legacy inherited {@code x0} references to the 1.21.1 widget
     * position API and renames only verified empty narration overrides.
     *
     * @return the number of bytecode sites changed
     */
    static int apply(ClassNode node) {
        String expectedSuper = TARGET_SUPERS.get(node.name);
        if (expectedSuper == null) return 0;
        if (!expectedSuper.equals(node.superName)) {
            throw new IllegalStateException(
                    "Palladium list widget superclass changed: " + node.name + " extends " + node.superName);
        }

        int changes = migrateLeftCoordinate(node);
        MethodNode target = null;
        for (MethodNode method : node.methods) {
            if (!UPDATE_NARRATION.equals(method.name) || !UPDATE_NARRATION_DESC.equals(method.desc)) continue;
            if (target != null) {
                throw new IllegalStateException("Palladium list widget has duplicate updateNarration overrides: " + node.name);
            }
            target = method;
        }
        if (target == null) return changes;
        if ((target.access & (ACC_STATIC | ACC_ABSTRACT | ACC_NATIVE)) != 0
                || (target.access & (ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE)) != ACC_PUBLIC) {
            throw new IllegalStateException(
                    "Palladium list widget updateNarration access flags changed: " + node.name);
        }

        int executableInstructions = 0;
        for (AbstractInsnNode instruction : target.instructions) {
            if (instruction.getOpcode() < 0) continue;
            executableInstructions++;
            if (instruction.getOpcode() != RETURN) {
                throw new IllegalStateException(
                        "Palladium list widget updateNarration is no longer the verified empty no-op: " + node.name);
            }
        }
        if (executableInstructions != 1) {
            throw new IllegalStateException(
                    "Palladium list widget updateNarration no longer contains exactly one RETURN: " + node.name);
        }

        for (MethodNode method : node.methods) {
            if (UPDATE_WIDGET_NARRATION.equals(method.name) && UPDATE_NARRATION_DESC.equals(method.desc)) {
                throw new IllegalStateException(
                        "Palladium list widget already declares updateWidgetNarration: " + node.name);
            }
        }
        target.name = UPDATE_WIDGET_NARRATION;
        return changes + 1;
    }

    private static int migrateLeftCoordinate(ClassNode node) {
        int changes = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof FieldInsnNode field)
                        || !field.owner.equals(node.name)
                        || !(field.name.equals("x0") || field.name.equals("f_93393_"))
                        || !field.desc.equals("I")) {
                    continue;
                }
                MethodInsnNode replacement;
                if (field.getOpcode() == GETFIELD) {
                    replacement = new MethodInsnNode(INVOKEVIRTUAL, ABSTRACT_WIDGET, "getX", "()I", false);
                } else if (field.getOpcode() == PUTFIELD) {
                    replacement = new MethodInsnNode(INVOKEVIRTUAL, ABSTRACT_WIDGET, "setX", "(I)V", false);
                } else {
                    throw new IllegalStateException(
                            "Palladium list widget uses x0 with an unexpected field opcode: "
                                    + node.name + "." + method.name);
                }
                method.instructions.set(field, replacement);
                changes++;
            }
        }
        return changes;
    }
}
