package legacyconnector.v1_7_10.net.minecraftforge.common;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom;

/**
 * Forge 1.7.10's static hook collection. Only the state mods reach into directly lives here (the seed list tall grass
 * drops from); behaviour hooks the engine drives are not part of the shim.
 */
public class ForgeHooks {
    /** Seeds tall grass can drop (mods add their own through {@code MinecraftForge.addGrassSeed}). */
    static final List<SeedEntry> seedList = new ArrayList<>();

    public static class SeedEntry extends WeightedRandom.Item {
        public final ItemStack seed;

        public SeedEntry(ItemStack seed, int weight) {
            super(weight);
            this.seed = seed;
        }

        public ItemStack getStack(java.util.Random rand) {
            return seed.copy();
        }
    }

    /** Whether the tool in {@code stack} is the kind that breaks {@code block} (with this metadata) quickly. */
    public static boolean isToolEffective(ItemStack stack, legacyconnector.v1_7_10.net.minecraft.block.Block block, int metadata) {
        if (stack == null || stack.getItem() == null) return false;
        for (String toolClass : stack.getItem().getToolClasses(stack)) {
            if (block.isToolEffective(toolClass, metadata)) return true;
        }
        return false;
    }

    /** As Forge: a block that needs no tool is harvestable by hand; otherwise the held tool's harvest level must reach the block's. */
    public static boolean canHarvestBlock(legacyconnector.v1_7_10.net.minecraft.block.Block block,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, int metadata) {
        if (block.getMaterial().isToolNotRequired()) return true;
        ItemStack stack = player.inventory.getCurrentItem();
        String tool = block.getHarvestTool(metadata);
        if (stack == null || tool == null) return player.canHarvestBlock(block);
        int toolLevel = stack.getItem().getHarvestLevel(stack, tool);
        if (toolLevel < 0) return player.canHarvestBlock(block);
        return toolLevel >= block.getHarvestLevel(metadata);
    }

    /** How fast the player breaks the block: 0 for unbreakable blocks, slower by 3x when the tool cannot harvest it. */
    public static float blockStrength(legacyconnector.v1_7_10.net.minecraft.block.Block block,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        float hardness = block.getBlockHardness(world, x, y, z);
        if (hardness < 0.0F) return 0.0F;
        if (!canHarvestBlock(block, player, metadata)) return player.getBreakSpeed(block, true, metadata, x, y, z) / hardness / 100.0F;
        return player.getBreakSpeed(block, false, metadata, x, y, z) / hardness / 30.0F;
    }

    public static float onLivingFall(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, float distance) {
        return distance;
    }

    public static void onLivingJump(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity) {
    }

    public static float getEnchantPower(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return world.getBlock(x, y, z).getEnchantPowerBonus(world, x, y, z);
    }

    public static boolean canToolHarvestBlock(legacyconnector.v1_7_10.net.minecraft.block.Block block, int metadata, ItemStack stack) {
        if (block.getMaterial().isToolNotRequired()) return true;
        return stack != null && stack.getItem() != null && stack.getItem().canHarvestBlock(block, stack);
    }
}
