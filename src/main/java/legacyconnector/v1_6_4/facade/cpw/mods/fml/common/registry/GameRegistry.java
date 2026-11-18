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

    /**
     * {@code registerBlock(block, itemClass, name, modId, itemCtorArgs...)}: the item block is built by the constructor that takes the block's id minus 256 followed by
     * the arguments ({@code ItemBlockCamoButton(int, Block)}); returned nothing in 1.6.4 (the call otherwise has no 1.7.10 return type to link to).
     */
    public static void registerBlock(Block block, Class<? extends ItemBlock> itemClass, String name, String modId, Object... itemCtorArgs) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerBlock(block, itemClass, name, modId, itemCtorArgs);
    }

    /** {@code registerBlock(block, itemClass, name, itemCtorArgs...)} (Forge 1.6.4 without the mod id). */
    public static void registerBlock(Block block, Class<? extends ItemBlock> itemClass, String name, Object... itemCtorArgs) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerBlock(block, itemClass, name, (String) null, itemCtorArgs);
    }

    /** {@code addBiome(biome)}: Forge 1.6.4 added the biome to the default world type's biome list, which the pack's biome placement then draws from. */
    public static void addBiome(legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase biome) {
        if (biome != null) legacyconnector.v1_7_10.net.minecraft.world.WorldType.DEFAULT.addNewBiome(biome);
    }

    /** {@code registerWorldGenerator(generator)}: no weight argument in 1.6.4. */
    public static void registerWorldGenerator(legacyconnector.v1_7_10.cpw.mods.fml.common.IWorldGenerator generator) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerWorldGenerator(generator, 0);
    }

    /** {@code registerBlock(block, itemClass)} (1.5.2): the name is the unlocalized name without {@code tile.}, or the block id for blocks that have none (Applied Energistics 1 names its subtypes per item stack). */
    public static void registerBlock(Block block, Class<? extends ItemBlock> itemClass) {
        String n = block.getUnlocalizedName();
        if (n != null && n.startsWith("tile.")) n = n.substring(5);
        if (n == null || n.isEmpty() || n.equals("null")) n = "block" + legacyconnector.v1_6_4.net.minecraft.util.Ids.blockId(block);
        registerBlock(block, itemClass, n);
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

    public static void registerCraftingHandler(legacyconnector.v1_6_4.cpw.mods.fml.common.ICraftingHandler handler) {
        org.kylepitcock.legacyconnector.v1_6_4.bridge.Crafting164.registerCraftingHandler(handler);
    }

    public static void registerPickupHandler(legacyconnector.v1_6_4.cpw.mods.fml.common.IPickupNotifier handler) {
        org.kylepitcock.legacyconnector.v1_6_4.bridge.Crafting164.registerPickupHandler(handler);
    }

    /** Mods call these when they craft, smelt or pick up outside the vanilla paths: the crafting handlers hear it through the event. */
    public static void onItemCrafted(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.item.ItemStack item,
            legacyconnector.v1_7_10.net.minecraft.inventory.IInventory craftMatrix) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler.instance().firePlayerCraftingEvent(player, item, craftMatrix);
    }

    public static void onItemSmelted(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.item.ItemStack item) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler.instance().firePlayerSmeltedEvent(player, item);
    }

    /** 1.4.7 and 1.5.2 {@code onPlayerChangedDimension(player)}: a mod that moved a player itself lets the registered player trackers know. */
    public static void onPlayerChangedDimension(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        org.kylepitcock.legacyconnector.v1_6_4.bridge.Players164.firePlayerChangedDimension(player);
    }

    public static void onPickupNotification(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem item) {
        legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler.instance().firePlayerItemPickupEvent(player, item);
    }

    /** {@code removeBiome(biome)}: Forge 1.6.4 took the biome off the default world type's biome list. */
    public static void removeBiome(legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase biome) {
        if (biome != null) legacyconnector.v1_7_10.net.minecraft.world.WorldType.DEFAULT.removeBiome(biome);
    }
}
