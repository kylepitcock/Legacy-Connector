package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer;

/** Facade of the 1.7.10 shim's {@code RenderGlobal} for 1.4.7 and 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class RenderGlobal {
    private RenderGlobal() {}

    /** {@code drawOutlinedBoundingBox(box)} (static): the 12 edges of the box in the current colour (1.7.10 takes the colour as an argument). */
    public static void drawOutlinedBoundingBox(legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB box) {
        legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal.drawOutlinedBoundingBox(box, -1);
    }

    /**
     * {@code onEntityCreate(entity)}: the world access hook for a spawned entity. 1.5.2 used it to start the skin and cloak download; the 1.7.10 game's
     * (and this layer's) renderer does nothing there, entities are tracked when the world adds them.
     */
    public static void onEntityCreate(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal self, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
    }

    /** {@code onEntityDestroy(entity)}: the counterpart of {@code onEntityCreate}, empty in 1.7.10 as well. */
    public static void onEntityDestroy(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal self, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
    }
}
