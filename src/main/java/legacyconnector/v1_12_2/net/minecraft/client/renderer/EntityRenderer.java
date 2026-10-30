package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * 1.12.2 {@code Minecraft.entityRenderer}: the world renderer's public helpers mapped onto the modern game renderer (post-processing
 * shaders, the totem animation, the lightmap switches of the emulated GL state) and the name plate drawing legacy renderers use.
 * The world itself is drawn by the modern game; {@link #updateCameraAndRender} and {@link #renderWorld} do nothing.
 */
public class EntityRenderer implements legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManagerReloadListener {
    private static EntityRenderer instance;
    public static boolean anaglyphEnable;
    public static int anaglyphField;
    public final ItemRenderer itemRenderer;
    public boolean drawBlockOutline = true;
    public boolean renderHand = true;
    private final Minecraft mc;

    public EntityRenderer(Minecraft mc, IResourceManager resourceManager) {
        this.mc = mc;
        this.itemRenderer = ItemRenderer.mcc$instance();
    }

    /** The game's ({@code Minecraft.entityRenderer}). */
    public static synchronized EntityRenderer mcc$instance() {
        if (instance == null) instance = new EntityRenderer(Minecraft.getMinecraft(), Minecraft.getMinecraft().getResourceManager());
        return instance;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
    }

    private legacyconnector.v1_12_2.net.minecraft.client.gui.MapItemRenderer mcc$mapItemRenderer;

    /** The map renderer (maps in item frames, held maps and map tooltips), drawing through the modern one. */
    public legacyconnector.v1_12_2.net.minecraft.client.gui.MapItemRenderer getMapItemRenderer() {
        if (mcc$mapItemRenderer == null) mcc$mapItemRenderer = new legacyconnector.v1_12_2.net.minecraft.client.gui.MapItemRenderer(mc == null ? null : mc.getTextureManager());
        return mcc$mapItemRenderer;
    }

    public boolean isShaderActive() {
        return net.minecraft.client.Minecraft.getInstance().gameRenderer.currentEffect() != null;
    }

    /** The active post-processing chain (a view of the modern one), or null. */
    public legacyconnector.v1_12_2.net.minecraft.client.shader.ShaderGroup getShaderGroup() {
        return legacyconnector.v1_12_2.net.minecraft.client.shader.ShaderGroup.mcc$of(net.minecraft.client.Minecraft.getInstance().gameRenderer.currentEffect());
    }

    public void stopUseShader() {
        net.minecraft.client.Minecraft.getInstance().gameRenderer.shutdownEffect();
    }

    /** A post-processing shader by its 1.12 location ({@code shaders/post/*.json}); vanilla's moved shaders load from the same path. */
    public void loadShader(ResourceLocation resourceLocationIn) {
        if (resourceLocationIn == null) return;
        try {
            net.minecraft.client.Minecraft.getInstance().gameRenderer.loadEffect(resourceLocationIn.mcc$modern());
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("loading the shader " + resourceLocationIn, e);
        }
    }

    public void switchUseShader() {
        if (isShaderActive()) stopUseShader();
    }

    /** The modern game ticks the renderer itself. */
    public void updateRenderer() {
    }

    public void updateCameraAndRender(float partialTicks, long nanoTime) {
    }

    public void renderWorld(float partialTicks, long finishTimeNano) {
    }

    /** The totem-of-undying style item pop-up. */
    public void displayItemActivation(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) net.minecraft.client.Minecraft.getInstance().gameRenderer.displayItemActivation(Stacks1122.toModern(stack));
    }

    public void disableLightmap() {
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    public void enableLightmap() {
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /** The GUI projection: screens and HUD overlays already draw in the modern GUI pass. */
    public void setupOverlayRendering() {
    }

    /** Fog colour: the modern fog renderer sets it for the level being drawn. */
    public void setupFogColor(boolean black) {
    }

    /** A name plate facing the viewer: a dark background and the text, seen through blocks unless the entity sneaks. */
    public static void drawNameplate(FontRenderer fontRendererIn, String str, float x, float y, float z, int verticalShift, float viewerYaw, float viewerPitch,
            boolean isThirdPersonFrontal, boolean isSneaking) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-viewerYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float) (isThirdPersonFrontal ? -1 : 1) * viewerPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.025F, -0.025F, 0.025F);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        if (!isSneaking) GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        int half = fontRendererIn.getStringWidth(str) / 2;
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos((double) (-half - 1), (double) (-1 + verticalShift), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        buffer.pos((double) (-half - 1), (double) (8 + verticalShift), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        buffer.pos((double) (half + 1), (double) (8 + verticalShift), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        buffer.pos((double) (half + 1), (double) (-1 + verticalShift), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        if (!isSneaking) {
            fontRendererIn.drawString(str, -fontRendererIn.getStringWidth(str) / 2, verticalShift, 553648127);
            GlStateManager.enableDepth();
        }
        GlStateManager.depthMask(true);
        fontRendererIn.drawString(str, -fontRendererIn.getStringWidth(str) / 2, verticalShift, isSneaking ? 553648127 : -1);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    // ------------------------------------------------------------------ camera and hands (runtime list, client)

    /**
     * 1.12's cinematic camera state (smoothed mouse turn). The modern mouse handler smooths its own turn; these keep what mods write
     * so they read it back, as camera mods that drive the cinematic camera expect.
     */
    public float smoothCamYaw;
    public float smoothCamPitch;
    public float smoothCamFilterX;
    public float smoothCamFilterY;
    public float smoothCamPartialTicks;

    /**
     * 1.12 loaded the world projection and camera matrices here before drawing world overlays. The modern game renderer owns both and
     * has them set up while legacy world-render callbacks run, so only the camera facts legacy code reads afterwards are refreshed.
     */
    public void setupCameraTransform(float partialTicks, int pass) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.gameRenderer != null) ActiveRenderInfo.getCameraPosition();
    }

    private static boolean mcc$inHand;

    /**
     * Draws the first-person hands and held items at the current matrix: mods that cancel {@code RenderHandEvent} to change how the
     * hands are drawn (another FOV, a shader) call it to draw them themselves. Only in first person with the HUD shown, as 1.12 did.
     */
    public void renderHand(float partialTicks, int pass) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mcc$inHand || mc.player == null || mc.gameRenderer == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson()) return;
        var pose = org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.pose();
        if (pose == null) pose = new com.mojang.blaze3d.vertex.PoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        mcc$inHand = true;
        try {
            mc.gameRenderer.itemInHandRenderer.renderHandsWithItems(partialTicks, pose, buffers, mc.player,
                    mc.getEntityRenderDispatcher().getPackedLightCoords(mc.player, partialTicks));
            buffers.endBatch();
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing the hands", e);
        } finally {
            mcc$inHand = false;
        }
    }
}
