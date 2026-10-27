package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * 1.12.2 texture upload helpers: the 1.7.10 shim's uploads (allocate, sub-image, mipmaps), plus the names and helpers 1.12.2 added.
 * All GL calls must be made on the render thread.
 */
public class TextureUtil extends legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureUtil {
    public static final DynamicTexture MISSING_TEXTURE = new DynamicTexture(16, 16);
    public static final int[] MISSING_TEXTURE_DATA = MISSING_TEXTURE.getTextureData();
    private static final int[] MIPMAP_BUFFER = new int[4];

    static {
        int black = -16777216, magenta = -524040;
        int[] a = {magenta, magenta, magenta, magenta, magenta, magenta, magenta, magenta};
        int[] b = {black, black, black, black, black, black, black, black};
        int len = a.length;
        for (int i = 0; i < 16; ++i) {
            System.arraycopy(i < 8 ? a : b, 0, MISSING_TEXTURE_DATA, 16 * i, len);
            System.arraycopy(i < 8 ? b : a, 0, MISSING_TEXTURE_DATA, 16 * i + len, len);
        }
    }

    /** Reads an image file from the client's resources (the modern resource manager: every pack the game has loaded). */
    public static int[] readImageData(IResourceManager resourceManager, ResourceLocation imageLocation) throws IOException {
        BufferedImage image;
        try (InputStream in = open(imageLocation)) {
            image = readBufferedImage(in);
        }
        if (image == null) throw new IOException("Not an image: " + imageLocation);
        int w = image.getWidth(), h = image.getHeight();
        int[] pixels = new int[w * h];
        image.getRGB(0, 0, w, h, pixels, 0, w);
        return pixels;
    }

    /** Opens a resource of the client's packs, for code that read through the legacy resource manager. */
    public static InputStream open(ResourceLocation location) throws IOException {
        var rl = org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations.modern(location.getNamespace(), location.getPath());
        var resource = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(rl);
        if (resource.isEmpty()) throw new java.io.FileNotFoundException(location.toString());
        return resource.get().open();
    }

    public static BufferedImage readBufferedImage(InputStream imageStream) throws IOException {
        if (imageStream == null) return null;
        try (imageStream) {
            return javax.imageio.ImageIO.read(imageStream);
        }
    }

    public static void allocateTextureImpl(int glTextureId, int mipmapLevels, int width, int height) {
        deleteTexture(glTextureId);
        bindTexture(glTextureId);
        if (mipmapLevels >= 0) {
            org.lwjgl.opengl.GL11C.glTexParameteri(3553, 33085, mipmapLevels);
            org.lwjgl.opengl.GL11C.glTexParameteri(3553, 33082, 0);
            org.lwjgl.opengl.GL11C.glTexParameteri(3553, 33083, mipmapLevels);
            org.lwjgl.opengl.GL11C.glTexParameterf(3553, 34049, 0.0F);
        }
        for (int i = 0; i <= mipmapLevels; ++i) {
            org.lwjgl.opengl.GL11C.glTexImage2D(3553, i, 6408, width >> i, height >> i, 0, 32993, 33639, (java.nio.IntBuffer) null);
        }
    }

    public static void bindTexture(int textureId) {
        legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.bindTexture(textureId);
    }

    /** Anaglyph 3D was removed; colours stay as they are (vanilla only changed them with the option on). */
    public static int anaglyphColor(int color) {
        return color;
    }

    public static void processPixelValues(int[] pixel, int width, int height) {
        int[] row = new int[width];
        int k = height / 2;
        for (int l = 0; l < k; ++l) {
            System.arraycopy(pixel, l * width, row, 0, width);
            System.arraycopy(pixel, (height - 1 - l) * width, pixel, l * width, width);
            System.arraycopy(row, 0, pixel, (height - 1 - l) * width, width);
        }
    }

    public static void uploadTextureMipmap(int[][] textureData, int width, int height, int originX, int originY, boolean linear, boolean clamp) {
        legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureUtil.uploadTextureMipmap(textureData, width, height, originX, originY, linear, clamp);
    }

    public static int[][] generateMipmapData(int mipmapLevels, int width, int[][] levels) {
        return legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureUtil.generateMipmapData(mipmapLevels, width, levels);
    }

    public static int glGenTextures() {
        return com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread() ? legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureUtil.glGenTextures() : 0;
    }
}
