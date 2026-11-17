package legacyconnector.v1_7_10.net.minecraft.client.renderer;

/** 1.7.10's world renderer object: mods set a couple of its debug fields; rendering itself is the modern game's. */
public class EntityRenderer implements legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManagerReloadListener {
    /** 3D anaglyph mode (gone from the modern game: always off). */
    public static boolean anaglyphEnable;
    public final ItemRenderer itemRenderer;
    public int debugViewDirection;
    public boolean debugView;
    public float farPlaneDistance;
    public int rendererUpdateCount;
    private final legacyconnector.v1_7_10.net.minecraft.client.gui.MapItemRenderer theMapItemRenderer;

    public EntityRenderer() {
        this(null, null);
    }

    /** Mods subclass it for alternative views (Witchery's entity viewer, Fisk's Heroes' camera); the modern game still renders. */
    public EntityRenderer(legacyconnector.v1_7_10.net.minecraft.client.Minecraft mc, legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager manager) {
        this.itemRenderer = new ItemRenderer(mc);
        this.theMapItemRenderer = new legacyconnector.v1_7_10.net.minecraft.client.gui.MapItemRenderer(
                legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager.INSTANCE);
    }

    /** Draws legacy map data (mods' lecterns, atlases and wireless maps). */
    public legacyconnector.v1_7_10.net.minecraft.client.gui.MapItemRenderer getMapItemRenderer() {
        return theMapItemRenderer;
    }

    /** Finds what the crosshair is on (block or entity), from the current camera entity: the modern pick. */
    /** The field of view in degrees: the player's setting (or 70 when {@code useSetting} is false) scaled by speed and effects. */
    public float getFOVModifier(float partialTicks, boolean useSetting) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        float fov = useSetting ? mc.options.fov().get().floatValue() : 70.0F;
        if (mc.player != null) {
            fov *= mc.player.getFieldOfViewModifier();
            if (mc.player.getHealth() <= 0.0F) fov /= (1.0F - 500.0F / (Math.min(mc.player.deathTime + partialTicks, 20.0F) + 500.0F)) * 2.0F + 1.0F;
            if (mc.player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) fov = fov * 60.0F / 70.0F;
        }
        return fov;
    }

    public void getMouseOver(float partialTicks) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.gameRenderer != null && mc.level != null && mc.getCameraEntity() != null) mc.gameRenderer.pick(partialTicks);
    }

    /**
     * 1.7.10 loaded the world projection and camera matrices here before drawing world overlays. The modern game renderer owns
     * both; this keeps the fields legacy code reads afterwards (far plane, billboard rotations) up to date.
     */
    public void setupCameraTransform(float partialTicks, int pass) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.options != null) farPlaneDistance = mc.options.getEffectiveRenderDistance() * 16;
        var camera = mc.gameRenderer == null ? null : mc.gameRenderer.getMainCamera();
        if (camera != null && camera.isInitialized()) ActiveRenderInfo.mcc$update(camera.getYRot(), camera.getXRot());
    }

    /** How far behind the player the third person camera sits (mods set it by reflection: Legends' giant characters). */
    public float thirdPersonDistance = 4.0F;
    public float thirdPersonDistanceTemp = 4.0F;

    /**
     * The frame. A mod's subclass overrides this to set things up, calls {@code super} and undoes them afterwards: the call to here is the point where the
     * modern frame is drawn (see {@code LegacyRendererOverrides}).
     */
    public void updateCameraAndRender(float partialTicks) {
        if (theShaderGroup != null) theShaderGroup.loadShaderGroup(partialTicks);
        org.kylepitcock.legacyconnector.v1_7_10.client.LegacyRendererOverrides.yieldToModernFrame();
    }

    private legacyconnector.v1_7_10.net.minecraft.client.shader.ShaderGroup theShaderGroup;

    /** The post-processing chain the legacy code switched on (null when none). */
    public legacyconnector.v1_7_10.net.minecraft.client.shader.ShaderGroup getShaderGroup() {
        return theShaderGroup;
    }

    /** Whether a post-processing chain is on. */
    public boolean isShaderActive() {
        return theShaderGroup != null;
    }

    /** Switches the post-processing chain off and frees it. */
    public void deactivateShader() {
        if (theShaderGroup != null) {
            theShaderGroup.deleteShaderGroup();
            theShaderGroup = null;
        }
    }

    /** Switches a post-processing chain ({@code shaders/post/<name>.json}) on, replacing the current one. */
    public void func_147705_c(legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation location) {
        deactivateShader();
        theShaderGroup = new legacyconnector.v1_7_10.net.minecraft.client.shader.ShaderGroup(
                legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager.INSTANCE, null, null, location);
    }

    public void disableLightmap(double partialTicks) {
    }

    public void enableLightmap(double partialTicks) {
    }

    public void func_78483_a(double partialTicks) {
    }

    public void func_78463_b(double partialTicks) {
    }

    public void setupOverlayRendering() {
    }

    /**
     * 1.7.10's EntityRenderer is a resource reload listener (it reloaded its post shaders); mods that replace it register their
     * subclass with the resource manager, so the interface must be there. The modern game reloads its own shaders.
     */
    @Override
    public void onResourceManagerReload(legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager manager) {
    }
}
