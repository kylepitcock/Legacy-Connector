package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

/**
 * Keeps AlienEvo's 1.20.1 centre-anchored entity-preview call working on
 * Minecraft 1.21.1's rectangle-based InventoryScreen preview API.
 */
public final class InventoryScreenCompat1201 {
    private static final ThreadLocal<Viewport> ACTIVE_VIEWPORT = new ThreadLocal<>();

    private InventoryScreenCompat1201() {
    }

    /** Supplies the active screen's actual preview-panel clip rectangle. */
    public static void setViewportFromBounds(int left, int top, int right, int bottom) {
        if (right <= left || bottom <= top) {
            throw new IllegalArgumentException("AlienEvo preview viewport must have positive width and height");
        }
        ACTIVE_VIEWPORT.set(new Viewport(left, top, right, bottom));
    }

    /**
     * Supplies the exact StatsPage portrait band bounds in screen coordinates.
     * This mirrors the original dev.24 canvas/scissor math.
     */
    public static void setBadgeViewport(float originX, float originY, float scale, int bottom) {
        int[] bounds = badgeViewportBounds(originX, originY, scale, bottom);
        setViewportFromBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    /** Clears the per-render viewport on both normal and exceptional exits. */
    public static void clearViewport() {
        ACTIVE_VIEWPORT.remove();
    }

    static int[] badgeViewportBounds(float originX, float originY, float scale, int bottom) {
        return new int[] {
                Math.round(originX + 130.0F * scale),
                Math.round(originY + 80.0F * scale),
                Math.round(originX + 320.0F * scale),
                Math.round(originY + bottom * scale)
        };
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
     * <p>The new API requires a clip rectangle. For Omnitrix and Badge screens,
     * use their actual panel bounds. Retain the large fallback only for an
     * uninstrumented legacy call site.
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
        Viewport viewport = ACTIVE_VIEWPORT.get();
        int left = viewport == null ? x - Math.max(1, guiWidth) : viewport.left();
        int top = viewport == null ? y - Math.max(1, guiHeight) : viewport.top();
        int right = viewport == null ? x + Math.max(1, guiWidth) : viewport.right();
        int bottom = viewport == null ? y + Math.max(1, guiHeight) : viewport.bottom();
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

    private record Viewport(int left, int top, int right, int bottom) {
    }
}
