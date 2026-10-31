package legacyconnector.v1_12_2.net.minecraft.client.shader;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;

/**
 * 1.12.2 post-processing chain ({@code shaders/post/<name>.json}), run by a modern {@link PostChain} over a framebuffer (normally the
 * main one). {@code EntityRenderer.getShaderGroup()} returns a view of the game's active chain. Mods tune its passes through
 * {@link #listShaders} and their uniforms. A chain the modern game cannot load logs once and does nothing.
 */
public class ShaderGroup {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Shaders");
    private final Framebuffer mainFramebuffer;
    private final String shaderGroupName;
    /** The passes (Shader views of the chain's passes, then those added with addShader). */
    public final List<Shader> listShaders = new ArrayList<>();
    private PostChain mcc$chain;
    private final boolean mcc$owned;
    private float time;
    private float lastStamp;

    public ShaderGroup(TextureManager textureManagerIn, IResourceManager resourceManagerIn, Framebuffer mainFramebufferIn, ResourceLocation resourceLocationIn) {
        this.mainFramebuffer = mainFramebufferIn;
        this.shaderGroupName = resourceLocationIn == null ? "" : resourceLocationIn.toString();
        this.mcc$owned = true;
        var mc = net.minecraft.client.Minecraft.getInstance();
        var target = mainFramebufferIn != null && mainFramebufferIn.mcc$target() != null ? mainFramebufferIn.mcc$target() : mc.getMainRenderTarget();
        if (resourceLocationIn == null) return;
        if (!org.kylepitcock.legacyconnector.v1_12_2.client.PostShaders1122.usable(resourceLocationIn.mcc$modern())) return;
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            LOG.warn("The post shader {} was made off the render thread and is not loaded", resourceLocationIn);
            return;
        }
        try {
            mcc$chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), target, resourceLocationIn.mcc$modern());
            mcc$chain.resize(target.width, target.height);
            mcc$loadPasses();
        } catch (Exception | LinkageError e) {
            LOG.warn("The post shader {} could not be loaded: {}", resourceLocationIn, e.toString());
            mcc$chain = null;
        }
    }

    private ShaderGroup(PostChain chain) {
        this.mainFramebuffer = Framebuffer.mcc$main();
        this.shaderGroupName = chain.getName();
        this.mcc$chain = chain;
        this.mcc$owned = false;
        mcc$loadPasses();
    }

    private static ShaderGroup current;

    /** A view of the game's active post chain (EntityRenderer.getShaderGroup), or null. */
    public static synchronized ShaderGroup mcc$of(PostChain chain) {
        if (chain == null) return null;
        if (current == null || current.mcc$chain != chain) current = new ShaderGroup(chain);
        return current;
    }

    @SuppressWarnings("unchecked")
    private void mcc$loadPasses() {
        try {
            var f = PostChain.class.getDeclaredField("passes");
            f.setAccessible(true);
            for (PostPass pass : (List<PostPass>) f.get(mcc$chain)) listShaders.add(new Shader(pass));
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.debug("The passes of {} cannot be listed: {}", shaderGroupName, e.toString());
        }
    }

    public void deleteShaderGroup() {
        if (mcc$chain != null && mcc$owned) mcc$chain.close();
        mcc$chain = null;
        listShaders.clear();
    }

    public Shader addShader(String programName, Framebuffer framebufferIn, Framebuffer framebufferOut) {
        if (mcc$chain == null || framebufferIn == null || framebufferOut == null) return null;
        try {
            Shader shader = new Shader(mcc$chain.addPass(programName, framebufferIn.mcc$target(), framebufferOut.mcc$target(), false));
            listShaders.add(shader);
            return shader;
        } catch (Exception | LinkageError e) {
            LOG.warn("The shader pass {} could not be added to {}: {}", programName, shaderGroupName, e.toString());
            return null;
        }
    }

    public Framebuffer getFramebufferRaw(String attributeName) {
        if (mcc$chain == null) return null;
        try {
            return Framebuffer.mcc$wrap(mcc$chain.getTempTarget(attributeName));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public void createBindFramebuffers(int width, int height) {
        if (mcc$chain != null) mcc$chain.resize(width, height);
    }

    public void render(float partialTicks) {
        float delta = partialTicks < lastStamp ? 1.0F - lastStamp + partialTicks : partialTicks - lastStamp;
        lastStamp = partialTicks;
        time += delta;
        while (time > 20.0F) time -= 20.0F;
        if (mcc$chain == null || !com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) return;
        try {
            mcc$chain.process(partialTicks);
            net.minecraft.client.Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        } catch (RuntimeException e) {
            LOG.warn("The post shader {} failed and is turned off: {}", shaderGroupName, e.toString());
            deleteShaderGroup();
        }
    }

    public final String getShaderGroupName() {
        return mcc$chain != null ? mcc$chain.getName() : shaderGroupName;
    }
}
