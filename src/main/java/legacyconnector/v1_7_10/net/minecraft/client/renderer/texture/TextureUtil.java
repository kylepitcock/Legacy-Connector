package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11C;

/** Texture upload helpers that mods use for textures they build themselves. All calls must be made on the render thread. */
public class TextureUtil {
    public static final DynamicTexture missingTexture = new DynamicTexture(16, 16);

    public static int glGenTextures() {
        return GL11C.glGenTextures();
    }

    /** Reads a texture file of the resource manager as ARGB pixels (row by row). */
    public static int[] readImageData(legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager manager,
            legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation location) throws java.io.IOException {
        BufferedImage image;
        try (java.io.InputStream in = manager.getResource(location).getInputStream()) {
            image = javax.imageio.ImageIO.read(in);
        }
        if (image == null) throw new java.io.IOException("Not an image: " + location);
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        return pixels;
    }

    /** Flips an image's rows upside down in place ({@code width} by {@code height} pixels). */
    public static void func_147953_a(int[] pixels, int width, int height) {
        int[] row = new int[width];
        for (int y = 0; y < height / 2; y++) {
            System.arraycopy(pixels, y * width, row, 0, width);
            System.arraycopy(pixels, (height - 1 - y) * width, pixels, y * width, width);
            System.arraycopy(row, 0, pixels, (height - 1 - y) * width, width);
        }
    }

