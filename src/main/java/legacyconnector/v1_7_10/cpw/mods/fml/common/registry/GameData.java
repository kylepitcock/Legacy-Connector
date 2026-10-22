package legacyconnector.v1_7_10.cpw.mods.fml.common.registry;

import legacyconnector.v1_7_10.cpw.mods.fml.common.Loader;
import legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;

/** FML's registry access point. */
public class GameData {
    @SuppressWarnings("unchecked")
    public static FMLControlledNamespacedRegistry<Block> getBlockRegistry() {
        return (FMLControlledNamespacedRegistry<Block>) Block.blockRegistry;
    }

    @SuppressWarnings("unchecked")
    public static FMLControlledNamespacedRegistry<Item> getItemRegistry() {
        return (FMLControlledNamespacedRegistry<Item>) Item.itemRegistry;
    }

    /** Owner of a "modid:name" registry name. */
    public static ModContainer findModOwner(String name) {
        int colon = name.indexOf(':');
        String mod = colon < 0 ? "minecraft" : name.substring(0, colon);
        return Loader.instance().getIndexedModList().get(mod);
    }
}
