package legacyconnector.v1_7_10.cpw.mods.fml.client.registry;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class ClientRegistry {
    /** Registers a tile entity class under an id and binds its special renderer in one call. */
    public static void registerTileEntity(Class tileEntityClass, String id, TileEntitySpecialRenderer renderer) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerTileEntity(tileEntityClass, id);
        bindTileEntitySpecialRenderer(tileEntityClass, renderer);
    }

    public static void bindTileEntitySpecialRenderer(Class tileEntityClass, TileEntitySpecialRenderer renderer) {
        TileEntityRendererDispatcher.instance.mapSpecialRenderers.put(tileEntityClass, renderer);
        renderer.func_147497_a(TileEntityRendererDispatcher.instance);
    }

    public static void registerKeyBinding(legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding key) {
        if (key != null) legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding.mcc$register(key);
    }
}
