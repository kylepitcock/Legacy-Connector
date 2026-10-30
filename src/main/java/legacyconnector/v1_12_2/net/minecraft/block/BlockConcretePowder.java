package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's concrete powder: falls, sixteen colours by metadata, hardens into concrete next to water. */
public class BlockConcretePowder extends BlockFalling {
    public static final PropertyEnum<EnumDyeColor> COLOR = PropertyEnum.create("color", EnumDyeColor.class);

    public BlockConcretePowder() {
        super(Material.SAND);
        setDefaultState(blockState.getBaseState().withProperty(COLOR, EnumDyeColor.WHITE));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    /** Hardens when water touches any side but the bottom; true when it did. */
    protected boolean tryTouchWater(World worldIn, BlockPos pos, IBlockState state) {
        for (EnumFacing f : EnumFacing.values()) {
            if (f != EnumFacing.DOWN && worldIn.getBlockState(pos.offset(f)).getMaterial() == Material.WATER) {
                worldIn.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.CONCRETE.getStateFromMeta(state.getValue(COLOR).getMetadata()), 3);
                return true;
            }
        }
        return false;
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!tryTouchWater(worldIn, pos, state)) super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!tryTouchWater(worldIn, pos, state)) super.onBlockAdded(worldIn, pos, state);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(COLOR).getMetadata();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (EnumDyeColor c : EnumDyeColor.values()) items.add(new ItemStack(this, 1, c.getMetadata()));
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return MapColor.getBlockColor(state.getValue(COLOR));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(COLOR, EnumDyeColor.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(COLOR).getMetadata();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, COLOR);
    }
}
