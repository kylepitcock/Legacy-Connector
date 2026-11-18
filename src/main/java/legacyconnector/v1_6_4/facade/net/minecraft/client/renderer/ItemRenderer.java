package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer;

/** Facade of the 1.7.10 shim's {@code ItemRenderer} for 1.4.7 and 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class ItemRenderer {
    private ItemRenderer() {}

    /**
     * {@code renderItemIn2D(tessellator, u1, v1, u2, v2)}: the flat item with the thickness of a 16 by 16 sprite (1.4.7 knew no other size); 1.7.10 takes the
     * sprite size and the thickness as arguments.
     */
    public static void renderItemIn2D(legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator tessellator, float u1, float v1, float u2, float v2) {
        legacyconnector.v1_7_10.net.minecraft.client.renderer.ItemRenderer.renderItemIn2D(tessellator, u1, v1, u2, v2, 16, 16, 0.0625F);
    }

    /**
     * 1.4.7 {@code renderItemIn2D(tessellator, u1, v1, u2, v2, thickness)} (the vanilla 1.4.7 method takes five floats: the sprite rectangle and the depth of the extruded
     * item, 0.0625 for every vanilla item; 1.5.2 added the sprite size and 1.7.10 keeps both): the flat item of a 16 by 16 sprite with that thickness.
     */
    public static void renderItemIn2D(legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator tessellator, float u1, float v1, float u2, float v2, float thickness) {
        legacyconnector.v1_7_10.net.minecraft.client.renderer.ItemRenderer.renderItemIn2D(tessellator, u1, v1, u2, v2, 16, 16, thickness);
    }

    private static legacyconnector.v1_7_10.net.minecraft.client.gui.MapItemRenderer mapRenderer;

    /** 1.6.4 {@code mapItemRenderer}: the renderer that draws a held map's canvas (1.7.10 creates it inside the map item's own rendering). */
    public static synchronized legacyconnector.v1_7_10.net.minecraft.client.gui.MapItemRenderer mcc$get$mapItemRenderer(legacyconnector.v1_7_10.net.minecraft.client.renderer.ItemRenderer self) {
        if (mapRenderer == null) mapRenderer = new legacyconnector.v1_7_10.net.minecraft.client.gui.MapItemRenderer(legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().getTextureManager());
        return mapRenderer;
    }
}
