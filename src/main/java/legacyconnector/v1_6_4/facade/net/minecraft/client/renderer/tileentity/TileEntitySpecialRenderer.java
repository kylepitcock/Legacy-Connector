package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer.tileentity;

/** Facade of the 1.7.10 shim's {@code TileEntitySpecialRenderer} for 1.4.7 and 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class TileEntitySpecialRenderer {
    private TileEntitySpecialRenderer() {}

    /** {@code bindTextureByURL(url, fallbackPath)}: binds a downloaded image, the fallback texture while it has not arrived (see {@code Render.loadDownloadableImageTexture}). */
    public static void bindTextureByURL(legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer self, String url, String fallback) {
        legacyconnector.v1_6_4.facade.net.minecraft.client.renderer.entity.Render.mcc$bindDownloadable(url, fallback);
    }
}
