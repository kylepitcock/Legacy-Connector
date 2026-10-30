package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_12_2.net.minecraft.world.storage.MapData;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2 map renderer ({@code EntityRenderer.getMapItemRenderer()}): a map's 128x128 picture and decorations at the current matrix,
 * drawn by the modern map renderer from the client's copy of the map ({@code map_N}); the modern renderer keeps the textures up to date.
 */
public class MapItemRenderer {
    public MapItemRenderer(TextureManager textureManagerIn) {
    }

    /** The modern map renderer refreshes a map's texture whenever its data changed. */
    public void updateMapTexture(MapData mapdataIn) {
    }

    public void renderMap(MapData mapdataIn, boolean noOverlayRendering) {
        if (mapdataIn == null) return;
        var pose = RenderContext.pose();
        var buffers = RenderContext.buffers();
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (pose == null || buffers == null || mc.level == null) return;
        var id = mcc$id(mapdataIn.mapName);
        if (id == null) return;
        var data = mc.level.getMapData(id);
        if (data == null) return;
        try {
            mc.gameRenderer.getMapRenderer().render(pose, buffers, id, data, noOverlayRendering, RenderContext.vertexLight());
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing a map", e);
        }
    }

    /** {@code map_12} is map id 12. */
    private static net.minecraft.world.level.saveddata.maps.MapId mcc$id(String name) {
        if (name == null) return null;
        int underscore = name.lastIndexOf('_');
        try {
            return new net.minecraft.world.level.saveddata.maps.MapId(Integer.parseInt(name.substring(underscore + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void clearLoadedMaps() {
        net.minecraft.client.Minecraft.getInstance().gameRenderer.getMapRenderer().resetData();
    }
}
