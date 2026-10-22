package legacyconnector.v1_7_10.cpw.mods.fml.client.registry;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class ClientRegistry {
    public static void bindTileEntitySpecialRenderer(Class tileEntityClass, TileEntitySpecialRenderer renderer) {
        TileEntityRendererDispatcher.instance.mapSpecialRenderers.put(tileEntityClass, renderer);
        renderer.func_147497_a(TileEntityRendererDispatcher.instance);
    }

    public static void registerKeyBinding(legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding key) {
        if (key != null) legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding.mcc$register(key);
    }
}
