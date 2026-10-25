package legacyconnector.v1_7_10.net.minecraft.client.shader;

/**
 * A framebuffer legacy code can bind. Only the game's main one exists: it is what modern rendering already draws into,
 * so binding and unbinding it leave the render target alone.
 */
public class Framebuffer {
    public int framebufferWidth;
    public int framebufferHeight;
    public int framebufferTextureWidth;
    public int framebufferTextureHeight;
    public int framebufferTexture;
    public boolean useDepth = true;

    public Framebuffer() {
    }

    public Framebuffer(int width, int height, boolean useDepth) {
        this.framebufferWidth = width;
        this.framebufferHeight = height;
        this.framebufferTextureWidth = width;
        this.framebufferTextureHeight = height;
        this.useDepth = useDepth;
    }

    public void bindFramebuffer(boolean setViewport) {
    }

    public void unbindFramebuffer() {
    }

    public void bindFramebufferTexture() {
    }

    public void unbindFramebufferTexture() {
    }

    public void createBindFramebuffer(int width, int height) {
        framebufferWidth = framebufferTextureWidth = width;
        framebufferHeight = framebufferTextureHeight = height;
    }

    public void deleteFramebuffer() {
    }

    public void framebufferClear() {
    }

    public void framebufferRender(int width, int height) {
    }

    /** Sets the filter used when the framebuffer is drawn scaled. */
    public void setFramebufferFilter(int filter) {
    }
}
