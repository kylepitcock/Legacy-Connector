package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
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
    // Mc1201Migration redirects this same legacy signature through its generic client helper first.
    static final String MIDDLE_CLIENT_COMPAT =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/MiddleClientCompat";
    static final String CLIENT_COMPAT =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/ClientCompat";
    static final String GUI_GRAPHICS = "net/minecraft/client/gui/GuiGraphics";
    static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";

    static final String LEGACY_PREVIEW_DESC =
            "(L" + GUI_GRAPHICS + ";IIIFFL" + LIVING_ENTITY + ";)V";
    static final String PREVIEW_METHOD_DESC =
            "(L" + GUI_GRAPHICS + ";IIFFFL" + LIVING_ENTITY + ";FZ)V";
    static final String LEGACY_BACKGROUND_DESC =
            "(L" + GUI_GRAPHICS + ";)V";
    static final String MIGRATED_BACKGROUND_DESC =
            "(Lnet/minecraft/client/gui/screens/Screen;L" + GUI_GRAPHICS + ";)V";
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
     * The generic Minecraft migration may already have redirected this exact
     * signature to MiddleClientCompat before this AlienEvo-specific pass runs.
     * Accept that known static bridge as already adapted; otherwise route the
     * untouched call through the AlienEvo-specific compatibility helper.
     */
    private static int adaptInventoryPreview(ClassNode node) {
        MethodNode renderEntity = findUniqueMethod(node, "renderEntity", PREVIEW_METHOD_DESC);
        if (renderEntity == null) {
            throw new IllegalStateException("AlienEvo AlienPreviewWidget.renderEntity method shape changed");
        }

        MethodInsnNode legacy = null;
        MethodInsnNode genericBridge = null;
        int legacyCount = 0;
        int inventoryCompatCount = 0;
        int middleClientCompatCount = 0;
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
                inventoryCompatCount++;
            } else if (MIDDLE_CLIENT_COMPAT.equals(call.owner)
                    && call.getOpcode() == INVOKESTATIC) {
                // The generic bridge's 2x2 viewport is suitable for its original
                // tiny anchor contract, but clips AlienEvo's much larger model preview.
                genericBridge = call;
                middleClientCompatCount++;
            } else {
                throw new IllegalStateException(
                        "AlienEvo preview has an unexpected InventoryScreen preview invocation owner/opcode");
            }
        }

        if (legacyCount == 0 && inventoryCompatCount == 1 && middleClientCompatCount == 0) {
            return 0; // Already using the AlienEvo-sized viewport bridge.
        }
        if (legacyCount == 0 && inventoryCompatCount == 0
                && middleClientCompatCount == 1 && genericBridge != null) {
            genericBridge.owner = INVENTORY_COMPAT;
            genericBridge.setOpcode(INVOKESTATIC);
            genericBridge.itf = false;
            return 1;
        }
        if (legacyCount == 1 && inventoryCompatCount == 0
                && middleClientCompatCount == 0 && legacy != null) {
            legacy.owner = INVENTORY_COMPAT;
            legacy.setOpcode(INVOKESTATIC);
            legacy.itf = false;
            return 1;
        }

        throw new IllegalStateException(
                "AlienEvo AlienPreviewWidget expected exactly one legacy, generic or AlienEvo preview call; found "
                        + legacyCount + " legacy, " + middleClientCompatCount + " generic and "
                        + inventoryCompatCount + " AlienEvo helpers");
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
        int migratedNoopCount = 0;
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
            } else if (MIGRATED_BACKGROUND_DESC.equals(call.desc)
                    && CLIENT_COMPAT.equals(call.owner)
                    && call.getOpcode() == INVOKESTATIC) {
                // Mc1201Migration already redirected Screen.renderBackground to
                // ClientCompat's intentionally empty 1.21.1 compatibility hook.
                migratedNoopCount++;
            } else if (MODERN_BACKGROUND_DESC.equals(call.desc)) {
                modernCount++;
            } else {
                throw new IllegalStateException(
                        "AlienEvo PlumberBadgeScreen has an unexpected background compatibility invocation");
            }
        }

        if (legacyCount == 0 && modernCount == 0 && migratedNoopCount <= 1) {
            // Screen.render() in 1.21.1 calls the new four-argument virtual
            // renderBackground hook. Removing the old one-argument call is not
            // enough: without this override, super.render() still applies blur.
            return ensureNoopModernBackground(node);
        }
        if (legacyCount != 1 || modernCount != 0 || migratedNoopCount != 0 || legacy == null) {
            throw new IllegalStateException(
                    "AlienEvo PlumberBadgeScreen expected one legacy background call; found "
                            + legacyCount + " legacy, " + migratedNoopCount + " no-op compat and "
                            + modernCount + " modern");
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
        return 1 + ensureNoopModernBackground(node);
    }

    /**
     * Screen.render() dispatches this modern hook virtually after the Badge's
     * own render body calls super.render(). Override it with an empty body so
     * vanilla's 1.21.1 in-world blur is suppressed without skipping widgets.
     */
    private static int ensureNoopModernBackground(ClassNode node) {
        MethodNode existing = findUniqueMethod(node, "renderBackground", MODERN_BACKGROUND_DESC);
        if (existing != null) {
            if ((existing.access & ACC_PUBLIC) == 0 || existing.instructions == null) {
                throw new IllegalStateException(
                        "AlienEvo PlumberBadgeScreen has an incompatible modern background override");
            }
            int executable = 0;
            for (AbstractInsnNode instruction : existing.instructions.toArray()) {
                if (instruction.getOpcode() < 0) continue;
                if (instruction.getOpcode() != RETURN) {
                    throw new IllegalStateException(
                            "AlienEvo PlumberBadgeScreen modern background override is not a no-op");
                }
                executable++;
            }
            if (executable != 1) {
                throw new IllegalStateException(
                        "AlienEvo PlumberBadgeScreen modern background override has an unexpected body");
            }
            return 0;
        }

        MethodNode noOp = new MethodNode(ACC_PUBLIC, "renderBackground", MODERN_BACKGROUND_DESC, null, null);
        noOp.instructions.add(new InsnNode(RETURN));
        noOp.maxLocals = 5;
        node.methods.add(noOp);
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
