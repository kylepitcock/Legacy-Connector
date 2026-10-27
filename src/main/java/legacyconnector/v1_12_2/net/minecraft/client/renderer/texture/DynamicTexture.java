package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;

/**
 * A texture the mod draws into an ARGB array ({@link #getTextureData()}) and uploads with {@link #updateDynamicTexture()}. Backed by a
 * modern dynamic texture, which is what binding its location (see {@link TextureManager#getDynamicTextureLocation}) shows.
 */
public class DynamicTexture extends AbstractTexture {
    private final int[] dynamicTextureData;
    private final int width;
    private final int height;
    private net.minecraft.client.renderer.texture.DynamicTexture modern;

    public DynamicTexture(BufferedImage bufferedImage) {
        this(bufferedImage.getWidth(), bufferedImage.getHeight());
        bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), dynamicTextureData, 0, bufferedImage.getWidth());
        updateDynamicTexture();
    }

    public DynamicTexture(int textureWidth, int textureHeight) {
        width = textureWidth;
        height = textureHeight;
        dynamicTextureData = new int[textureWidth * textureHeight];
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
    }

    /** The modern texture, created on first use. */
    public synchronized net.minecraft.client.renderer.texture.DynamicTexture mcc$modern() {
        if (modern == null) modern = new net.minecraft.client.renderer.texture.DynamicTexture(new NativeImage(width, height, true));
        return modern;
    }

    /** Copies the ARGB array into the modern texture and uploads it (on the render thread; later when called elsewhere). */
    public void updateDynamicTexture() {
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(this::updateDynamicTexture);
            return;
        }
        NativeImage image = mcc$modern().getPixels();
        if (image == null) return;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = dynamicTextureData[y * width + x];
                image.setPixelRGBA(x, y, argb & 0xFF00FF00 | (argb >> 16 & 255) | (argb & 255) << 16);
            }
        }
        mcc$modern().upload();
    }

    public int[] getTextureData() {
        return dynamicTextureData;
    }

    @Override
    public int getGlTextureId() {
        return com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread() ? mcc$modern().getId() : 0;
    }

    @Override
    public void deleteGlTexture() {
        net.minecraft.client.renderer.texture.DynamicTexture m;
        synchronized (this) {
            m = modern;
            modern = null;
        }
        if (m != null) {
            if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) m.close();
            else com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(m::close);
        }
    }
}
