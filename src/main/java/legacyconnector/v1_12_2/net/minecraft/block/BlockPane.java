package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 panes and bars: a thin post that reaches out to solid faces, other panes and glass around it. */
public class BlockPane extends Block {
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    /** Boxes by connections (bit per horizontal index: south 1, west 2, north 4, east 8). */
    protected static final AxisAlignedBB[] AABB_BY_INDEX = new AxisAlignedBB[16];

    static {
        for (int i = 0; i < 16; i++) {
            boolean s = (i & 1) != 0, w = (i & 2) != 0, n = (i & 4) != 0, e = (i & 8) != 0;
            AABB_BY_INDEX[i] = new AxisAlignedBB(w ? 0.0D : 0.4375D, 0.0D, n ? 0.0D : 0.4375D, e ? 1.0D : 0.5625D, 1.0D, s ? 1.0D : 0.5625D);
        }
    }

    private final boolean canDrop;

    protected BlockPane(Material materialIn, boolean canDrop) {
        super(materialIn);
        setDefaultState(blockState.getBaseState().withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false).withProperty(WEST, false));
        this.canDrop = canDrop;
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.DECORATIONS);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
            Entity entityIn, boolean isActualState) {
        if (!isActualState) state = getActualState(state, worldIn, pos);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_BY_INDEX[0]);
        if (state.getValue(NORTH)) addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_BY_INDEX[getBoundingBoxIndex(EnumFacing.NORTH)]);
        if (state.getValue(SOUTH)) addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_BY_INDEX[getBoundingBoxIndex(EnumFacing.SOUTH)]);
        if (state.getValue(EAST)) addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_BY_INDEX[getBoundingBoxIndex(EnumFacing.EAST)]);
        if (state.getValue(WEST)) addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_BY_INDEX[getBoundingBoxIndex(EnumFacing.WEST)]);
    }

    private static int getBoundingBoxIndex(EnumFacing side) {
        return 1 << side.getHorizontalIndex();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        state = getActualState(state, source, pos);
        return AABB_BY_INDEX[getBoundingBoxIndex(state)];
    }

    private static int getBoundingBoxIndex(IBlockState state) {
        int i = 0;
        if (state.getValue(NORTH)) i |= getBoundingBoxIndex(EnumFacing.NORTH);
        if (state.getValue(EAST)) i |= getBoundingBoxIndex(EnumFacing.EAST);
        if (state.getValue(SOUTH)) i |= getBoundingBoxIndex(EnumFacing.SOUTH);
        if (state.getValue(WEST)) i |= getBoundingBoxIndex(EnumFacing.WEST);
        return i;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state.withProperty(NORTH, canPaneConnectTo(worldIn, pos, EnumFacing.NORTH)).withProperty(SOUTH, canPaneConnectTo(worldIn, pos, EnumFacing.SOUTH))
                .withProperty(WEST, canPaneConnectTo(worldIn, pos, EnumFacing.WEST)).withProperty(EAST, canPaneConnectTo(worldIn, pos, EnumFacing.EAST));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return !canDrop ? legacyconnector.v1_12_2.net.minecraft.init.Items.AIR : super.getItemDropped(state, rand, fortune);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    public final boolean canPaneConnectToBlock(Block blockIn) {
        return blockIn.getDefaultState().isFullCube() || blockIn == this || blockIn instanceof BlockPane
                || Block.mcc$named(blockIn, "glass", "stained_glass", "stained_glass_pane", "glass_pane", "iron_bars");
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
        return new BlockStateContainer(this, NORTH, EAST, WEST, SOUTH);
    }

    @Override
    public boolean canBeConnectedTo(IBlockAccess world, BlockPos pos, EnumFacing facing) {
        BlockPos other = pos.offset(facing);
        return attachesTo(world, world.getBlockState(other), other, facing.getOpposite());
    }

    /** Whether the pane joins the block on that side: solid faces, thin poles (other panes), and glass. */
    public boolean attachesTo(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing facing) {
        Block block = state.getBlock();
        BlockFaceShape shape = state.getBlockFaceShape(world, pos, facing);
        return !isExceptBlockForAttachWithPiston(block) && shape == BlockFaceShape.SOLID || shape == BlockFaceShape.MIDDLE_POLE_THIN
                || block instanceof BlockPane || Block.mcc$named(block, "glass", "stained_glass", "stained_glass_pane", "glass_pane", "iron_bars");
    }

    public boolean canPaneConnectTo(IBlockAccess world, BlockPos pos, EnumFacing dir) {
        BlockPos other = pos.offset(dir);
        IBlockState state = world.getBlockState(other);
        return state.getBlock().canBeConnectedTo(world, other, dir.getOpposite()) || attachesTo(world, state, other, dir.getOpposite());
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return face != EnumFacing.UP && face != EnumFacing.DOWN ? BlockFaceShape.MIDDLE_POLE_THIN : BlockFaceShape.CENTER_SMALL;
    }
}
