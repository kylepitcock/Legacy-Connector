package legacyconnector.v1_12_2.net.minecraft.client.shader;

/** Minimal stub (area R1: rendering) so {@code Minecraft.getFramebuffer()} can return it; R1's class replaces it. */
public class Framebuffer {
    public int framebufferTextureWidth;
    public int framebufferTextureHeight;
    public int framebufferWidth;
    public int framebufferHeight;
    public boolean useDepth;

    public Framebuffer(int width, int height, boolean useDepth) {
        this.useDepth = useDepth;
        this.framebufferWidth = this.framebufferTextureWidth = width;
        this.framebufferHeight = this.framebufferTextureHeight = height;
    }
}
