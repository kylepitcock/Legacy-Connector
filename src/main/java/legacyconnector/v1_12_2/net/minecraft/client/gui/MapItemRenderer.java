package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_12_2.net.minecraft.world.storage.MapData;
import legacyconnector.v1_12_2.net.minecraft.world.storage.MapDecoration;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's map renderer: draws a map's 128x128 picture (and its icons) at the current matrix through the game's map renderer. A map
 * the client level knows by its name ({@code map_<id>}) is drawn from the live modern map; any other map data (a mod's own, or a
 * copy) is drawn from a modern client map filled with its colours and icons, kept per legacy map.
 */
public class MapItemRenderer {
    /** Map ids for legacy maps the client level does not know (counting down from the top, away from real ids). */
    private static int nextPrivateId = Integer.MAX_VALUE;
    private static final Map<MapData, Detached> DETACHED = new WeakHashMap<>();

    private final TextureManager textureManager;

    public MapItemRenderer(TextureManager textureManagerIn) {
        this.textureManager = textureManagerIn;
    }

    private static MapItemRenderer instance;

    /** The client's map renderer ({@code EntityRenderer.getMapItemRenderer()}). */
    public static synchronized MapItemRenderer mcc$instance() {
        if (instance == null) instance = new MapItemRenderer(null);
        return instance;
    }

    private record Detached(net.minecraft.world.level.saveddata.maps.MapId id, net.minecraft.world.level.saveddata.maps.MapItemSavedData data) {}

    /** Marks the map's picture for upload (the game uploads it when drawn next). */
    public void updateMapTexture(MapData mapdataIn) {
        if (mapdataIn == null) return;
        Instance instance = getMapInstanceIfExists(mapdataIn.mapName);
        if (instance != null && instance.mapData == mapdataIn) instance.updateMapTexture();
        try {
            Detached d = detached(mapdataIn, false);
            if (d != null && fill(mapdataIn, d.data())) net.minecraft.client.Minecraft.getInstance().gameRenderer.getMapRenderer().update(d.id(), d.data());
        } catch (RuntimeException | LinkageError e) {
            // drawn as it was
        }
    }

    /**
     * Draws the map at (0,0)-(128,128) of the current matrix. {@code noOverlayRendering} is 1.12's item frame mode: only the icons
     * shown on frames are drawn.
     */
    public void renderMap(MapData mapdataIn, boolean noOverlayRendering) {
        if (mapdataIn == null) return;
        var pose = RenderContext.pose();
        var buffers = RenderContext.buffers();
        if (pose == null || buffers == null) return;
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.world.level.saveddata.maps.MapId id = null;
            net.minecraft.world.level.saveddata.maps.MapItemSavedData data = null;
            int number = mcc$mapNumber(mapdataIn.mapName);
            if (number >= 0 && mc.level != null) {
                id = new net.minecraft.world.level.saveddata.maps.MapId(number);
                data = mc.level.getMapData(id);
            }
            if (data == null) {
                Detached d = detached(mapdataIn, true);
                id = d.id();
                data = d.data();
                if (fill(mapdataIn, data)) mc.gameRenderer.getMapRenderer().update(id, data);
            }
            mc.gameRenderer.getMapRenderer().render(pose, buffers, id, data, noOverlayRendering, RenderContext.light());
            var gui = RenderContext.gui();
            if (gui != null) gui.flush();
        } catch (RuntimeException | LinkageError e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing a map", e);
        }
    }

    /** The number of a map named {@code map_<n>}, or -1. */
    private static int mcc$mapNumber(String name) {
        if (name == null || !name.startsWith("map_")) return -1;
        try {
            return Integer.parseInt(name.substring(4));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static synchronized Detached detached(MapData legacy, boolean create) {
        Detached d = DETACHED.get(legacy);
        if (d == null && create) {
            var dim = legacy.dimension == -1 ? net.minecraft.world.level.Level.NETHER
                    : legacy.dimension == 1 ? net.minecraft.world.level.Level.END : net.minecraft.world.level.Level.OVERWORLD;
            var data = net.minecraft.world.level.saveddata.maps.MapItemSavedData.createForClient(legacy.scale, false, dim);
            d = new Detached(new net.minecraft.world.level.saveddata.maps.MapId(nextPrivateId--), data);
            DETACHED.put(legacy, d);
        }
        return d;
    }

    /** Copies the legacy colours and icons into the modern client map (the renderer re-uploads it when the colours changed). */
    private static boolean fill(MapData legacy, net.minecraft.world.level.saveddata.maps.MapItemSavedData data) {
        boolean changed = !java.util.Arrays.equals(legacy.colors, data.colors);
        if (changed) System.arraycopy(legacy.colors, 0, data.colors, 0, Math.min(legacy.colors.length, data.colors.length));
        java.util.List<net.minecraft.world.level.saveddata.maps.MapDecoration> decorations = new java.util.ArrayList<>();
        for (MapDecoration dec : legacy.mapDecorations.values()) {
            if (dec == null || dec.getType() == null) continue;
            decorations.add(new net.minecraft.world.level.saveddata.maps.MapDecoration(dec.getType().mcc$modern(), dec.getX(), dec.getY(), dec.getRotation(),
                    java.util.Optional.empty()));
        }
        data.addClientSideDecorations(decorations);
        return changed;
    }

    public void clearLoadedMaps() {
        synchronized (MapItemRenderer.class) {
            DETACHED.clear();
        }
        synchronized (loadedMaps) {
            for (Instance i : loadedMaps.values()) i.mapTexture.deleteGlTexture();
            loadedMaps.clear();
        }
    }

    // ------------------------------------------------------------------ 1.12's per-map texture instances (mods reading map pixels)

    private final Map<String, Instance> loadedMaps = new java.util.HashMap<>();

    /** The map's texture instance, made (and filled from its colours) on first use. */
    public Instance getMapRendererInstance(MapData mapdataIn) {
        synchronized (loadedMaps) {
            Instance instance = loadedMaps.get(mapdataIn.mapName);
            if (instance == null) {
                instance = new Instance(mapdataIn);
                loadedMaps.put(mapdataIn.mapName, instance);
            }
            return instance;
        }
    }

    public Instance getMapInstanceIfExists(String name) {
        synchronized (loadedMaps) {
            return loadedMaps.get(name);
        }
    }

    public MapData getData(Instance instance) {
        return instance != null ? instance.mapData : null;
    }

    /** A map's 128x128 texture: the map colours as pixels (transparent checkered where nothing was mapped). */
    public class Instance {
        private final MapData mapData;
        public final legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.DynamicTexture mapTexture;
        public final int[] mapTextureData;

        Instance(MapData mapdataIn) {
            this.mapData = mapdataIn;
            this.mapTexture = new legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.DynamicTexture(128, 128);
            this.mapTextureData = mapTexture.getTextureData();
            updateMapTexture();
        }

        void updateMapTexture() {
            for (int i = 0; i < 16384 && i < mapTextureData.length; ++i) {
                int j = mapData.colors[i] & 255;
                var color = legacyconnector.v1_12_2.net.minecraft.block.material.MapColor.COLORS[j / 4];
                mapTextureData[i] = j / 4 == 0 || color == null ? (i + i / 128 & 1) * 8 + 16 << 24 : color.getMapColor(j & 3);
            }
            mapTexture.updateDynamicTexture();
        }
    }
}
