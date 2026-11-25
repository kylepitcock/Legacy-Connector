package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Adapts AlienEvo's shared GUI entity-preview call and removes the old Badge
 * screen background call whose 1.21.1 replacement adds an unwanted blur pass.
 */
final class AlienEvoGuiRender1201 implements Opcodes {
    static final String PREVIEW_WIDGET =
            "net/habb/alienevo/client/screen/power/AlienPreviewWidget";
    static final String BADGE_SCREEN =
            "net/habb/alienevo/client/screen/badge/PlumberBadgeScreen";
    static final String SCALED_SCREEN =
            "net/habb/alienevo/client/screen/ScaledScreen";
    static final String SCREEN = "net/minecraft/client/gui/screens/Screen";
    static final String INVENTORY_SCREEN =
            "net/minecraft/client/gui/screens/inventory/InventoryScreen";
    static final String INVENTORY_COMPAT =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/InventoryScreenCompat1201";
    static final String GUI_GRAPHICS = "net/minecraft/client/gui/GuiGraphics";
    static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";

    static final String LEGACY_PREVIEW_DESC =
            "(L" + GUI_GRAPHICS + ";IIIFFL" + LIVING_ENTITY + ";)V";
    static final String PREVIEW_METHOD_DESC =
            "(L" + GUI_GRAPHICS + ";IIFFFL" + LIVING_ENTITY + ";FZ)V";
    static final String LEGACY_BACKGROUND_DESC =
            "(L" + GUI_GRAPHICS + ";)V";
    static final String MODERN_BACKGROUND_DESC =
            "(L" + GUI_GRAPHICS + ";IIF)V";
    static final String BADGE_RENDER_DESC =
            "(L" + GUI_GRAPHICS + ";IIF)V";

    private AlienEvoGuiRender1201() {
    }

    static int apply(ClassNode node) {
        if (PREVIEW_WIDGET.equals(node.name)) {
            return adaptInventoryPreview(node);
        }
        if (BADGE_SCREEN.equals(node.name)) {
            return removeLegacyBadgeBackground(node);
        }
        return 0;
    }

    /**
     * AlienEvo 1.20.1 calls the seven-argument InventoryScreen preview API.
     * Minecraft 1.21.1 replaced it with the rectangle-based ten-argument API.
     * Keep the legacy call descriptor at the call site and route it through a
     * small runtime helper that supplies a screen-sized clip rectangle centred
     * on the original x/y anchor.
     */
    private static int adaptInventoryPreview(ClassNode node) {
        MethodNode renderEntity = findUniqueMethod(node, "renderEntity", PREVIEW_METHOD_DESC);
        if (renderEntity == null) {
            throw new IllegalStateException("AlienEvo AlienPreviewWidget.renderEntity method shape changed");
        }

        MethodInsnNode legacy = null;
        int legacyCount = 0;
        int helperCount = 0;
        for (AbstractInsnNode instruction : renderEntity.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode call)
                    || !call.name.equals("renderEntityInInventoryFollowsMouse")
                    || !LEGACY_PREVIEW_DESC.equals(call.desc)) {
                continue;
            }
            if (INVENTORY_SCREEN.equals(call.owner)
                    && call.getOpcode() == INVOKESTATIC) {
                legacy = call;
                legacyCount++;
            } else if (INVENTORY_COMPAT.equals(call.owner)
                    && call.getOpcode() == INVOKESTATIC) {
                helperCount++;
            } else {
                throw new IllegalStateException(
                        "AlienEvo preview has an unexpected InventoryScreen preview invocation owner/opcode");
            }
        }

        if (legacyCount == 0 && helperCount == 1) {
            return 0; // Already adapted.
        }
        if (legacyCount != 1 || helperCount != 0 || legacy == null) {
            throw new IllegalStateException(
                    "AlienEvo AlienPreviewWidget expected exactly one legacy InventoryScreen preview call; found "
                            + legacyCount + " legacy and " + helperCount + " adapted");
        }

        legacy.owner = INVENTORY_COMPAT;
        legacy.setOpcode(INVOKESTATIC);
        legacy.itf = false;
        return 1;
    }

    /**
     * PlumberBadgeScreen has its own opaque panel/backdrop drawing. Its legacy
     * renderBackground(GuiGraphics) call has no direct equivalent in 1.21.1:
     * the modern Screen overload applies the new in-world blur. Remove only
     * that explicit background call and retain the screen's own backdrop and
     * inherited widget rendering.
     */
    private static int removeLegacyBadgeBackground(ClassNode node) {
        MethodNode render = findUniqueMethod(node, "render", BADGE_RENDER_DESC);
        if (render == null) {
            throw new IllegalStateException("AlienEvo PlumberBadgeScreen.render method shape changed");
        }

        MethodInsnNode legacy = null;
        int legacyCount = 0;
        int modernCount = 0;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode call)
                    || !call.name.equals("renderBackground")) {
                continue;
            }
            if (LEGACY_BACKGROUND_DESC.equals(call.desc)) {
                if ((call.getOpcode() != INVOKEVIRTUAL && call.getOpcode() != INVOKESPECIAL)
                        || (!node.name.equals(call.owner)
                        && !SCALED_SCREEN.equals(call.owner)
                        && !SCREEN.equals(call.owner))) {
                    throw new IllegalStateException(
                            "AlienEvo PlumberBadgeScreen has an unexpected legacy background invocation");
                }
                legacy = call;
                legacyCount++;
            } else if (MODERN_BACKGROUND_DESC.equals(call.desc)) {
                modernCount++;
            }
        }

        if (legacyCount == 0 && modernCount == 0) {
            return 0; // Already removed.
        }
        if (legacyCount != 1 || modernCount != 0 || legacy == null) {
            throw new IllegalStateException(
                    "AlienEvo PlumberBadgeScreen expected one legacy background call; found "
                            + legacyCount + " legacy and " + modernCount + " modern");
        }

        AbstractInsnNode graphicsLoad = previousMeaningful(legacy);
        AbstractInsnNode screenLoad = previousMeaningful(graphicsLoad);
        if (!(graphicsLoad instanceof VarInsnNode graphics
                && graphics.getOpcode() == ALOAD && graphics.var == 1)
                || !(screenLoad instanceof VarInsnNode screen
                && screen.getOpcode() == ALOAD && screen.var == 0)) {
            throw new IllegalStateException(
                    "AlienEvo PlumberBadgeScreen background call no longer has the inspected this/GuiGraphics stack shape");
        }

        render.instructions.remove(screenLoad);
        render.instructions.remove(graphicsLoad);
        render.instructions.remove(legacy);
        return 1;
    }

    private static MethodNode findUniqueMethod(ClassNode node, String name, String descriptor) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) {
                if (found != null) {
                    throw new IllegalStateException("Duplicate AlienEvo method " + name + descriptor);
                }
                found = method;
            }
        }
        return found;
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode value = instruction == null ? null : instruction.getPrevious();
        while (value != null && value.getOpcode() < 0) {
            value = value.getPrevious();
        }
        return value;
    }
}
