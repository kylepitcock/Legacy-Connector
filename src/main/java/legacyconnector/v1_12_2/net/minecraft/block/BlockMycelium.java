package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 mycelium: snowy under snow, dies to dirt in the dark, spreads onto lit dirt nearby, drops dirt. */
public class BlockMycelium extends Block {
    public static final PropertyBool SNOWY = PropertyBool.create("snowy");

    protected BlockMycelium() {
        super(Material.GRASS, MapColor.PURPLE);
        setDefaultState(blockState.getBaseState().withProperty(SNOWY, false));
        setTickRandomly(true);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        Block above = worldIn.getBlockState(pos.up()).getBlock();
        return state.withProperty(SNOWY, Block.mcc$named(above, "snow", "snow_layer"));
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.isRemote || !worldIn.isAreaLoaded(pos, 2)) return;
        BlockPos up = pos.up();
        if (worldIn.getLightFromNeighbors(up) < 4 && worldIn.getBlockState(up).getLightOpacity(worldIn, up) > 2) {
            worldIn.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.DIRT.getDefaultState());
        } else if (worldIn.getLightFromNeighbors(up) >= 9) {
            for (int i = 0; i < 4; ++i) {
                BlockPos p = pos.add(rand.nextInt(3) - 1, rand.nextInt(5) - 3, rand.nextInt(3) - 1);
                IBlockState target = worldIn.getBlockState(p);
                IBlockState above = worldIn.getBlockState(p.up());
                if (target.getBlock() == legacyconnector.v1_12_2.net.minecraft.init.Blocks.DIRT && target.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.DIRT
                        && worldIn.getLightFromNeighbors(p.up()) >= 4 && above.getLightOpacity(worldIn, p.up()) <= 2) {
                    worldIn.setBlockState(p, getDefaultState());
                }
            }
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(legacyconnector.v1_12_2.net.minecraft.init.Blocks.DIRT);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, SNOWY);
    }
}
