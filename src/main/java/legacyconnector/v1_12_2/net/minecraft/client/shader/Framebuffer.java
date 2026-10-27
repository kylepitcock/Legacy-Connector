package legacyconnector.v1_12_2.net.minecraft.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

/**
 * 1.12.2 framebuffer, backed by a modern {@link RenderTarget}: a mod's own framebuffer is a {@link TextureTarget} of its size, and
 * {@link #mcc$main()} ({@code Minecraft.getFramebuffer()}) is the game's main target. The public ids (framebuffer object, colour
 * texture, depth buffer) are the modern target's GL names, so mods that bind them with raw GL calls get the real objects. Unbinding
 * returns drawing to the main target, as binding framebuffer 0 returned it to the window in 1.12.
 */
public class Framebuffer {
    private static Framebuffer main;
    public int framebufferTextureWidth;
    public int framebufferTextureHeight;
    public int framebufferWidth;
    public int framebufferHeight;
    public boolean useDepth;
    public int framebufferObject = -1;
    public int framebufferTexture = -1;
    public int depthBuffer = -1;
    public float[] framebufferColor = new float[] {1.0F, 1.0F, 1.0F, 0.0F};
    public int framebufferFilter = 9728;
    private RenderTarget mcc$target;
    private final boolean mcc$isMain;

    public Framebuffer(int width, int height, boolean useDepthIn) {
        this.useDepth = useDepthIn;
        this.mcc$isMain = false;
        createBindFramebuffer(width, height);
    }

    private Framebuffer(RenderTarget target) {
        this.useDepth = target.useDepth;
        this.mcc$isMain = true;
        this.mcc$target = target;
        mcc$sync();
    }

    /** The game's main framebuffer ({@code Minecraft.getFramebuffer()}). */
    public static synchronized Framebuffer mcc$main() {
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (main == null || main.mcc$target != target) main = new Framebuffer(target);
        main.mcc$sync();
        return main;
    }

    /** A view of a target the shim does not own (a post chain's temporary target): never resized or deleted through it. */
    public static Framebuffer mcc$wrap(RenderTarget target) {
        return target == null ? null : new Framebuffer(target);
    }

    /** The modern target (null before it was created). */
    public RenderTarget mcc$target() {
        return mcc$target;
    }

    private void mcc$sync() {
        if (mcc$target == null) return;
        framebufferWidth = mcc$target.viewWidth;
        framebufferHeight = mcc$target.viewHeight;
        framebufferTextureWidth = mcc$target.width;
        framebufferTextureHeight = mcc$target.height;
        framebufferObject = mcc$target.frameBufferId;
        framebufferTexture = mcc$target.getColorTextureId();
        depthBuffer = useDepth ? mcc$target.getDepthTextureId() : -1;
        framebufferFilter = mcc$target.filterMode;
    }

    private static boolean renderThread() {
        return com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread();
    }

    public void createBindFramebuffer(int width, int height) {
        framebufferWidth = framebufferTextureWidth = width;
        framebufferHeight = framebufferTextureHeight = height;
        if (mcc$isMain || !renderThread() || width <= 0 || height <= 0) return;
        if (mcc$target == null) mcc$target = new TextureTarget(width, height, useDepth, Minecraft.ON_OSX);
        else mcc$target.resize(width, height, Minecraft.ON_OSX);
        mcc$target.setClearColor(framebufferColor[0], framebufferColor[1], framebufferColor[2], framebufferColor[3]);
        mcc$sync();
    }

    public void createFramebuffer(int width, int height) {
        createBindFramebuffer(width, height);
    }

    public void deleteFramebuffer() {
        if (mcc$isMain || mcc$target == null || !renderThread()) return;
        mcc$target.destroyBuffers();
        mcc$target = null;
        framebufferObject = framebufferTexture = depthBuffer = -1;
    }

    public boolean enableStencil() {
        if (mcc$target == null) return false;
        if (!mcc$target.isStencilEnabled() && renderThread()) mcc$target.enableStencil();
        return mcc$target.isStencilEnabled();
    }

    public boolean isStencilEnabled() {
        return mcc$target != null && mcc$target.isStencilEnabled();
    }

    public void checkFramebufferComplete() {
        if (mcc$target != null) mcc$target.checkStatus();
    }

    public void setFramebufferFilter(int framebufferFilterIn) {
        framebufferFilter = framebufferFilterIn;
        if (mcc$target != null && renderThread()) mcc$target.setFilterMode(framebufferFilterIn);
    }

    public void bindFramebufferTexture() {
        if (mcc$target != null) mcc$target.bindRead();
    }

    public void unbindFramebufferTexture() {
        if (mcc$target != null) mcc$target.unbindRead();
    }

    public void bindFramebuffer(boolean setViewport) {
        if (mcc$target != null && renderThread()) mcc$target.bindWrite(setViewport);
    }

    public void unbindFramebuffer() {
        if (!renderThread()) return;
        if (mcc$target != null) mcc$target.unbindWrite();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    public void setFramebufferColor(float red, float green, float blue, float alpha) {
        framebufferColor[0] = red;
        framebufferColor[1] = green;
        framebufferColor[2] = blue;
        framebufferColor[3] = alpha;
        if (mcc$target != null) mcc$target.setClearColor(red, green, blue, alpha);
    }

    public void framebufferClear() {
        if (mcc$target == null || !renderThread()) return;
        mcc$target.setClearColor(framebufferColor[0], framebufferColor[1], framebufferColor[2], framebufferColor[3]);
        mcc$target.clear(Minecraft.ON_OSX);
        if (!mcc$isMain) Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    public void framebufferRender(int width, int height) {
        framebufferRenderExt(width, height, true);
    }

    /** Draws the colour texture over the whole window (a mod's loading screen, a blurred copy). */
    public void framebufferRenderExt(int width, int height, boolean disableBlend) {
        if (mcc$target == null || !renderThread()) return;
        mcc$target.blitToScreen(width, height, disableBlend);
    }
}
