package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.Sprites1122;

/**
 * A 1.12.2 atlas sprite. The modern block atlas does the stitching: once it is built, every legacy sprite takes its position, size and
 * uv range from the modern sprite of the same name ({@link #mcc$sync}), and its first frame's pixels for code that reads them.
 */
public class TextureAtlasSprite {
    private ResourceLocation[] dependencies = new ResourceLocation[0];
    protected final String iconName;
    protected List<int[][]> framesTextureData = Lists.newArrayList();
    protected int[][] interpolatedFrameData;
    protected boolean rotated;
    protected int originX;
    protected int originY;
    protected int width;
    protected int height;
    private float minU;
    private float maxU;
    private float minV;
    private float maxV;
    protected int frameCounter;
    protected int tickCounter;
    /** The modern sprite this one was last synchronized with (null: not stitched yet). */
    private net.minecraft.client.renderer.texture.TextureAtlasSprite mcc$modern;

    public TextureAtlasSprite(String spriteName) {
        iconName = spriteName;
    }

    protected static TextureAtlasSprite makeAtlasSprite(ResourceLocation spriteResourceLocation) {
        return new TextureAtlasSprite(spriteResourceLocation.toString());
    }

    public void initSprite(int inX, int inY, int originInX, int originInY, boolean rotatedIn) {
        originX = originInX;
        originY = originInY;
        rotated = rotatedIn;
        float f = (float) (0.009999999776482582D / inX);
        float f1 = (float) (0.009999999776482582D / inY);
        minU = (float) originInX / (float) inX + f;
        maxU = (float) (originInX + width) / (float) inX - f;
        minV = (float) originInY / (float) inY + f1;
        maxV = (float) (originInY + height) / (float) inY - f1;
    }

    public void copyFrom(TextureAtlasSprite atlasSpirit) {
        originX = atlasSpirit.originX;
        originY = atlasSpirit.originY;
        width = atlasSpirit.width;
        height = atlasSpirit.height;
        rotated = atlasSpirit.rotated;
        minU = atlasSpirit.minU;
        maxU = atlasSpirit.maxU;
        minV = atlasSpirit.minV;
        maxV = atlasSpirit.maxV;
    }

    /**
     * Takes position, size, uvs and pixels from the modern sprite of this name in the block atlas (missing sprite: the missing
     * texture's). Called after each atlas build for the sprites legacy code holds.
     */
    public void mcc$sync(net.minecraft.client.renderer.texture.TextureAtlasSprite modern) {
        if (modern == null) return;
        mcc$modern = modern;
        var contents = modern.contents();
        width = contents.width();
        height = contents.height();
        originX = modern.getX();
        originY = modern.getY();
        rotated = false;
        minU = modern.getU0();
        maxU = modern.getU1();
        minV = modern.getV0();
        maxV = modern.getV1();
        framesTextureData = Lists.newArrayList();
        try {
            var image = contents.getOriginalImage();
            if (image != null) {
                int frames = Math.max(1, image.getHeight() / Math.max(1, height));
                int w = Math.min(width, image.getWidth());
                for (int frame = 0; frame < frames; frame++) {
                    int[] pixels = new int[width * height];
                    for (int y = 0; y < height && frame * height + y < image.getHeight(); y++) {
                        for (int x = 0; x < w; x++) {
                            int abgr = image.getPixelRGBA(x, frame * height + y);
                            pixels[y * width + x] = abgr & 0xFF00FF00 | (abgr >> 16 & 255) | (abgr & 255) << 16;
                        }
                    }
                    framesTextureData.add(new int[][] {pixels});
                }
            }
        } catch (RuntimeException e) {
            // pixels not available (the image was released); the sprite still has its position
        }
    }

    /** The modern sprite this one shows (stitched lazily if legacy code asks before the atlas reload told it). */
    public net.minecraft.client.renderer.texture.TextureAtlasSprite mcc$modern() {
        if (mcc$modern == null) mcc$sync(Sprites1122.sprite(iconName));
        return mcc$modern;
    }

    /** The modern sprite last synchronized with, without looking one up. */
    public net.minecraft.client.renderer.texture.TextureAtlasSprite mcc$modernIfSynced() {
        return mcc$modern;
    }

