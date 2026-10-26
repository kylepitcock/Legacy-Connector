package legacyconnector.v1_7_10.net.minecraft.client.shader;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.PostChain;

/**
 * A post-processing chain read from {@code shaders/post/<name>.json} (Thaumcraft's warp desaturation and blur), run by the
 * modern {@link PostChain} over the game's main framebuffer. A chain the modern game cannot load (a program in a GLSL
 * version the core profile rejects) logs once and does nothing.
 */
public class ShaderGroup {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Shaders");

    private final Framebuffer mainFramebuffer;
    private final String shaderGroupName;
    private PostChain mcc$chain;
    private int mainFramebufferWidth;
    private int mainFramebufferHeight;
    private float field_148036_j;
    private float field_148037_k;

    public ShaderGroup(TextureManager textures, IResourceManager resources, Framebuffer main, ResourceLocation location) {
        this.mainFramebuffer = main;
        this.shaderGroupName = location == null ? "" : location.toString();
        var mc = net.minecraft.client.Minecraft.getInstance();
        var target = mc.getMainRenderTarget();
        this.mainFramebufferWidth = target.width;
        this.mainFramebufferHeight = target.height;
        if (location == null) return;
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            // GL objects can only be made on the render thread (a driver call elsewhere aborts the JVM)
            LOG.warn("The post shader {} was made off the render thread and is not loaded", location);
            return;
        }
        try {
            var modern = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    org.kylepitcock.legacyconnector.core.Ids.namespace(location.getResourceDomain()),
                    org.kylepitcock.legacyconnector.core.Ids.normalizePath(location.getResourcePath()));
            mcc$chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), target, modern);
            mcc$chain.resize(target.width, target.height);
        } catch (Exception | LinkageError e) {
            LOG.warn("The post shader {} could not be loaded: {}", location, e.toString());
            mcc$chain = null;
        }
    }

    /** Frees the chain's framebuffers and programs. */
    public void deleteShaderGroup() {
        if (mcc$chain != null) {
            mcc$chain.close();
            mcc$chain = null;
        }
    }

    /** Sizes the chain's framebuffers to the window. */
    public void createBindFramebuffers(int width, int height) {
        mainFramebufferWidth = width;
        mainFramebufferHeight = height;
        if (mcc$chain != null) mcc$chain.resize(width, height);
    }

    /** Runs the chain over the main framebuffer; {@code partialTicks} advances the chain's clock as in 1.7.10. */
    public void loadShaderGroup(float partialTicks) {
        float delta = partialTicks < field_148037_k ? 1.0F - field_148037_k + partialTicks : partialTicks - field_148037_k;
        field_148037_k = partialTicks;
        field_148036_j += delta;
        while (field_148036_j > 20.0F) field_148036_j -= 20.0F;
        if (mcc$chain == null || !com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) return;
        try {
            var target = net.minecraft.client.Minecraft.getInstance().getMainRenderTarget();
            if (target.width != mainFramebufferWidth || target.height != mainFramebufferHeight) createBindFramebuffers(target.width, target.height);
            mcc$chain.process(delta);
            target.bindWrite(true);
        } catch (RuntimeException e) {
            LOG.warn("The post shader {} failed and is turned off: {}", shaderGroupName, e.toString());
            deleteShaderGroup();
        }
    }

    public final String getShaderGroupName() {
        return mcc$chain != null ? mcc$chain.getName() : shaderGroupName;
    }
}
