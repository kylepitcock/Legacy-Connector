package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * A legacy texture object ({@code SimpleTexture}, a mod's own {@code AbstractTexture} subclass, a downloaded skin...) as a modern
 * texture: registering it runs the object's legacy {@code loadTexture} (which reads its images and uploads them itself), and
 * binding it binds the GL texture the legacy object owns, so modern draws of its location show what the mod uploaded.
 */
public final class LegacyGlTexture extends AbstractTexture {
    private final ITextureObject legacy;

    public LegacyGlTexture(ITextureObject legacy) {
        this.legacy = legacy;
    }

    public ITextureObject legacy() {
        return legacy;
    }

    /** 1.7.10 loaded (and on a resource reload re-loaded) texture objects with the mods' resource manager. */
    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        legacy.loadTexture(legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().getResourceManager());
    }

    /** The legacy object's current texture (it may replace it, e.g. a skin uploaded once its download finished). */
    @Override
    public int getId() {
        return legacy.getGlTextureId();
    }

    @Override
    public void releaseId() {
        if (legacy instanceof legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.AbstractTexture texture) {
            if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) texture.deleteGlTexture();
            else com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(texture::deleteGlTexture);
        }
    }
}
