package legacyconnector.v1_12_2.net.minecraft.block;

import com.google.common.base.Predicates;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockWorldState;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.block.state.pattern.BlockPattern;
import legacyconnector.v1_12_2.net.minecraft.block.state.pattern.BlockStateMatcher;
import legacyconnector.v1_12_2.net.minecraft.block.state.pattern.FactoryBlockPattern;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.init.Blocks;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 end portal frame: a facing and whether it holds an eye of ender; twelve filled frames around a 3x3 hole make the portal. */
public class BlockEndPortalFrame extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool EYE = PropertyBool.create("eye");
    protected static final AxisAlignedBB AABB_BLOCK = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.8125D, 1.0D);
    protected static final AxisAlignedBB AABB_EYE = new AxisAlignedBB(0.3125D, 0.8125D, 0.3125D, 0.6875D, 1.0D, 0.6875D);
    private static BlockPattern portalShape;

    public BlockEndPortalFrame() {
        super(Material.ROCK, MapColor.GREEN);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(EYE, false));
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB_BLOCK;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
            Entity entityIn, boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_BLOCK);
        if (worldIn.getBlockState(pos).getValue(EYE)) addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_EYE);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.AIR;
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(EYE, false);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        return blockState.getValue(EYE) ? 15 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(EYE, (meta & 4) != 0).withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(EYE)) i |= 4;
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
        return new BlockStateContainer(this, FACING, EYE);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** The shape of a complete portal frame ring (every frame facing inward with an eye). */
    public static BlockPattern getOrCreatePortalShape() {
        if (portalShape == null) {
            portalShape = FactoryBlockPattern.start().aisle("?vvv?", ">???<", ">???<", ">???<", "?^^^?")
                    .where('?', BlockWorldState.hasState(BlockStateMatcher.ANY))
                    .where('^', BlockWorldState.hasState(frame(EnumFacing.SOUTH)))
                    .where('>', BlockWorldState.hasState(frame(EnumFacing.WEST)))
                    .where('v', BlockWorldState.hasState(frame(EnumFacing.NORTH)))
                    .where('<', BlockWorldState.hasState(frame(EnumFacing.EAST))).build();
        }
        return portalShape;
    }

    private static BlockStateMatcher frame(EnumFacing facing) {
        return BlockStateMatcher.forBlock(Blocks.END_PORTAL_FRAME).where(EYE, Predicates.equalTo(true)).where(FACING, Predicates.equalTo(facing));
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }
}
