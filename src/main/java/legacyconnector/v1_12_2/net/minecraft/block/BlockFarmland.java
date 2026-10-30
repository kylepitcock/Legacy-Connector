package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.common.EnumPlantType;
import legacyconnector.v1_12_2.net.minecraftforge.common.IPlantable;

/** 1.12.2 farmland: moisture 0-7 (metadata), wet within four blocks of water, dries back to dirt with nothing planted. */
public class BlockFarmland extends Block {
    public static final PropertyInteger MOISTURE = PropertyInteger.create("moisture", 0, 7);
    protected static final AxisAlignedBB FARMLAND_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.9375D, 1.0D);
    /** The top sixteenth above the farmland's box (the part an entity standing on farmland would sink into). */
    protected static final AxisAlignedBB field_194405_c = new AxisAlignedBB(0.0D, 0.9375D, 0.0D, 1.0D, 1.0D, 1.0D);

    protected BlockFarmland() {
        super(Material.GROUND);
        setDefaultState(blockState.getBaseState().withProperty(MOISTURE, 0));
        setTickRandomly(true);
        setLightOpacity(255);
        this.useNeighborBrightness = true;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FARMLAND_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        int moisture = state.getValue(MOISTURE);
        if (!hasWater(worldIn, pos) && !worldIn.isRainingAt(pos.up())) {
            if (moisture > 0) worldIn.setBlockState(pos, state.withProperty(MOISTURE, moisture - 1), 2);
            else if (!hasCrops(worldIn, pos)) turnToDirt(worldIn, pos);
        } else if (moisture < 7) {
            worldIn.setBlockState(pos, state.withProperty(MOISTURE, 7), 2);
        }
    }

    protected static void turnToDirt(World world, BlockPos pos) {
        world.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.DIRT.getDefaultState());
    }

    private boolean hasCrops(World worldIn, BlockPos pos) {
        Block block = worldIn.getBlockState(pos.up()).getBlock();
        return block instanceof IPlantable p && canSustainPlant(worldIn.getBlockState(pos), worldIn, pos, EnumFacing.UP, p);
    }

    private boolean hasWater(World worldIn, BlockPos pos) {
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (worldIn.getBlockState(pos.add(x, y, z)).getMaterial() == Material.WATER) return true;
                }
            }
        }
        return false;
    }

    /** Crops and stems grow on farmland. */
    @Override
    public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction, IPlantable plantable) {
        return plantable.getPlantType(world, pos.offset(direction)) == EnumPlantType.Crop || super.canSustainPlant(state, world, pos, direction, plantable);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (worldIn.getBlockState(pos.up()).getMaterial().isSolid()) turnToDirt(worldIn, pos);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (worldIn.getBlockState(pos.up()).getMaterial().isSolid()) turnToDirt(worldIn, pos);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.init.Blocks.DIRT.getItemDropped(legacyconnector.v1_12_2.net.minecraft.init.Blocks.DIRT.getDefaultState(),
                rand, fortune);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(MOISTURE, meta & 7);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(MOISTURE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, MOISTURE);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }
}
