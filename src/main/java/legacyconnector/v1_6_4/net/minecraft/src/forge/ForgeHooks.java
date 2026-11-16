package legacyconnector.v1_6_4.net.minecraft.src.forge;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** The parts of Forge 3.x's hook class that mods call themselves (the game's own hook points are the shim's events). */
public class ForgeHooks {
    public static final int majorVersion = 3;
    public static final int minorVersion = 3;
    public static final int revisionVersion = 7;
    public static final int buildVersion = 135;

    public ForgeHooks() {}

    public static boolean canHarvestBlock(Block block, EntityPlayer player, int metadata) {
        return player.canHarvestBlock(block);
    }

    public static boolean isToolEffective(ItemStack stack, Block block, int metadata) {
        return stack != null && stack.getItem() != null && stack.getItem().getDigSpeed(stack, block, metadata) > 1.0F;
    }

    public static float blockStrength(Block block, EntityPlayer player, int metadata) {
        return player.mcc$breakSpeed(block, metadata);
    }

    public static int getMajorVersion() {
        return majorVersion;
    }

    public static int getMinorVersion() {
        return minorVersion;
    }

    public static int getRevisionVersion() {
        return revisionVersion;
    }

    public static int getBuildVersion() {
        return buildVersion;
    }

    /** Forge 3.x's furnace fuel query: the longest burn time the registered fuel handlers give the item. */
    public static int getItemBurnTime(ItemStack item) {
        return legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.getFuelValue(item);
    }

    public static boolean onUseBonemeal(legacyconnector.v1_7_10.net.minecraft.world.World world, int blockId, int x, int y, int z) {
        for (IBonemealHandler h : MinecraftForge.BONEMEAL) if (h.onUseBonemeal(world, blockId, x, y, z)) return true;
        return false;
    }

    public static void plantGrassPlant(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        legacyconnector.v1_7_10.net.minecraftforge.common.ForgeHooks.plantGrass(world, x, y, z);
    }

    public static void onTakenFromCrafting(EntityPlayer player, ItemStack item, legacyconnector.v1_7_10.net.minecraft.inventory.IInventory matrix) {
        for (ICraftingHandler h : MinecraftForge.CRAFTING) h.onTakenFromCrafting(player, item, matrix);
    }

    public static boolean onMinecartInteract(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart cart, EntityPlayer player) {
        boolean handled = false;
        for (IMinecartHandler h : MinecraftForge.MINECART) handled |= h.onMinecartInteract(cart, player, true);
        return handled;
    }

    public static ItemStack onArrowNock(ItemStack bow, legacyconnector.v1_7_10.net.minecraft.world.World world, EntityPlayer player) {
        for (IArrowNockHandler h : MinecraftForge.ARROW_NOCK) {
            ItemStack r = h.onArrowNock(bow, world, player);
            if (r != null) return r;
        }
        return bow;
    }

    public static boolean onArrowLoose(ItemStack bow, legacyconnector.v1_7_10.net.minecraft.world.World world, EntityPlayer player, int charge) {
        for (IArrowLooseHandler h : MinecraftForge.ARROW_LOOSE) if (h.onArrowLoose(bow, world, player, charge)) return true;
        return false;
    }

    public static boolean onItemPickup(EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem item) {
        return false;
    }
}
