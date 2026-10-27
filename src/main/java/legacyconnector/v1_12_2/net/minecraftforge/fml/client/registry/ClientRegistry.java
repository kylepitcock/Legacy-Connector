package legacyconnector.v1_12_2.net.minecraftforge.fml.client.registry;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;

/** FML's client registrations: tile entity renderers and key bindings (they appear in the controls screen). */
public class ClientRegistry {
    /**
     * Binds a renderer to a tile entity class (subclasses inherit it, as vanilla looked renderers up): the modern renderer of the block
     * entity hosting a legacy tile entity draws through it ({@code LegacyBlockEntityRenderer1122}).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends TileEntity> void bindTileEntitySpecialRenderer(Class<T> tileEntityClass, TileEntitySpecialRenderer<? super T> specialRenderer) {
        if (tileEntityClass == null || specialRenderer == null) return;
        TileEntityRendererDispatcher.instance.mcc$register(tileEntityClass, (TileEntitySpecialRenderer) specialRenderer);
    }

    public static void registerKeyBinding(legacyconnector.v1_12_2.net.minecraft.client.settings.KeyBinding key) {
        if (key != null) legacyconnector.v1_12_2.net.minecraft.client.settings.KeyBinding.mcc$register(key);
    }
}
