package legacyconnector.v1_7_10.net.minecraft.src;

import legacyconnector.v1_7_10.cpw.mods.fml.common.Loader;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;

/**
 * The part of ModLoader (Risugami's mod API, which Forge 1.5.2 shipped as {@code net.minecraft.src.ModLoader}) that Forge mods call into:
 * the registration helpers, which forward to the FML registry, and {@code isModLoaded}. Mods written against {@code BaseMod} need more
 * (the lifecycle hooks, the {@code BaseMod} class), which is a separate layer.
 */
public class ModLoader {
    private ModLoader() {}

    /** True for a loaded mod of that name (ModLoader asked its BaseMod list; Forge's wrapper also answered for FML mods). */
    public static boolean isModLoaded(String name) {
        return Loader.isModLoaded(name);
    }

    /** The client (Forge's wrapper handed out the game instance); only meaningful on the client. */
    public static legacyconnector.v1_7_10.net.minecraft.client.Minecraft getMinecraftInstance() {
        return legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft();
    }

    public static void registerBlock(Block block) {
        registerBlock(block, null);
    }

    public static void registerBlock(Block block, Class<? extends ItemBlock> itemClass) {
        String name = block.getUnlocalizedName();
        if (name != null && name.startsWith("tile.")) name = name.substring(5);
        if (name == null || name.isEmpty() || name.equals("null")) name = "block" + Block.getIdFromBlock(block);
        GameRegistry.registerBlock(block, itemClass == null ? ItemBlock.class : itemClass, name);
    }

    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, String id) {
        GameRegistry.registerTileEntity(tileEntityClass, id);
    }

    public static void addRecipe(ItemStack output, Object... params) {
        GameRegistry.addRecipe(output, params);
    }

    public static void addShapelessRecipe(ItemStack output, Object... params) {
        GameRegistry.addShapelessRecipe(output, params);
    }

    /** Display names came from the mod's language files in the shim; ModLoader's name table has no effect. */
    public static void addName(Object instance, String name) {
    }

    public static void addLocalization(String key, String value) {
    }
}