    /**
     * Fills in the missing mipmap levels ({@code levels[i] == null}) up to {@code maxLevel} by halving the level above, as
     * 1.7.10 did: images with transparent pixels are averaged in linear space and pixels below 96 alpha cleared.
     */
    public static int[][] generateMipmapData(int maxLevel, int width, int[][] levels) {
        int[][] out = new int[maxLevel + 1][];
        out[0] = levels[0];
        if (maxLevel <= 0) return out;
        boolean transparent = false;
        for (int p : levels[0]) {
            if (p >> 24 == 0) {
                transparent = true;
                break;
            }
        }
        for (int level = 1; level <= maxLevel; level++) {
            if (level < levels.length && levels[level] != null) {
                out[level] = levels[level];
                continue;
            }
            int[] above = out[level - 1];
            int[] mip = new int[above.length >> 2];
            int w = width >> level;
            int h = w == 0 ? 0 : mip.length / w;
            int stride = w << 1;
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int i = 2 * (x + y * stride);
                    mip[x + y * w] = blend(above[i], above[i + 1], above[i + stride], above[i + 1 + stride], transparent);
                }
            }
            out[level] = mip;
        }
        return out;
    }

    private static int blend(int a, int b, int c, int d, boolean transparent) {
        if (!transparent) {
            return channel(a, b, c, d, 24) << 24 | channel(a, b, c, d, 16) << 16 | channel(a, b, c, d, 8) << 8 | channel(a, b, c, d, 0);
        }
        float fa = 0.0F, fr = 0.0F, fg = 0.0F, fb = 0.0F;
        for (int p : new int[] {a, b, c, d}) {
            if (p >> 24 == 0) continue;
            fa += (float) Math.pow((p >> 24 & 255) / 255.0F, 2.2D);
            fr += (float) Math.pow((p >> 16 & 255) / 255.0F, 2.2D);
            fg += (float) Math.pow((p >> 8 & 255) / 255.0F, 2.2D);
            fb += (float) Math.pow((p & 255) / 255.0F, 2.2D);
        }
        int alpha = (int) (Math.pow(fa / 4.0F, 1.0D / 2.2D) * 255.0D);
        int red = (int) (Math.pow(fr / 4.0F, 1.0D / 2.2D) * 255.0D);
        int green = (int) (Math.pow(fg / 4.0F, 1.0D / 2.2D) * 255.0D);
        int blue = (int) (Math.pow(fb / 4.0F, 1.0D / 2.2D) * 255.0D);
        if (alpha < 96) alpha = 0;
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    /** The gamma-correct average of one channel of four pixels. */
    private static int channel(int a, int b, int c, int d, int shift) {
        float fa = (float) Math.pow((a >> shift & 255) / 255.0F, 2.2D);
        float fb = (float) Math.pow((b >> shift & 255) / 255.0F, 2.2D);
        float fc = (float) Math.pow((c >> shift & 255) / 255.0F, 2.2D);
        float fd = (float) Math.pow((d >> shift & 255) / 255.0F, 2.2D);
        return (int) (Math.pow((fa + fb + fc + fd) * 0.25D, 1.0D / 2.2D) * 255.0D);
    }

    /**
     * Pads a {@code width} x {@code height} image in place by {@code border} pixels on every side with copies of the image
     * (the array must already be sized for the padded image), for anisotropic filtering of atlas sprites.
     */
    public static int[] prepareAnisotropicData(int[] pixels, int width, int height, int border) {
        int stride = width + 2 * border;
        for (int y = height - 1; y >= 0; y--) {
            int src = y * width;
            int dst = border + (y + border) * stride;
            for (int x = 0; x < border; x += width) {
                int n = Math.min(width, border - x);
                System.arraycopy(pixels, src + width - n, pixels, dst - x - n, n);
            }
            System.arraycopy(pixels, src, pixels, dst, width);
            for (int x = 0; x < border; x += width) {
                System.arraycopy(pixels, src, pixels, dst + width + x, Math.min(width, border - x));
            }
        }
        for (int y = 0; y < border; y += height) {
            int n = Math.min(height, border - y);
            System.arraycopy(pixels, (border + height - n) * stride, pixels, (border - y - n) * stride, stride * n);
        }
        for (int y = 0; y < border; y += height) {
            int n = Math.min(height, border - y);
            System.arraycopy(pixels, border * stride, pixels, (height + border + y) * stride, stride * n);
        }
        return pixels;
    }

    public static void deleteTexture(int id) {
        GL11C.glDeleteTextures(id);
    }

    public static int uploadTextureImage(int id, BufferedImage image) {
        return uploadTextureImageAllocate(id, image, false, false);
    }

    public static int uploadTextureImageAllocate(int id, BufferedImage image, boolean blur, boolean clamp) {
        allocateTexture(id, image.getWidth(), image.getHeight());
        return uploadTextureImageSub(id, image, 0, 0, blur, clamp);
    }

    public static void allocateTexture(int id, int width, int height) {
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, id);
        GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, width, height, 0, org.lwjgl.opengl.GL12C.GL_BGRA, GL11C.GL_UNSIGNED_BYTE, (IntBuffer) null);
    }

    public static int uploadTextureImageSub(int id, BufferedImage image, int x, int y, boolean blur, boolean clamp) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        uploadTextureSub(id, pixels, width, height, x, y, blur, clamp);
        return id;
    }

    public static void uploadTexture(int id, int[] pixels, int width, int height) {
        allocateTexture(id, width, height);
        uploadTextureSub(id, pixels, width, height, 0, 0, false, false);
    }

    public static void uploadTextureSub(int id, int[] pixels, int width, int height, int x, int y, boolean blur, boolean clamp) {
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, id);
        IntBuffer buffer = org.kylepitcock.legacyconnector.v1_7_10.client.GlPixels.ints(pixels);
        GL11C.glPixelStorei(GL11C.GL_UNPACK_ROW_LENGTH, 0);
        GL11C.glTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, x, y, width, height, org.lwjgl.opengl.GL12C.GL_BGRA, GL11C.GL_UNSIGNED_BYTE, buffer);
        int filter = blur ? GL11C.GL_LINEAR : GL11C.GL_NEAREST;
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, filter);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, filter);
        int wrap = clamp ? 33071 : GL11C.GL_REPEAT;
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, wrap);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, wrap);
    }

    /** Uploads precomputed mipmap levels ({@code levels[0]} is the full size image). */
    public static void uploadTextureMipmap(int[][] levels, int width, int height, int x, int y, boolean blur, boolean clamp) {
        for (int level = 0; level < levels.length; level++) {
            int w = Math.max(1, width >> level);
            int h = Math.max(1, height >> level);
            IntBuffer buffer = org.kylepitcock.legacyconnector.v1_7_10.client.GlPixels.ints(levels[level]);
            GL11C.glTexSubImage2D(GL11C.GL_TEXTURE_2D, level, x >> level, y >> level, w, h, org.lwjgl.opengl.GL12C.GL_BGRA, GL11C.GL_UNSIGNED_BYTE, buffer);
        }
    }

    /** Resets the filter state a mod changed with {@link #func_152777_a}; nothing to restore here. */
    public static void func_147945_b() {
    }

    public static void func_152777_a(boolean blur, boolean mipmap, float anisotropy) {
    }
}
