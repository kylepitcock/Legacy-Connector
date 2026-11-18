package legacyconnector.v1_7_10.org.lwjgl.input;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import legacyconnector.v1_7_10.org.lwjgl.LWJGLException;

/**
 * LWJGL 2's native mouse cursor, made of one image (ARGB ints, origin at the lower left like OpenGL) and a hotspot. It becomes a GLFW cursor
 * the first time {@link Mouse#setNativeCursor} shows it (GLFW cursors are made on the render thread; mods build theirs while loading, on
 * another one). Animated cursors show their first image: GLFW has no animated cursors.
 */
public class Cursor {
    public static final int CURSOR_ONE_BIT_TRANSPARENCY = 1;
    public static final int CURSOR_8_BIT_ALPHA = 2;
    public static final int CURSOR_ANIMATION = 4;

    private final int width;
    private final int height;
    private final int xHotspot;
    private final int yHotspot;
    /** The first image as RGBA bytes, top row first (GLFW's layout). */
    private final byte[] rgba;
    private long handle;
    private boolean destroyed;

    public Cursor(int width, int height, int xHotspot, int yHotspot, int numImages, IntBuffer images, IntBuffer delays) throws LWJGLException {
        if (width <= 0 || height <= 0 || numImages <= 0 || images == null) throw new LWJGLException("Invalid cursor size or images");
        if (images.remaining() < width * height) throw new LWJGLException("Not enough cursor image data: " + images.remaining() + " < " + width * height);
        this.width = width;
        this.height = height;
        this.xHotspot = Math.max(0, Math.min(width - 1, xHotspot));
        // the hotspot is counted from the bottom row
        this.yHotspot = Math.max(0, Math.min(height - 1, height - 1 - yHotspot));
        this.rgba = new byte[width * height * 4];
        int base = images.position();
        for (int row = 0; row < height; row++) {
            int source = base + (height - 1 - row) * width;
            for (int col = 0; col < width; col++) {
                int argb = images.get(source + col);
                int at = (row * width + col) * 4;
                rgba[at] = (byte) (argb >> 16);
                rgba[at + 1] = (byte) (argb >> 8);
                rgba[at + 2] = (byte) argb;
                rgba[at + 3] = (byte) (argb >>> 24);
            }
        }
    }

    /** What the system supports: colour cursors with 8 bit alpha (GLFW). */
    public static int getCapabilities() {
        return CURSOR_ONE_BIT_TRANSPARENCY | CURSOR_8_BIT_ALPHA;
    }

    public static int getMinCursorSize() {
        return 1;
    }

    /** The size the system draws without scaling (32 on Windows, as LWJGL 2 reported it). */
    public static int getMaxCursorSize() {
        return 32;
    }

    /** The GLFW cursor, made on first use; 0 when GLFW refused the image. Render thread only. */
    long mcc$handle() {
        if (handle == 0 && !destroyed) {
            org.lwjgl.glfw.GLFWImage image = org.lwjgl.glfw.GLFWImage.malloc();
            ByteBuffer pixels = org.lwjgl.system.MemoryUtil.memAlloc(rgba.length);
            try {
                pixels.put(rgba).flip();
                image.set(width, height, pixels);
                handle = org.lwjgl.glfw.GLFW.glfwCreateCursor(image, xHotspot, yHotspot);
            } finally {
                org.lwjgl.system.MemoryUtil.memFree(pixels);
                image.free();
            }
        }
        return handle;
    }

    public void setTimeout(int timeout) {
    }

    public int getTimeout() {
        return 0;
    }

    public boolean isActive() {
        return Mouse.mcc$currentCursor() == this;
    }

    public void destroy() {
        try {
            if (Mouse.mcc$currentCursor() == this) Mouse.setNativeCursor(null);
        } catch (LWJGLException ignored) {
        }
        final long dead = handle;
        handle = 0;
        destroyed = true;
        if (dead != 0) net.minecraft.client.Minecraft.getInstance().execute(() -> org.lwjgl.glfw.GLFW.glfwDestroyCursor(dead));
    }
}
