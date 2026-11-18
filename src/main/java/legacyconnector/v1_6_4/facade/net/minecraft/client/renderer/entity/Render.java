package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;

/** Facade of the 1.7.10 shim's {@code Render} for 1.4.7 and 1.5.2 mods (reached from {@code RenderLiving} and {@code RenderPlayer}). See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class Render {
    private Render() {}

    /**
     * {@code loadDownloadableImageTexture(url, fallbackPath)}: binds the image of an address (a skin or cape), the fallback texture while it has not arrived. False
     * when there is neither, so the caller draws with the texture it already had.
     */
    public static boolean loadDownloadableImageTexture(legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.Render self, String url, String fallback) {
        return mcc$bindDownloadable(url, fallback);
    }

    /** The {@code super.loadDownloadableImageTexture(url, fallback)} of a renderer that overrides it. */
    public static boolean mcc$super$loadDownloadableImageTexture(legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.Render self, String url, String fallback) {
        return mcc$bindDownloadable(url, fallback);
    }

    public static boolean mcc$bindDownloadable(String url, String fallback) {
        TextureManager manager = TextureManager.INSTANCE;
        int id = legacyconnector.v1_6_4.facade.net.minecraft.client.renderer.texture.TextureManager.getTextureForDownloadableImage(manager, url, fallback);
        if (id < 0) return false;
        manager.bindTexture(id);
        return true;
    }
}
