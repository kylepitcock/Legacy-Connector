package legacyconnector.v1_7_10.org.lwjgl.opengl;

/** LWJGL 2's context format request (bit depths, samples); the window itself is the host's, so the values are only kept for queries. */
public final class PixelFormat {
    private int bpp;
    private int alpha;
    private int depth = 8;
    private int stencil;
    private int samples;

    public PixelFormat() {
    }

    public PixelFormat(int bpp, int alpha, int depth) {
        this(bpp, alpha, depth, 0, 0);
    }

    public PixelFormat(int bpp, int alpha, int depth, int stencil, int samples) {
        this.bpp = bpp;
        this.alpha = alpha;
        this.depth = depth;
        this.stencil = stencil;
        this.samples = samples;
    }

    public PixelFormat withBitsPerPixel(int bpp) {
        return new PixelFormat(bpp, alpha, depth, stencil, samples);
    }

    public PixelFormat withAlphaBits(int alpha) {
        return new PixelFormat(bpp, alpha, depth, stencil, samples);
    }

    public PixelFormat withDepthBits(int depth) {
        return new PixelFormat(bpp, alpha, depth, stencil, samples);
    }

    public PixelFormat withStencilBits(int stencil) {
        return new PixelFormat(bpp, alpha, depth, stencil, samples);
    }

    public PixelFormat withSamples(int samples) {
        return new PixelFormat(bpp, alpha, depth, stencil, samples);
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    public int getAlphaBits() {
        return alpha;
    }

    public int getDepthBits() {
        return depth;
    }

    public int getStencilBits() {
        return stencil;
    }

    public int getSamples() {
        return samples;
    }
}
