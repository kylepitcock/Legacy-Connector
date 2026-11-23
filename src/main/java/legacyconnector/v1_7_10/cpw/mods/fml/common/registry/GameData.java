package legacyconnector.v1_7_10.cpw.mods.fml.common.registry;

import legacyconnector.v1_7_10.cpw.mods.fml.common.Loader;
import legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;

/** FML's registry access point. */
public class GameData {
    /** FML's (deprecated) direct handles on the two registries, the same objects as {@link #getBlockRegistry}/{@link #getItemRegistry}. */
    public static final FMLControlledNamespacedRegistry<Block> blockRegistry = getBlockRegistry();
    public static final FMLControlledNamespacedRegistry<Item> itemRegistry = getItemRegistry();

    @SuppressWarnings("unchecked")
    public static FMLControlledNamespacedRegistry<Block> getBlockRegistry() {
        return (FMLControlledNamespacedRegistry<Block>) Block.blockRegistry;
    }

    @SuppressWarnings("unchecked")
    public static FMLControlledNamespacedRegistry<Item> getItemRegistry() {
        return (FMLControlledNamespacedRegistry<Item>) Item.itemRegistry;
    }

    private static final GameData MAIN = new GameData();

    /** The main (active) registry set. */
    public static GameData getMain() {
        return MAIN;
    }

    /** Registers an item under a name at a wanted id; ids are not numbered in the layer, so the wanted id is returned. */
    public static int registerItem(Item item, String name, int idHint) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerItem(item, name);
        return idHint;
    }

    /** FML froze the registries after loading and reverted to the frozen copy on a failed world join; the host owns that cycle here. */
    public static void freezeData() {
    }

    public static void revertToFrozen() {
    }

    /** Writes the registry contents to a file (a debugging aid of FML's): there is no id table to dump. */
    public static void dumpRegistry(java.io.File minecraftDir) {
    }

    /** Owner of a "modid:name" registry name. */
    public static ModContainer findModOwner(String name) {
        int colon = name.indexOf(':');
        String mod = colon < 0 ? "minecraft" : name.substring(0, colon);
        return Loader.instance().getIndexedModList().get(mod);
    }
}