    private void ensure() {
        if (mcc$modern == null && maxU == 0.0F && maxV == 0.0F) mcc$sync(Sprites1122.sprite(iconName));
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getIconWidth() {
        ensure();
        return width;
    }

    public int getIconHeight() {
        ensure();
        return height;
    }

    public float getMinU() {
        ensure();
        return minU;
    }

    public float getMaxU() {
        ensure();
        return maxU;
    }

    public float getInterpolatedU(double u) {
        float f = getMaxU() - minU;
        return minU + f * (float) u / 16.0F;
    }

    public float getUnInterpolatedU(float u) {
        float f = getMaxU() - minU;
        return (u - minU) / f * 16.0F;
    }

    public float getMinV() {
        ensure();
        return minV;
    }

    public float getMaxV() {
        ensure();
        return maxV;
    }

    public float getInterpolatedV(double v) {
        float f = getMaxV() - minV;
        return minV + f * (float) v / 16.0F;
    }

    public float getUnInterpolatedV(float p_188536_1_) {
        float f = getMaxV() - minV;
        return (p_188536_1_ - minV) / f * 16.0F;
    }

    public String getIconName() {
        return iconName;
    }

    /** Animation is the modern atlas's (it ticks the stitched sprites itself). */
    public void updateAnimation() {
    }

    public void setIconWidth(int newWidth) {
        width = newWidth;
    }

    public void setIconHeight(int newHeight) {
        height = newHeight;
    }

    /**
     * Legacy sprite loading from a PNG: the modern atlas loads the texture file of this sprite's name itself, so only the size is kept.
     */
    public void loadSprite(PngSizeInfo sizeInfo, boolean animated) {
        resetSprite();
        width = sizeInfo.pngWidth;
        height = animated ? width : sizeInfo.pngHeight;
    }

    public void generateMipmaps(int level) {
        List<int[][]> list = Lists.newArrayList();
        for (int i = 0; i < framesTextureData.size(); ++i) {
            int[][] frame = framesTextureData.get(i);
            if (frame != null) {
                try {
                    list.add(TextureUtil.generateMipmapData(level, width, frame));
                } catch (Throwable t) {
                    list.add(frame);
                }
            }
        }
        setFramesTextureData(list);
    }

    protected void allocateFrameTextureData(int index) {
        if (framesTextureData.size() <= index) {
            for (int i = framesTextureData.size(); i <= index; ++i) framesTextureData.add(null);
        }
    }

    protected static int[][] getFrameTextureData(int[][] data, int rows, int columns, int p_147962_3_) {
        int[][] out = new int[data.length][];
        for (int i = 0; i < data.length; ++i) {
            int[] level = data[i];
            if (level != null) {
                out[i] = new int[(rows >> i) * (columns >> i)];
                System.arraycopy(level, p_147962_3_ * out[i].length, out[i], 0, out[i].length);
            }
        }
        return out;
    }

    /**
     * Reads a sprite's frames from its PNG (a vertical strip of square frames is split into its frames), level 0 only: the mipmap levels
     * are filled by {@link #generateMipmaps}.
     */
    public void loadSpriteFrames(legacyconnector.v1_12_2.net.minecraft.client.resources.IResource resource, int mipmaplevels) throws java.io.IOException {
        java.awt.image.BufferedImage image;
        try (java.io.InputStream in = resource.getInputStream()) {
            image = TextureUtil.readBufferedImage(in);
        }
        if (image == null) throw new java.io.IOException("Couldn't read sprite " + iconName);
        int w = image.getWidth(), h = image.getHeight();
        int[] pixels = new int[w * h];
        image.getRGB(0, 0, w, h, pixels, 0, w);
        int levels = Math.max(1, mipmaplevels);
        if (h > w && h % w == 0) {
            int frame = w * w;
            for (int i = 0; i < h / w; i++) {
                int[][] data = new int[levels][];
                data[0] = new int[frame];
                System.arraycopy(pixels, i * frame, data[0], 0, frame);
                framesTextureData.add(data);
            }
        } else {
            int[][] data = new int[levels][];
            data[0] = pixels;
            framesTextureData.add(data);
        }
    }

    /** One channel blended between two values (animation interpolation). */
    protected int interpolateColor(double factor, int to, int from) {
        return (int) (factor * (double) to + (1.0D - factor) * (double) from);
    }

    public void clearFramesTextureData() {
        framesTextureData.clear();
    }

    public boolean hasAnimationMetadata() {
        net.minecraft.client.renderer.texture.TextureAtlasSprite m = mcc$modern();
        return m != null && m.contents().getUniqueFrames().count() > 1;
    }

    public void setFramesTextureData(List<int[][]> newFramesTextureData) {
        framesTextureData = newFramesTextureData;
    }

    protected void resetSprite() {
        framesTextureData = Lists.newArrayList();
        frameCounter = 0;
        tickCounter = 0;
    }

    public String toString() {
        return "TextureAtlasSprite{name='" + iconName + "', frameCount=" + framesTextureData.size() + ", rotated=" + rotated + ", x=" + originX + ", y="
                + originY + ", height=" + height + ", width=" + width + ", u0=" + minU + ", u1=" + maxU + ", v0=" + minV + ", v1=" + maxV + '}';
    }

    public int[][] getFrameTextureData(int index) {
        ensure();
        return index >= 0 && index < framesTextureData.size() ? framesTextureData.get(index) : null;
    }

    public int getFrameCount() {
        ensure();
        return framesTextureData.size();
    }

    // ------------------------------------------------------------------ Forge

    /** Custom sprite loaders cannot feed the modern atlas: such sprites show the texture file of their name. */
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return false;
    }

    public boolean load(IResourceManager manager, ResourceLocation location, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
        return true;
    }

    public Collection<ResourceLocation> getDependencies() {
        return Collections.unmodifiableList(java.util.Arrays.asList(dependencies));
    }

    public int getPixelRGBA(int frameIndex, int x, int y) {
        net.minecraft.client.renderer.texture.TextureAtlasSprite m = mcc$modern();
        return m == null ? 0 : m.getPixelRGBA(frameIndex, x, y);
    }
}
