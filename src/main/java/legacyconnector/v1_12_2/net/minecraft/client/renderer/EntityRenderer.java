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
}
