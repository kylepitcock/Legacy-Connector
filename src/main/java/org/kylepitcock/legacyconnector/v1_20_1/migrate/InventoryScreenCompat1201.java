package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

/**
 * Keeps AlienEvo's 1.20.1 centre-anchored entity-preview call working on
 * Minecraft 1.21.1's rectangle-based InventoryScreen preview API.
 */
public final class InventoryScreenCompat1201 {
    private InventoryScreenCompat1201() {
    }

    /**
     * Compatibility signature used by AlienEvo's original bytecode:
     * graphics, centre X/Y, model scale, mouse deltas and entity.
     *
     * <p>AlienEvo passes {@code anchor - cursor} for both mouse arguments,
     * while Minecraft 1.21.1 expects absolute GUI cursor coordinates. Recover
     * the original cursor position before delegating; forwarding the deltas
     * unchanged makes the preview rotate around the wrong point.
     *
     * <p>The 1.21.1 overload requires a clipping rectangle. Use a screen-sized
     * rectangle centred on the legacy anchor so that it does not introduce a
     * smaller crop than the old centre-anchored renderer.
     */
    public static void renderEntityInInventoryFollowsMouse(
            GuiGraphics graphics,
            int x,
            int y,
            int scale,
            float mouseX,
            float mouseY,
            LivingEntity entity) {
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();
        int halfWidth = Math.max(1, guiWidth);
        int halfHeight = Math.max(1, guiHeight);
        int left = x - halfWidth;
        int top = y - halfHeight;
        int right = x + halfWidth;
        int bottom = y + halfHeight;
        float absoluteMouseX = absoluteMouseCoordinate(x, mouseX);
        float absoluteMouseY = absoluteMouseCoordinate(y, mouseY);

        AlienEvoRuntimeDiagnostics1201Runtime.traceOmnitrixPowerPreview(
                entity, x, y, scale, guiWidth, guiHeight, mouseX, mouseY,
                absoluteMouseX, absoluteMouseY, left, top, right, bottom, guiPoseSnapshot(graphics));

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, left, top, right, bottom, scale, 0.0F,
                absoluteMouseX, absoluteMouseY, entity);
    }

    static float absoluteMouseCoordinate(float anchor, float legacyDelta) {
        return anchor - legacyDelta;
    }

    private static String guiPoseSnapshot(GuiGraphics graphics) {
        try {
            var pose = graphics.pose().last();
            return "pose=" + pose.pose() + ",normal=" + pose.normal();
        } catch (Throwable unavailable) {
            return "unavailable:" + unavailable.getClass().getSimpleName();
        }
    }

}
