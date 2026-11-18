package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.world.EnumSkyBlock;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** 1.12.2 snow block: drops four snowballs and melts in block light above 11. Mods extend it for their own snow-like blocks. */
public class BlockSnowBlock extends Block {
    protected BlockSnowBlock() {
        super(Material.CRAFTED_SNOW);
        setTickRandomly(true);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.SNOWBALL;
    }

    @Override
    public int quantityDropped(Random random) {
        return 4;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.getLightFor(EnumSkyBlock.BLOCK, pos) > 11) {
            dropBlockAsItem(worldIn, pos, worldIn.getBlockState(pos), 0);
            worldIn.setBlockToAir(pos);
        }
    }
}
