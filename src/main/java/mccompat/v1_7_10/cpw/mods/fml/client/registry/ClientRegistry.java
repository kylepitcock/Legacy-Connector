package mccompat.v1_7_10.cpw.mods.fml.client.registry;

import mccompat.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import mccompat.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class ClientRegistry {
    public static void bindTileEntitySpecialRenderer(Class tileEntityClass, TileEntitySpecialRenderer renderer) {
        TileEntityRendererDispatcher.instance.mapSpecialRenderers.put(tileEntityClass, renderer);
        renderer.func_147497_a(TileEntityRendererDispatcher.instance);
    }

    public static void registerKeyBinding(Object key) {
    }
}
