package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_7_10.net.minecraft.client.resources.data.AnimationMetadataSection;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.AtlasUV;

/**
 * An icon in the block/item atlas. Registered sprites name a texture the pack provides; sprites with a custom
 * loader ({@link #hasCustomLoader}, e.g. Railcraft's unstitched sprite sheets) have their pixels produced by the mod's
 * {@link #load} when the resource pack is generated, and are written as ordinary textures (see LegacySprites).
 */
public class TextureAtlasSprite implements IIcon {
    /** The modern sprite id once registered with a {@link TextureMap}. */
    protected String iconName;
    protected List framesTextureData = new ArrayList();
    protected AnimationMetadataSection animationMetadata;
    protected boolean rotated;
    protected int originX;
    protected int originY;
    protected int width;
    protected int height;
    protected float minU;
    protected float maxU;
    protected float minV;
    protected float maxV;
    protected int frameCounter;
    protected int tickCounter;
    protected boolean useAnisotropicFiltering;

    protected TextureAtlasSprite(String name) {
        this.iconName = name;
    }

    public static TextureAtlasSprite makeAtlasSprite(String name) {
        return new TextureAtlasSprite(name);
    }

    public void initSprite(int sheetWidth, int sheetHeight, int originX, int originY, boolean rotated) {
        this.originX = originX;
        this.originY = originY;
        this.rotated = rotated;
    }

    /** The sprite this one was copied from (a custom loader that finds a texture already loaded aliases it instead of loading it again). */
    public TextureAtlasSprite mcc$copySource;

    public void copyFrom(TextureAtlasSprite other) {
        this.mcc$copySource = other;
        this.originX = other.originX;
        this.originY = other.originY;
        this.width = other.width;
        this.height = other.height;
        this.rotated = other.rotated;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    /** Forge: a sprite that loads its own pixels (return true) instead of reading {@code location}. */
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return false;
    }

    /** Forge: produces {@link #framesTextureData}; returns false when it loaded the sprite (true: skip it). */
    public boolean load(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    /** Takes the frames of an already read texture (vanilla's loader hands every frame/mipmap image here). */
    /** Gives fully transparent pixels the average colour of the opaque ones, so filtering does not bleed black into edges. */
    private void fixTransparentPixels(int[][] data) {
        int[] pixels = data[0];
        float r = 0, g = 0, b = 0;
        int count = 0;
        for (int px : pixels) {
            int alpha = px >> 24 & 255;
            if (alpha > 0) {
                r += (px >> 16 & 255) * (float) alpha;
                g += (px >> 8 & 255) * (float) alpha;
                b += (px & 255) * (float) alpha;
                count += alpha;
            }
        }
        if (count == 0) return;
        int avg = (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
        for (int i = 0; i < pixels.length; i++) {
            if ((pixels[i] >> 24 & 255) == 0) pixels[i] = avg;
        }
    }

    /** Vanilla pads each frame for anisotropic filtering; the modern atlas does its own, so the data is returned as it is. */
    private int[][] prepareAnisotropicFiltering(int[][] data) {
        return data;
    }

    public void loadSprite(BufferedImage[] images, AnimationMetadataSection animation, boolean anisotropic) {
        resetSprite();
        if (images == null || images.length == 0 || images[0] == null) return;
        this.animationMetadata = animation;
        this.width = images[0].getWidth();
        this.height = images[0].getHeight();
        int levels = Math.max(1, images.length);
        int[][] frame = new int[levels][];
        for (int i = 0; i < levels; i++) {
            BufferedImage img = images[i];
            if (img == null) break;
            int[] pixels = new int[img.getWidth() * img.getHeight()];
            img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
            frame[i] = pixels;
        }
        framesTextureData.add(frame);
    }

    public void loadSprite(BufferedImage[] images, AnimationMetadataSection animation) {
        loadSprite(images, animation, false);
    }

    public void generateMipmaps(int levels) {
    }

    public void resetSprite() {
        animationMetadata = null;
        framesTextureData.clear();
        frameCounter = 0;
        tickCounter = 0;
    }

    public void clearFramesTextureData() {
        framesTextureData.clear();
    }

    public void setFramesTextureData(List frames) {
        this.framesTextureData = frames;
    }

    public int[][] getFrameTextureData(int frame) {
        return (int[][]) framesTextureData.get(frame);
    }

    public int getFrameCount() {
        return framesTextureData.size();
    }

    public boolean hasAnimationMetadata() {
        return animationMetadata != null;
    }

    public void updateAnimation() {
    }

    /** The loaded pixel size (the icon size reports the atlas size once on the client). */
    public int getIconWidth0() {
        return width;
    }

    public int getIconHeight0() {
        return height;
    }

    public void setIconWidth(int width) {
        this.width = width;
    }

    public void setIconHeight(int height) {
        this.height = height;
    }

    // ------------------------------------------------------------------ IIcon

    private float[] uv() {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? AtlasUV.of(iconName) : null;
    }

    @Override
    public int getIconWidth() {
        float[] uv = uv();
        return uv != null ? (int) uv[4] : width > 0 ? width : 16;
    }

    @Override
    public int getIconHeight() {
        float[] uv = uv();
        return uv != null ? (int) uv[5] : height > 0 ? height : 16;
    }

    @Override
    public float getMinU() {
        float[] uv = uv();
        return uv == null ? 0 : uv[0];
    }

    @Override
    public float getMaxU() {
        float[] uv = uv();
        return uv == null ? 1 : uv[1];
    }

    @Override
    public float getInterpolatedU(double u) {
        return getMinU() + (getMaxU() - getMinU()) * (float) u / 16.0F;
    }

    @Override
    public float getMinV() {
        float[] uv = uv();
        return uv == null ? 0 : uv[2];
    }

    @Override
    public float getMaxV() {
        float[] uv = uv();
        return uv == null ? 1 : uv[3];
    }

    @Override
    public float getInterpolatedV(double v) {
        return getMinV() + (getMaxV() - getMinV()) * (float) v / 16.0F;
    }

    @Override
    public String getIconName() {
        return iconName;
    }

    @Override
    public String toString() {
        return "TextureAtlasSprite{name='" + iconName + "'}";
    }
}
