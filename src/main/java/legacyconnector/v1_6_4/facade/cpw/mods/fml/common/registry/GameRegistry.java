package legacyconnector.v1_6_4.facade.cpw.mods.fml.common.registry;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;

/**
 * Facade of the 1.7.10 shim's {@code GameRegistry} for 1.6.4 mods: the registration calls that returned nothing (1.7.10's return the
 * registered block), and the id-keyed smelting recipes. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class GameRegistry {
    private GameRegistry() {}

    public static void registerBlock(Block block, String name) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerBlock(block, name);
    }

    public static void registerBlock(Block block, Class<? extends ItemBlock> itemClass, String name) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerBlock(block, itemClass, name);
    }

    /** {@code registerBlock(block, itemClass, name, modId)}: 1.6.4 returned nothing and took the owning mod id as the fourth argument. */
    public static void registerBlock(Block block, Class<? extends ItemBlock> itemClass, String name, String modId) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerBlock(block, itemClass, name, modId);
    }

    /** {@code registerWorldGenerator(generator)}: no weight argument in 1.6.4. */
    public static void registerWorldGenerator(legacyconnector.v1_7_10.cpw.mods.fml.common.IWorldGenerator generator) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerWorldGenerator(generator, 0);
    }

    /** {@code registerBlock(block)}: the name is the unlocalized name without the {@code tile.} prefix. */
    public static void registerBlock(Block block) {
        String n = block.getUnlocalizedName();
        if (n == null) n = "block" + legacyconnector.v1_6_4.net.minecraft.util.Ids.blockId(block);
        if (n.startsWith("tile.")) n = n.substring(5);
        registerBlock(block, n);
    }

    /** {@code addSmelting(itemId, output, xp)}: any damage of the item smelts. */
    public static void addSmelting(int id, legacyconnector.v1_7_10.net.minecraft.item.ItemStack output, float xp) {
        Item item = legacyconnector.v1_6_4.net.minecraft.util.Ids.item(id);
        if (item != null) {
            legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.addSmelting(
                    new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, 1, 32767), output, xp);
        }
    }

    public static void registerItem(Item item, String name, String modId) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerItem(item, name, modId);
    }
}
