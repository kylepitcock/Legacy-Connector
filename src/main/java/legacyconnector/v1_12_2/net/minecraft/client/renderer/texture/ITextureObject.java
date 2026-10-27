package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;

/** A texture with a GL texture of its own (see {@link TextureManager#loadTexture}). */
public interface ITextureObject {
    void setBlurMipmap(boolean blurIn, boolean mipmapIn);

    void restoreLastBlurMipmap();

    void loadTexture(IResourceManager resourceManager) throws IOException;

    int getGlTextureId();
}
