package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

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
    private static final String SELECTION_LIST_COMPAT =
            "legacyconnector/v1_20_1/PalladiumSelectionListCompat";
    private static final String RENDER_BACKGROUND = "setRenderBackground";
    private static final String RENDER_TOP_AND_BOTTOM = "setRenderTopAndBottom";
    private static final String RENDER_SELECTION = "setRenderSelection";
    private static final String RENDER_LIST_BACKGROUND = "renderListBackground";
    private static final String RENDER_LIST_SEPARATORS = "renderListSeparators";
    private static final String RENDER_SELECTION_HOOK = "renderSelection";
    private static final String GUI_GRAPHICS = "net/minecraft/client/gui/GuiGraphics";
    private static final String RENDER_BACKGROUND_DESC = "(L" + GUI_GRAPHICS + ";)V";
    private static final String RENDER_SELECTION_DESC = "(L" + GUI_GRAPHICS + ";IIIII)V";
    private static final String RENDER_FLAG_DESC = "(Z)V";
    private static final String RENDER_FLAG_HELPER_DESC = "(Ljava/lang/Object;Z)V";
    private static final String RENDER_CHECK_DESC = "(Ljava/lang/Object;)Z";
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
        changes += migrateRenderFlags(node);
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

    /**
     * Replaces the removed AbstractSelectionList setter calls with connector
     * state updates, then overrides the three 1.21.1 rendering hooks so each
     * legacy preference is preserved for this particular list instance.
     */
    private static int migrateRenderFlags(ClassNode node) {
        int changes = 0;
        boolean needsBackground = false;
        boolean needsSeparators = false;
        boolean needsSelection = false;

        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)
                        || !call.desc.equals(RENDER_FLAG_DESC)
                        || !(call.owner.equals(node.name)
                        || call.owner.equals(node.superName)
                        || call.owner.equals("net/minecraft/client/gui/components/AbstractSelectionList"))) {
                    continue;
                }
                String helperMethod;
                if (call.name.equals(RENDER_BACKGROUND)) {
                    helperMethod = RENDER_BACKGROUND;
                    needsBackground = true;
                } else if (call.name.equals(RENDER_TOP_AND_BOTTOM)) {
                    helperMethod = RENDER_TOP_AND_BOTTOM;
                    needsSeparators = true;
                } else if (call.name.equals(RENDER_SELECTION)) {
                    helperMethod = RENDER_SELECTION;
                    needsSelection = true;
                } else {
                    continue;
                }
                call.setOpcode(INVOKESTATIC);
                call.owner = SELECTION_LIST_COMPAT;
                call.name = helperMethod;
                call.desc = RENDER_FLAG_HELPER_DESC;
                call.itf = false;
                changes++;
            }
        }

        if (needsBackground) changes += installRenderHook(
                node, RENDER_LIST_BACKGROUND, RENDER_BACKGROUND_DESC, RENDER_BACKGROUND);
        if (needsSeparators) changes += installRenderHook(
                node, RENDER_LIST_SEPARATORS, RENDER_BACKGROUND_DESC, RENDER_TOP_AND_BOTTOM);
        if (needsSelection) changes += installRenderHook(
                node, RENDER_SELECTION_HOOK, RENDER_SELECTION_DESC, RENDER_SELECTION);
        return changes;
    }

    /**
     * If Palladium already overrides the modern hook, prepend the legacy flag
     * guard. Otherwise add a small override that delegates to the direct
     * superclass only when the old setting is enabled.
     */
    private static int installRenderHook(ClassNode node, String hook, String descriptor, String flag) {
        String checkMethod = switch (flag) {
            case RENDER_BACKGROUND -> "renderBackground";
            case RENDER_TOP_AND_BOTTOM -> "renderTopAndBottom";
            case RENDER_SELECTION -> "renderSelection";
            default -> throw new IllegalArgumentException("Unknown Palladium list render flag: " + flag);
        };
        MethodNode existing = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals(hook) && method.desc.equals(descriptor)) {
                if (existing != null) {
                    throw new IllegalStateException("Palladium list widget has duplicate " + hook + descriptor);
                }
                existing = method;
            }
        }

        InsnList guard = new InsnList();
        LabelNode enabled = new LabelNode();
        guard.add(new VarInsnNode(ALOAD, 0));
        guard.add(new MethodInsnNode(INVOKESTATIC, SELECTION_LIST_COMPAT,
                checkMethod, RENDER_CHECK_DESC, false));
        guard.add(new JumpInsnNode(IFNE, enabled));
        guard.add(new InsnNode(RETURN));
        guard.add(enabled);

        if (existing != null) {
            if ((existing.access & (ACC_STATIC | ACC_ABSTRACT | ACC_NATIVE)) != 0) {
                throw new IllegalStateException("Palladium list widget render hook has unsupported access flags: "
                        + node.name + "." + hook);
            }
            existing.instructions.insert(guard);
            existing.maxStack = Math.max(existing.maxStack, 1);
            return 1;
        }

        MethodNode generated = new MethodNode(ACC_PROTECTED, hook, descriptor, null, null);
        generated.instructions.add(guard);
        generated.instructions.add(new VarInsnNode(ALOAD, 0));
        int local = 1;
        for (org.objectweb.asm.Type argument : org.objectweb.asm.Type.getArgumentTypes(descriptor)) {
            generated.instructions.add(new VarInsnNode(argument.getOpcode(ILOAD), local));
            local += argument.getSize();
        }
        generated.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.superName, hook, descriptor, false));
        generated.instructions.add(new InsnNode(RETURN));
        generated.maxStack = local + 1;
        generated.maxLocals = local;
        node.methods.add(generated);
        return 1;
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
