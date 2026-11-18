package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer;

/** Facade of the 1.7.10 shim's {@code RenderGlobal} for 1.4.7 and 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class RenderGlobal {
    private RenderGlobal() {}

    /** {@code drawOutlinedBoundingBox(box)} (static): the 12 edges of the box in the current colour (1.7.10 takes the colour as an argument). */
    public static void drawOutlinedBoundingBox(legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB box) {
        legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal.drawOutlinedBoundingBox(box, -1);
    }

    /**
     * {@code onEntityCreate(entity)}: the world access hook for a spawned entity. 1.5.2 used it to start the skin and cloak download; the 1.7.10 game's
     * (and this layer's) renderer does nothing there, entities are tracked when the world adds them.
     */
    public static void onEntityCreate(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal self, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
    }

    /** {@code onEntityDestroy(entity)}: the counterpart of {@code onEntityCreate}, empty in 1.7.10 as well. */
    public static void onEntityDestroy(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal self, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
    }

    // ------------------------------------------------------------------ 1.6.4 gap round 3

    /** 1.6.4 {@code RenderGlobal.hasCloudFog(x, y, z, partialTicks)}: whether the dimension's sky effects fog the position (the Nether). */
    public static boolean hasCloudFog(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal self, double x, double y, double z, float partialTicks) {
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        return level != null && level.effects().isFoggyAt((int) Math.floor(x), (int) Math.floor(z));
    }

    /** 1.6.4 {@code RenderGlobal.setWorldAndLoadRenderers(world)}: the client world changed, so every chunk renderer is rebuilt. */
    public static void setWorldAndLoadRenderers(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal self, legacyconnector.v1_7_10.net.minecraft.client.multiplayer.WorldClient world) {
        net.minecraft.client.renderer.LevelRenderer renderer = net.minecraft.client.Minecraft.getInstance().levelRenderer;
        if (renderer != null) renderer.allChanged();
    }

    /** 1.6.4 {@code RenderGlobal.updateClouds()}: the modern cloud layer scrolls on its own, so there is nothing to tick. */
    public static void updateClouds(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal self) {
    }

    /** 1.6.4 {@code RenderGlobal.renderClouds(partialTicks)}: the modern renderer draws the cloud layer in its own pass, so a mod's call has nothing to draw. */
    public static void renderClouds(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal self, float partialTicks) {
    }

    /** 1.6.4 {@code RenderGlobal.renderCloudsFancy(partialTicks)}: see {@link #renderClouds}. */
    public static void renderCloudsFancy(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal self, float partialTicks) {
    }

    /** 1.6.4 {@code RenderGlobal.registerDestroyBlockIcons(register)}: the block breaking stages are modern atlas sprites, so there is nothing to register. */
    public static void registerDestroyBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal self, legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
    }
}
