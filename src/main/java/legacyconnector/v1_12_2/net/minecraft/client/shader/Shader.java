package legacyconnector.v1_12_2.net.minecraft.client.shader;

import net.minecraft.client.renderer.PostPass;

/** 1.12.2 pass of a post-processing chain (a modern {@link PostPass}): its program and its in and out framebuffers. */
public class Shader {
    private final ShaderManager manager;
    public final Framebuffer framebufferIn;
    public final Framebuffer framebufferOut;
    private final PostPass mcc$pass;

    Shader(PostPass pass) {
        this.mcc$pass = pass;
        this.manager = new ShaderManager(pass.getEffect());
        this.framebufferIn = Framebuffer.mcc$wrap(pass.inTarget);
        this.framebufferOut = Framebuffer.mcc$wrap(pass.outTarget);
    }

    /** A pass of the named program (a post-processing shader of the game's resource manager) from one framebuffer into another. */
    public Shader(legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager resourceManager, String programName, Framebuffer in, Framebuffer out)
            throws java.io.IOException {
        this(new PostPass(net.minecraft.client.Minecraft.getInstance().getResourceManager(), programName, in.mcc$target(), out.mcc$target(), false));
    }

    public ShaderManager getShaderManager() {
        return manager;
    }

    public void deleteShader() {
        mcc$pass.close();
    }

    public void render(float partialTicks) {
        mcc$pass.process(partialTicks);
    }
}
