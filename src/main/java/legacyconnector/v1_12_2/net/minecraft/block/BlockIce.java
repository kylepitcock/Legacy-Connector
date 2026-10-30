package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.EnumSkyBlock;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 ice: slippery, translucent, melts in block light above 11 and turns to water when broken over a solid or liquid block. */
public class BlockIce extends BlockBreakable {
    public BlockIce() {
        super(Material.ICE, false);
        this.slipperiness = 0.98F;
        setTickRandomly(true);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack) {
        if (legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                legacyconnector.v1_12_2.net.minecraft.init.Enchantments.SILK_TOUCH, stack) > 0) {
            super.harvestBlock(worldIn, player, pos, state, te, stack);
            return;
        }
        if (World.mcc$get$provider(worldIn) != null && World.mcc$get$provider(worldIn).doesWaterVaporize()) {
            worldIn.setBlockToAir(pos);
            return;
        }
        int fortune = legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                legacyconnector.v1_12_2.net.minecraft.init.Enchantments.FORTUNE, stack);
        harvesters.set(player);
        dropBlockAsItem(worldIn, pos, state, fortune);
        harvesters.set(null);
        Material below = worldIn.getBlockState(pos.down()).getMaterial();
        if (below.blocksMovement() || below.isLiquid()) worldIn.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.FLOWING_WATER.getDefaultState());
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.getLightFor(EnumSkyBlock.BLOCK, pos) > 11 - getDefaultState().getLightOpacity()) turnIntoWater(worldIn, pos);
    }

    /** Melts into water (or vanishes where water boils away). */
    protected void turnIntoWater(World worldIn, BlockPos pos) {
        if (World.mcc$get$provider(worldIn) != null && World.mcc$get$provider(worldIn).doesWaterVaporize()) {
            worldIn.setBlockToAir(pos);
            return;
        }
        dropBlockAsItem(worldIn, pos, worldIn.getBlockState(pos), 0);
        worldIn.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.WATER.getDefaultState());
        worldIn.neighborChanged(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.WATER, pos);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction getPushReaction(IBlockState state) {
        return legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction.NORMAL;
    }
}
