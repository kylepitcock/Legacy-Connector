package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * A 1.12.2 texture object (SimpleTexture, a mod's own AbstractTexture subclass) as a modern texture: registering it runs the legacy
 * {@code loadTexture} (which uploads its images itself), and binding it binds the GL texture the legacy object owns.
 */
public final class LegacyGlTexture1122 extends AbstractTexture {
    private final ITextureObject legacy;

    public LegacyGlTexture1122(ITextureObject legacy) {
        this.legacy = legacy;
    }

    public ITextureObject legacy() {
        return legacy;
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        legacy.loadTexture(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().getResourceManager());
    }

    @Override
    public int getId() {
        return legacy.getGlTextureId();
    }

    @Override
    public void releaseId() {
        if (legacy instanceof legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.AbstractTexture texture) {
            if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) texture.deleteGlTexture();
            else com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(texture::deleteGlTexture);
        }
    }
}
