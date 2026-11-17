package legacyconnector.v1_6_4.facade.net.minecraft.client.gui;

/** Facade of the 1.7.10 shim's {@code MapItemRenderer} for 1.5.2 and 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class MapItemRenderer {
    private MapItemRenderer() {}

    /**
     * {@code renderMap(player, textureManager, data)}: draws the map's canvas (the player and texture manager arguments were only used to bind the
     * canvas texture and mark the player's own marker, which 1.7.10 keeps in the map data and its own dynamic texture).
     */
    public static void renderMap(legacyconnector.v1_7_10.net.minecraft.client.gui.MapItemRenderer self,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager textures,
            legacyconnector.v1_7_10.net.minecraft.world.storage.MapData data) {
        self.func_148250_a(data, false);
    }
}
