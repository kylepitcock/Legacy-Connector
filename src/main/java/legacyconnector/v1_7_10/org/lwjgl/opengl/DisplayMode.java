package legacyconnector.v1_7_10.org.lwjgl.opengl;

/** LWJGL 2's display mode value. */
public final class DisplayMode {
    private final int width;
    private final int height;
    private final int bpp;
    private final int freq;
    private final boolean fullscreenCapable;

    public DisplayMode(int width, int height) {
        this(width, height, 32, 60, false);
    }

    public DisplayMode(int width, int height, int bpp, int freq) {
        this(width, height, bpp, freq, true);
    }

    private DisplayMode(int width, int height, int bpp, int freq, boolean fullscreenCapable) {
        this.width = width;
        this.height = height;
        this.bpp = bpp;
        this.freq = freq;
        this.fullscreenCapable = fullscreenCapable;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    public int getFrequency() {
        return freq;
    }

    public boolean isFullscreenCapable() {
        return fullscreenCapable;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DisplayMode m && m.width == width && m.height == height && m.bpp == bpp && m.freq == freq;
    }

    @Override
    public int hashCode() {
        return width ^ height ^ bpp ^ freq;
    }

    @Override
    public String toString() {
        return width + " x " + height + " x " + bpp + " @" + freq + "Hz";
    }
}
