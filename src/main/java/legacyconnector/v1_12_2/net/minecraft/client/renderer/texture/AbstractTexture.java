package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;

/** Base of textures with a GL texture of their own, created on first use; blur and mipmap set the bound texture's filters. */
public abstract class AbstractTexture implements ITextureObject {
    protected int glTextureId = -1;
    protected boolean blur;
    protected boolean mipmap;
    protected boolean blurLast;
    protected boolean mipmapLast;

    public void setBlurMipmapDirect(boolean blurIn, boolean mipmapIn) {
        blur = blurIn;
        mipmap = mipmapIn;
        int min, mag;
        if (blurIn) {
            min = mipmapIn ? 9987 : 9729;
            mag = 9729;
        } else {
            min = mipmapIn ? 9986 : 9728;
            mag = 9728;
        }
        GlStateManager.glTexParameteri(3553, 10241, min);
        GlStateManager.glTexParameteri(3553, 10240, mag);
    }

    @Override
    public void setBlurMipmap(boolean blurIn, boolean mipmapIn) {
        blurLast = blur;
        mipmapLast = mipmap;
        setBlurMipmapDirect(blurIn, mipmapIn);
    }

    @Override
    public void restoreLastBlurMipmap() {
        setBlurMipmapDirect(blurLast, mipmapLast);
    }

    @Override
    public int getGlTextureId() {
        if (glTextureId == -1) {
            int id = TextureUtil.glGenTextures();
            if (id == 0) return 0; // not on the render thread: try again there
            glTextureId = id;
        }
        return glTextureId;
    }

    public void deleteGlTexture() {
        if (glTextureId != -1) {
            TextureUtil.deleteTexture(glTextureId);
            glTextureId = -1;
        }
    }
}
