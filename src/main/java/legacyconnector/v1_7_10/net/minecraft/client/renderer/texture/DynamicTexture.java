package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager;

/**
 * A texture drawn by the mod into an ARGB array ({@link #getTextureData()}), uploaded by {@link #updateDynamicTexture()}.
 * Registered under a location through {@link TextureManager#loadTexture}, it becomes a modern dynamic texture.
 */
public class DynamicTexture implements ITextureObject {
    private final int width;
    private final int height;
    private final int[] dynamicTextureData;
    private net.minecraft.client.renderer.texture.DynamicTexture modern;

    public DynamicTexture(int width, int height) {
        this.width = width;
        this.height = height;
        this.dynamicTextureData = new int[width * height];
    }

    public DynamicTexture(java.awt.image.BufferedImage image) {
        this(image.getWidth(), image.getHeight());
        image.getRGB(0, 0, width, height, dynamicTextureData, 0, width);
        updateDynamicTexture();
    }

    public int[] getTextureData() {
        return dynamicTextureData;
    }

    /** The modern texture, created on first use (needs the render thread). */
    public synchronized net.minecraft.client.renderer.texture.DynamicTexture mcc$modern() {
        if (modern == null) modern = new Modern(new NativeImage(width, height, true));
        return modern;
    }

    public void updateDynamicTexture() {
        NativeImage image = mcc$modern().getPixels();
        if (image == null) return;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = dynamicTextureData[y * width + x];
                int abgr = argb & 0xFF00FF00 | (argb >> 16 & 255) | (argb & 255) << 16;
                image.setPixelRGBA(x, y, abgr);
            }
        }
        mcc$modern().upload();
    }

    @Override
    public void loadTexture(IResourceManager manager) throws IOException {
    }

    @Override
    public int getGlTextureId() {
        return mcc$modern().getId();
    }

    /** The modern texture: keeps a linear magnification filter the legacy code set, which the modern renderer's draws reset. */
    public static final class Modern extends net.minecraft.client.renderer.texture.DynamicTexture {
        public volatile boolean mcc$linear;

        Modern(NativeImage image) {
            super(image);
        }

        @Override
        public void setFilter(boolean blur, boolean mipmap) {
            super.setFilter(blur || mcc$linear, mipmap);
        }
    }
}
