package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import legacyconnector.v1_6_4.net.minecraft.client.renderer.texture.Texture;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.IImageBuffer;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.ImageBufferDownload;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.ThreadDownloadImageData;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Textures152;

/**
 * Facade of the 1.7.10 shim's {@code TextureManager} for 1.4.7 and 1.5.2 mods (their {@code RenderEngine}, and 1.5.2's own {@code TextureManager} singleton
 * of sprite sheets, which the relocation puts on the same class). See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class TextureManager {
    private TextureManager() {}

    /** {@code deleteTexture(glId)}: frees the GL texture a mod made with {@code allocateAndSetupTexture} or {@code createTextureFromBytes}. */
    public static void deleteTexture(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self, int glId) {
        if (glId > 0) com.mojang.blaze3d.platform.GlStateManager._deleteTexture(glId);
    }

    /** Downloaded images by their address: one texture object per URL, as the original's image data map. */
    private static final java.util.Map<String, ThreadDownloadImageData> DOWNLOADS = new java.util.concurrent.ConcurrentHashMap<>();

    private static ResourceLocation downloadLocation(String url) {
        return new ResourceLocation("legacyconnector", "download/" + Integer.toHexString(url.hashCode()) + "_" + Math.abs((long) url.length()));
    }

    /**
     * {@code obtainImageData(url, buffer)}: starts the download of an image (a skin, a cape, a mod's remote picture) and returns the object that holds it;
     * asked again for the same address it is the same object. The image goes through the layer's own downloader (skins.minecraft.net addresses are
     * answered with the profile's texture), and is cut by {@code buffer} when it arrives.
     */
    public static ThreadDownloadImageData obtainImageData(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self, String url,
            IImageBuffer buffer) {
        return obtain(self, url, buffer, null);
    }

    private static ThreadDownloadImageData obtain(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self, String url, IImageBuffer buffer,
            String fallback) {
        if (url == null) return null;
        return DOWNLOADS.computeIfAbsent(url, u -> {
            var data = new ThreadDownloadImageData(u, fallback == null ? null : Textures152.location(fallback), buffer);
            self.loadTexture(downloadLocation(u), data);
            return data;
        });
    }

    /**
     * {@code getTextureForDownloadableImage(url, fallbackPath)}: the texture id of a downloaded image, or of the fallback path's texture while there is no
     * image (the fallback is what the original showed until the download finished). -1 when both are null.
     */
    public static int getTextureForDownloadableImage(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self, String url, String fallback) {
        if (url == null) return fallback == null ? -1 : self.getTexture(fallback);
        var data = obtain(self, url, new ImageBufferDownload(), fallback);
        if (data == null) return fallback == null ? -1 : self.getTexture(fallback);
        return self.mcc$handleFor(downloadLocation(url));
    }

    /** {@code getMissingIcon(textureType)}: the purple and black square of the sheet (blocks and items alike). */
    public static legacyconnector.v1_7_10.net.minecraft.util.IIcon getMissingIcon(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self, int type) {
        return TextureMap.missingIcon();
    }

    /** {@code createTextureFromBytes(argb, width, height, glId)}: fills a texture a mod made earlier with these pixels. */
    public static void createTextureFromBytes(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self, int[] pixels, int width, int height, int glId) {
        if (pixels == null || width <= 0 || height <= 0 || pixels.length < width * height) return;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        self.mcc$replaceImage(glId, image);
    }

    /** {@code setupTexture(image, glId)}: fills a texture a mod made earlier with this image. */
    public static void setupTexture(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self, BufferedImage image, int glId) {
        self.mcc$replaceImage(glId, image);
    }

    /**
     * {@code getTextureContents(path)}: the ARGB pixels of a texture file (read like {@code loadImageFromTexturePack}: the mod's class path, then the resource
     * packs); a 16 by 16 missing-texture square for a path nothing has, as the original answered.
     */
    public static int[] getTextureContents(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self, String path) {
        try {
            BufferedImage image = legacyconnector.v1_6_4.cpw.mods.fml.client.TextureFXManager.instance().loadImageFromTexturePack(self, path);
            if (image != null) return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        } catch (java.io.IOException | RuntimeException ignored) {
            // fall through to the missing texture
        }
        int[] missing = new int[256];
        for (int i = 0; i < 256; i++) missing[i] = ((i >> 3 & 1) ^ (i >> 7 & 1)) == 0 ? 0xFF000000 : 0xFFFF00FF;
        return missing;
    }

    /**
     * {@code makeTexture(name, type, width, height, wrap, format, minFilter, magFilter, mipmap, image)}: a pixel buffer of the sprite-sheet API, filled with
     * the image. The sheet is not registered by name (the layer builds its atlases itself), so only the returned object knows its pixels.
     */
    public static Texture makeTexture(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self, String name, int type, int width, int height,
            int wrap, int format, int minFilter, int magFilter, boolean mipmap, BufferedImage image) {
        return new Texture(name, type, width, height, wrap, format, minFilter, magFilter, image);
    }

    /** {@code createEmptyTexture(name, type, width, height)}: a transparent texture of that size. */
    public static Texture createEmptyTexture(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self, String name, int type, int width, int height) {
        return new Texture(name, type, width, height, 10497, 6408, 9728, 9728, null);
    }

    /**
     * {@code refreshTextureMaps()}: the original rebuilt its sheets after a texture pack change. A no-op: the layer's atlases are the host's and reload
     * with its resource packs.
     */
    public static void refreshTextureMaps(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self) {
    }
}
