package legacyconnector.v1_12_2.org.lwjgl.input;

import java.nio.IntBuffer;

/** LWJGL 2 native cursor: kept as data (the modern window draws its own cursor; see {@link Mouse#setNativeCursor}). */
public class Cursor {
    public static final int CURSOR_ONE_BIT_TRANSPARENCY = 1, CURSOR_8_BIT_ALPHA = 2, CURSOR_ANIMATION = 4;

    private final int width, height, xHotspot, yHotspot, images;
    private boolean destroyed;

    public Cursor(int width, int height, int xHotspot, int yHotspot, int numImages, IntBuffer images, IntBuffer delays) {
        this.width = width;
        this.height = height;
        this.xHotspot = xHotspot;
        this.yHotspot = yHotspot;
        this.images = numImages;
    }

    public static int getMinCursorSize() {
        return 1;
    }

    public static int getMaxCursorSize() {
        return 512;
    }

    public static int getCapabilities() {
        return CURSOR_ONE_BIT_TRANSPARENCY | CURSOR_8_BIT_ALPHA | CURSOR_ANIMATION;
    }

    public void destroy() {
        destroyed = true;
    }

    public boolean mcc$destroyed() {
        return destroyed;
    }
}
