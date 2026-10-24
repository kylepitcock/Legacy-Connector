package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11C;

/** Texture upload helpers that mods use for textures they build themselves. All calls must be made on the render thread. */
public class TextureUtil {
    public static final DynamicTexture missingTexture = new DynamicTexture(16, 16);

    public static int glGenTextures() {
        return GL11C.glGenTextures();
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
        IntBuffer buffer = BufferUtils.createIntBuffer(pixels.length);
        buffer.put(pixels).flip();
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
            IntBuffer buffer = BufferUtils.createIntBuffer(levels[level].length);
            buffer.put(levels[level]).flip();
            GL11C.glTexSubImage2D(GL11C.GL_TEXTURE_2D, level, x >> level, y >> level, w, h, org.lwjgl.opengl.GL12C.GL_BGRA, GL11C.GL_UNSIGNED_BYTE, buffer);
        }
    }

    /** Resets the filter state a mod changed with {@link #func_152777_a}; nothing to restore here. */
    public static void func_147945_b() {
    }

    public static void func_152777_a(boolean blur, boolean mipmap, float anisotropy) {
    }
}
