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
}
