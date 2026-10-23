package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager;

/** A texture the legacy texture manager knows; only the atlases are real ones here (see {@link TextureManager#getTexture}). */
public interface ITextureObject {
    void loadTexture(IResourceManager manager) throws IOException;

    int getGlTextureId();
}
