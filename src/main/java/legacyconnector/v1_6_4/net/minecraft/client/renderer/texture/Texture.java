package legacyconnector.v1_6_4.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * Minecraft 1.5.2's {@code Texture}: a pixel buffer of a given size that mods build their own sprite sheets and animation frames from
 * ({@code TextureStitched.init} takes the sheet and the frames as these). The pixels are kept as RGBA bytes like the original; the GL texture is made
 * on the first {@link #getGlTextureId()} or {@link #bindTexture(int)} (through the texture manager's handle table, so it can be asked for while mods load).
 * Filtering and wrapping parameters are remembered but the layer's texture manager picks its own.
 */
public class Texture {
    private final String textureName;
    private final int textureType;
    private final int width;
    private final int height;
    private final int textureDepth;
    private final int textureWrap;
    private final int textureFormat;
    private final int textureMinFilter;
    private final int textureMagFilter;
    private final Rect2i textureRect;
    private ByteBuffer textureData;
    private int textureId = -1;
    private int glTextureId = -1;
    private boolean transferred;
    private boolean dirty = true;

    private static int nextId;

    public Texture(String name, int type, int width, int height, int wrap, int format, int minFilter, int magFilter, BufferedImage image) {
        this(name, type, width, height, 1, wrap, format, minFilter, magFilter, image);
    }

    public Texture(String name, int type, int width, int height, int depth, int wrap, int format, int minFilter, int magFilter, BufferedImage image) {
        this.textureName = name;
        this.textureType = type;
        this.width = Math.max(width, 0);
        this.height = Math.max(height, 0);
        this.textureDepth = depth;
        this.textureWrap = wrap;
        this.textureFormat = format;
        this.textureMinFilter = minFilter;
        this.textureMagFilter = magFilter;
        this.textureRect = new Rect2i(0, 0, this.width, this.height);
        this.textureData = ByteBuffer.allocateDirect(Math.max(this.width * this.height * 4, 0));
        synchronized (Texture.class) {
            this.textureId = nextId++;
        }
        if (image != null) transferFromImage(image);
    }

    public Rect2i getTextureRect() {
        return textureRect;
    }

    /** Fills a rectangle of the texture with an ARGB colour (clipped to the texture). */
    public void fillRect(Rect2i rect, int argb) {
        if (rect == null) return;
        int x0 = Math.max(rect.getRectX(), 0);
        int y0 = Math.max(rect.getRectY(), 0);
        int x1 = Math.min(rect.getRectX() + rect.getRectWidth(), width);
        int y1 = Math.min(rect.getRectY() + rect.getRectHeight(), height);
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) setPixel(x, y, argb);
        }
        markDirty();
    }

    /** Writes the texture as a PNG file (a debugging aid of the original). */
    public void writeImage(String path) {
        try {
            javax.imageio.ImageIO.write(toImage(), "png", new java.io.File(path));
        } catch (java.io.IOException | RuntimeException ignored) {
            // a debugging aid: failing to write is not an error of the mod
        }
    }

    /**
     * Copies another texture into this one with its top left corner at (x, y); {@code rotated} writes it turned a quarter (the stitcher rotates sprites
     * that fit a slot only that way). Pixels outside this texture are dropped.
     */
    public void copyFrom(int x, int y, Texture source, boolean rotated) {
        if (source == null) return;
        for (int sy = 0; sy < source.height; sy++) {
            for (int sx = 0; sx < source.width; sx++) {
                int dx = rotated ? x + sy : x + sx;
                int dy = rotated ? y + sx : y + sy;
                if (dx < 0 || dy < 0 || dx >= width || dy >= height) continue;
                setPixel(dx, dy, source.getPixel(sx, sy));
            }
        }
        markDirty();
    }

    /** Copies another texture in without the alpha blend of the original's second variant (1.5.2.x helper). */
    public void func_104062_b(int x, int y, Texture source) {
        copyFrom(x, y, source, false);
    }

    /** Replaces the pixels with those of an image (scaled to this texture's size is not done: the image is clipped or left transparent). */
    public void transferFromImage(BufferedImage image) {
        if (image == null) return;
        int w = Math.min(image.getWidth(), width);
        int h = Math.min(image.getHeight(), height);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) setPixel(x, y, image.getRGB(x, y));
        }
        transferred = true;
        markDirty();
    }

    public int getTextureId() {
        return textureId;
    }

    /** The GL id (a handle the texture manager resolves when it is bound); made on first use. */
    public synchronized int getGlTextureId() {
        if (glTextureId < 0 || dirty) {
            var manager = legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager.INSTANCE;
            if (glTextureId >= 0) manager.mcc$replaceImage(glTextureId, toImage());
            else glTextureId = manager.allocateAndSetupTexture(toImage());
            dirty = false;
        }
        return glTextureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getTextureName() {
        return textureName;
    }

    /** Binds the texture ({@code unit} is the texture unit the original activated; the layer binds the one the render context uses). */
    public void bindTexture(int unit) {
        legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager.INSTANCE.bindTexture(getGlTextureId());
    }

    /** Uploads the pixels now (a no-op until something binds the texture: the upload happens there). */
    public void uploadTexture() {
        getGlTextureId();
    }

    /** The RGBA bytes (width x height x 4, row by row); writes to the buffer are seen by {@link #getGlTextureId()} after {@link #markDirty()}. */
    public ByteBuffer getTextureData() {
        return textureData;
    }

    /** Tells the texture that its data buffer was written to, so the GL copy is made again. */
    public synchronized void markDirty() {
        dirty = true;
    }

    public boolean isTransferred() {
        return transferred;
    }

    public int getTextureType() {
        return textureType;
    }

    public int getTextureDepth() {
        return textureDepth;
    }

    public int getTextureWrap() {
        return textureWrap;
    }

    public int getTextureFormat() {
        return textureFormat;
    }

    public int getTextureMinFilter() {
        return textureMinFilter;
    }

    public int getTextureMagFilter() {
        return textureMagFilter;
    }

    /** One pixel as ARGB. */
    public int getPixel(int x, int y) {
        int i = (y * width + x) * 4;
        return (textureData.get(i + 3) & 255) << 24 | (textureData.get(i) & 255) << 16 | (textureData.get(i + 1) & 255) << 8 | (textureData.get(i + 2) & 255);
    }

    private void setPixel(int x, int y, int argb) {
        int i = (y * width + x) * 4;
        textureData.put(i, (byte) (argb >> 16));
        textureData.put(i + 1, (byte) (argb >> 8));
        textureData.put(i + 2, (byte) argb);
        textureData.put(i + 3, (byte) (argb >>> 24));
    }

    /** The pixels as an image (what the layer's texture manager uploads). */
    public BufferedImage toImage() {
        BufferedImage image = new BufferedImage(Math.max(width, 1), Math.max(height, 1), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) image.setRGB(x, y, getPixel(x, y));
        }
        return image;
    }

    @Override
    public String toString() {
        return "Texture{" + textureName + " " + width + "x" + height + "}";
    }
}
