package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 grass blocks (mods' grasses extend it): snowy under snow, die to dirt in the dark, spread themselves onto lit dirt nearby, drop
 * dirt, and grow tall grass around under bone meal.
 */
public class BlockGrass extends Block implements IGrowable {
    public static final PropertyBool SNOWY = PropertyBool.create("snowy");

    protected BlockGrass() {
        super(Material.GRASS);
        setDefaultState(blockState.getBaseState().withProperty(SNOWY, false));
        setTickRandomly(true);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        Block block = worldIn.getBlockState(pos.up()).getBlock();
        return state.withProperty(SNOWY, Block.mcc$named(block, "snow", "snow_layer", "snow_block"));
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.isRemote || !worldIn.isAreaLoaded(pos, 3)) return;
        BlockPos up = pos.up();
        if (worldIn.getLightFromNeighbors(up) < 4 && worldIn.getBlockState(up).getLightOpacity(worldIn, up) > 2) {
            worldIn.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.DIRT.getDefaultState());
        } else if (worldIn.getLightFromNeighbors(up) >= 9) {
            for (int i = 0; i < 4; ++i) {
                BlockPos p = pos.add(rand.nextInt(3) - 1, rand.nextInt(5) - 3, rand.nextInt(3) - 1);
                if (p.getY() >= 0 && p.getY() < 256 && !worldIn.isBlockLoaded(p)) return;
                IBlockState above = worldIn.getBlockState(p.up());
                IBlockState target = worldIn.getBlockState(p);
                if (isPlainDirt(worldIn, p, target) && worldIn.getLightFromNeighbors(p.up()) >= 4 && above.getLightOpacity(worldIn, p.up()) <= 2) {
                    worldIn.setBlockState(p, getDefaultState());
                }
            }
        }
    }

    /** Plain dirt (not coarse dirt or podzol). */
    private static boolean isPlainDirt(World world, BlockPos pos, IBlockState state) {
        if (!Block.mcc$named(state.getBlock(), "dirt")) return false;
        var level = world.mcc$level();
        return level == null || level.getBlockState(org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos)).is(net.minecraft.world.level.block.Blocks.DIRT);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(legacyconnector.v1_12_2.net.minecraft.init.Blocks.DIRT);
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    /** Bone meal: tall grass (and now and then a flower) on the grass around, by random walks as vanilla does. */
    @Override
    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        var level = worldIn.mcc$level();
        if (level == null) return;
        BlockPos up = pos.up();
        for (int i = 0; i < 128; ++i) {
            BlockPos p = up;
            int j = 0;
            boolean placed = true;
            while (j < i / 16) {
                p = p.add(rand.nextInt(3) - 1, (rand.nextInt(3) - 1) * rand.nextInt(3) / 2, rand.nextInt(3) - 1);
                if (worldIn.getBlockState(p.down()).getBlock() != this || worldIn.getBlockState(p).isNormalCube()) {
                    placed = false;
                    break;
                }
                ++j;
            }
            if (!placed || !worldIn.isAirBlock(p)) continue;
            var mp = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(p);
            var plant = rand.nextInt(8) == 0 ? (rand.nextBoolean() ? net.minecraft.world.level.block.Blocks.DANDELION : net.minecraft.world.level.block.Blocks.POPPY)
                    : net.minecraft.world.level.block.Blocks.SHORT_GRASS;
            level.setBlock(mp, plant.defaultBlockState(), 3);
        }
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
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
