package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import org.lwjgl.opengl.GL11C;

/** Base of textures with an OpenGL texture of their own: the texture is created on first use and deleted by {@link #deleteGlTexture}. */
public abstract class AbstractTexture implements ITextureObject {
    protected int glTextureId = -1;

    @Override
    public int getGlTextureId() {
        if (glTextureId == -1) {
            if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) return 0; // no GL context here: a native call aborts the JVM
            glTextureId = GL11C.glGenTextures();
        }
        return glTextureId;
    }

    public void deleteGlTexture() {
        if (glTextureId != -1 && com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            GL11C.glDeleteTextures(glTextureId);
            glTextureId = -1;
        }
    }
}
