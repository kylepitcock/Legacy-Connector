package legacyconnector.v1_7_10.net.minecraft.client.gui;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraft.world.storage.MapData;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Draws a legacy {@link MapData} (a mod's map, or one a mod built for a lectern, atlas or wireless map) as 1.7.10 did:
 * the 128x128 colour grid as a texture on a 128x128 quad at z -0.01, the decorations above it. Each map's texture is a
 * modern dynamic texture refreshed from the colours on every draw.
 */
public class MapItemRenderer {
    /** 1.7.10's icon sheet; the modern game has one texture per decoration (the same order of types). */
    private static final ResourceLocation field_148253_a = new ResourceLocation("textures/map/map_icons.png");
    private static final String[] DECORATIONS = {"player", "frame", "red_marker", "blue_marker", "target_x", "target_point", "player_off_map",
            "player_off_limits", "woodland_mansion", "ocean_monument", "white_banner", "orange_banner", "magenta_banner", "light_blue_banner",
            "yellow_banner", "lime_banner"};
    private final TextureManager textureManager;
    private final Map loadedMaps = new HashMap();

    public MapItemRenderer(TextureManager textureManager) {
        this.textureManager = textureManager;
    }

    /** Refreshes the texture of a map from its colours. */
    public void func_148246_a(MapData data) {
        if (data != null) instance(data).update();
    }

    /** Draws a map; {@code noOverlay} (item frames) leaves out all decorations but frames. */
    public void func_148250_a(MapData data, boolean noOverlay) {
        if (data != null) instance(data).render(noOverlay);
    }

    /** Frees every map texture. */
    public void func_148249_a() {
        for (Object o : loadedMaps.values()) ((Instance) o).release();
        loadedMaps.clear();
    }

    private Instance instance(MapData data) {
        Instance instance = (Instance) loadedMaps.get(data.mapName);
        if (instance == null || instance.data != data) {
            if (instance != null) instance.release();
            instance = new Instance(data);
            loadedMaps.put(data.mapName, instance);
        }
        return instance;
    }

    private static int counter;

    private static final class Instance {
        final MapData data;
        private net.minecraft.client.renderer.texture.DynamicTexture texture;
        private net.minecraft.resources.ResourceLocation location;

        Instance(MapData data) {
            this.data = data;
        }

        void update() {
            if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) return;
            if (texture == null) {
                texture = new net.minecraft.client.renderer.texture.DynamicTexture(128, 128, true);
                String name = data.mapName == null ? "map" : data.mapName.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
                location = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_map/" + name + "_" + counter++);
                net.minecraft.client.Minecraft.getInstance().getTextureManager().register(location, texture);
            }
            var pixels = texture.getPixels();
            if (pixels == null) return;
            byte[] colors = data.colors;
            for (int i = 0; i < 16384; i++) {
                int packed = colors == null || i >= colors.length ? 0 : colors[i] & 255;
                // 1.7.10 left uncoloured pixels a faint checkerboard
                int abgr = packed / 4 == 0 ? ((i + i / 128 & 1) * 8 + 16) << 24 : net.minecraft.world.level.material.MapColor.getColorFromPackedId(packed);
                pixels.setPixelRGBA(i % 128, i / 128, abgr);
            }
            texture.upload();
        }

        void render(boolean noOverlay) {
            update();
            if (location == null) return;
            Tessellator t = Tessellator.instance;
            RenderContext.bindTexture(location);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            t.startDrawingQuads();
            t.addVertexWithUV(0.0D, 128.0D, -0.01D, 0.0D, 1.0D);
            t.addVertexWithUV(128.0D, 128.0D, -0.01D, 1.0D, 1.0D);
            t.addVertexWithUV(128.0D, 0.0D, -0.01D, 1.0D, 0.0D);
            t.addVertexWithUV(0.0D, 0.0D, -0.01D, 0.0D, 0.0D);
            t.draw();
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            int k = 0;
            for (Object o : data.playersVisibleOnMap.values()) {
                if (!(o instanceof MapData.MapCoord coord)) continue;
                if (noOverlay && coord.iconSize != 1) continue;
                int type = coord.iconSize & 255;
                if (type >= DECORATIONS.length) type = 0;
                RenderContext.bindTexture(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/map/decorations/" + DECORATIONS[type] + ".png"));
                GL11.glPushMatrix();
                GL11.glTranslatef(coord.centerX / 2.0F + 64.0F, coord.centerZ / 2.0F + 64.0F, -0.02F);
                GL11.glRotatef(coord.iconRotation * 360 / 16.0F, 0.0F, 0.0F, 1.0F);
                GL11.glScalef(4.0F, 4.0F, 3.0F);
                GL11.glTranslatef(-0.125F, 0.125F, 0.0F);
                t.startDrawingQuads();
                t.addVertexWithUV(-1.0D, 1.0D, k * 0.001F, 0.0D, 0.0D);
                t.addVertexWithUV(1.0D, 1.0D, k * 0.001F, 1.0D, 0.0D);
                t.addVertexWithUV(1.0D, -1.0D, k * 0.001F, 1.0D, 1.0D);
                t.addVertexWithUV(-1.0D, -1.0D, k * 0.001F, 0.0D, 1.0D);
                t.draw();
                GL11.glPopMatrix();
                k++;
            }
        }

        void release() {
            if (location != null) {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null && com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) mc.getTextureManager().release(location);
            }
            texture = null;
            location = null;
        }
    }
}
