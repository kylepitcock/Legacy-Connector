package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's piston head: facing bits 0-2, sticky bit 3; breaking it breaks the piston behind. */
public class BlockPistonExtension extends BlockDirectional {
    public static final PropertyEnum<EnumPistonType> TYPE = PropertyEnum.create("type", EnumPistonType.class);
    public static final PropertyBool SHORT = PropertyBool.create("short");

    public BlockPistonExtension() {
        super(Material.PISTON);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(TYPE, EnumPistonType.DEFAULT).withProperty(SHORT, false));
        setSoundType(SoundType.STONE);
        setHardness(0.5F);
    }

    @Override
    public boolean isTopSolid(IBlockState state) {
        return state.getValue(FACING) == EnumFacing.UP;
    }

    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (player.isCreative()) {
            BlockPos base = pos.offset(state.getValue(FACING).getOpposite());
            if (worldIn.getBlockState(base).getBlock() instanceof BlockPistonBase) worldIn.setBlockToAir(base);
        }
        super.onBlockHarvested(worldIn, pos, state, player);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        EnumFacing facing = state.getValue(FACING).getOpposite();
        BlockPos base = pos.offset(facing);
        IBlockState baseState = worldIn.getBlockState(base);
        if (baseState.getBlock() instanceof BlockPistonBase && baseState.getValue(BlockPistonBase.EXTENDED)) {
            baseState.getBlock().dropBlockAsItem(worldIn, base, baseState, 0);
            worldIn.setBlockToAir(base);
        }
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
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return false;
    }

    @Override
    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        BlockPos base = pos.offset(state.getValue(FACING).getOpposite());
        IBlockState baseState = worldIn.getBlockState(base);
        if (!(baseState.getBlock() instanceof BlockPistonBase)) worldIn.setBlockToAir(pos);
        else baseState.neighborChanged(worldIn, base, blockIn, fromPos);
    }

    public static EnumFacing getFacing(int meta) {
        int i = meta & 7;
        return i > 5 ? null : EnumFacing.byIndex(i);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(state.getValue(TYPE) == EnumPistonType.STICKY ? legacyconnector.v1_12_2.net.minecraft.init.Blocks.STICKY_PISTON
                : legacyconnector.v1_12_2.net.minecraft.init.Blocks.PISTON);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, getFacing(meta)).withProperty(TYPE, (meta & 8) > 0 ? EnumPistonType.STICKY : EnumPistonType.DEFAULT);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = state.getValue(FACING).getIndex();
        if (state.getValue(TYPE) == EnumPistonType.STICKY) i |= 8;
        return i;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, TYPE, SHORT);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == state.getValue(FACING) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    public enum EnumPistonType implements IStringSerializable {
        DEFAULT("normal"),
        STICKY("sticky");

        private final String variantName;

        EnumPistonType(String name) {
            this.variantName = name;
        }

        @Override
        public String toString() {
            return variantName;
        }

        @Override
        public String getName() {
            return variantName;
        }
    }
}
