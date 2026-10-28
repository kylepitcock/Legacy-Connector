package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 walls: a post (dropped between two straight runs) reaching out to solid faces and other walls; 1.5 tall for collision. */
public class BlockWall extends Block {
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyEnum<EnumType> VARIANT = PropertyEnum.create("variant", EnumType.class);
    protected static final AxisAlignedBB[] AABB_BY_INDEX = boxes(1.0D);
    protected static final AxisAlignedBB[] CLIP_AABB_BY_INDEX = boxes(1.5D);

    /** Boxes by connections (south 1, west 2, north 4, east 8); the lone post is narrower than a run. */
    private static AxisAlignedBB[] boxes(double height) {
        AxisAlignedBB[] out = new AxisAlignedBB[16];
        for (int i = 0; i < 16; i++) {
            boolean s = (i & 1) != 0, w = (i & 2) != 0, n = (i & 4) != 0, e = (i & 8) != 0;
            double lo = i == 5 || i == 10 ? 0.3125D : 0.25D, hi = i == 5 || i == 10 ? 0.6875D : 0.75D;
            double h = height == 1.0D && (i == 5 || i == 10) ? 0.875D : height;
            out[i] = new AxisAlignedBB(w ? 0.0D : lo, 0.0D, n ? 0.0D : lo, e ? 1.0D : hi, h, s ? 1.0D : hi);
        }
        return out;
    }

    public BlockWall(Block modelBlock) {
        super(modelBlock == null ? legacyconnector.v1_12_2.net.minecraft.block.material.Material.ROCK : modelBlock.getMaterialLegacy());
        setDefaultState(blockState.getBaseState().withProperty(UP, false).withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false)
                .withProperty(WEST, false).withProperty(VARIANT, EnumType.NORMAL));
        if (modelBlock != null) {
            setHardness(modelBlock.mcc$hardness());
            setResistance(modelBlock.mcc$resistance() / 3.0F);
            setSoundType(modelBlock.getSoundType());
        }
        setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        state = getActualState(state, source, pos);
        return AABB_BY_INDEX[getAABBIndex(state)];
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
            Entity entityIn, boolean isActualState) {
        if (!isActualState) state = getActualState(state, worldIn, pos);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, CLIP_AABB_BY_INDEX[getAABBIndex(state)]);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        blockState = getActualState(blockState, worldIn, pos);
        return CLIP_AABB_BY_INDEX[getAABBIndex(blockState)];
    }

    private static int getAABBIndex(IBlockState state) {
        int i = 0;
        if (state.getValue(NORTH)) i |= 1 << EnumFacing.NORTH.getHorizontalIndex();
        if (state.getValue(EAST)) i |= 1 << EnumFacing.EAST.getHorizontalIndex();
        if (state.getValue(SOUTH)) i |= 1 << EnumFacing.SOUTH.getHorizontalIndex();
        if (state.getValue(WEST)) i |= 1 << EnumFacing.WEST.getHorizontalIndex();
        return i;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    private boolean canConnectTo(IBlockAccess worldIn, BlockPos pos, EnumFacing facing) {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        Block block = iblockstate.getBlock();
        BlockFaceShape shape = iblockstate.getBlockFaceShape(worldIn, pos, facing);
        boolean pole = shape == BlockFaceShape.MIDDLE_POLE_THICK || shape == BlockFaceShape.MIDDLE_POLE
                && (block instanceof BlockFenceGate || Block.mcc$named(block, "fence_gate", "spruce_fence_gate", "birch_fence_gate", "jungle_fence_gate", "dark_oak_fence_gate", "acacia_fence_gate"));
        return !isExceptBlockForAttachWithPiston(block) && shape == BlockFaceShape.SOLID || pole || block instanceof BlockWall
                || Block.mcc$named(block, "cobblestone_wall");
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (EnumType t : EnumType.values()) items.add(new ItemStack(this, 1, t.getMetadata()));
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return side == EnumFacing.DOWN ? super.shouldSideBeRendered(blockState, blockAccess, pos, side) : true;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(VARIANT, EnumType.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    /** The four connections, and the post shown unless the wall runs straight through with nothing on top. */
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        boolean n = canWallConnectTo(worldIn, pos, EnumFacing.NORTH);
        boolean e = canWallConnectTo(worldIn, pos, EnumFacing.EAST);
        boolean s = canWallConnectTo(worldIn, pos, EnumFacing.SOUTH);
        boolean w = canWallConnectTo(worldIn, pos, EnumFacing.WEST);
        boolean straight = n && !e && s && !w || !n && e && !s && w;
        return state.withProperty(UP, !straight || !worldIn.isAirBlock(pos.up())).withProperty(NORTH, n).withProperty(EAST, e).withProperty(SOUTH, s)
                .withProperty(WEST, w);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, UP, NORTH, EAST, WEST, SOUTH, VARIANT);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return face != EnumFacing.UP && face != EnumFacing.DOWN ? BlockFaceShape.MIDDLE_POLE_THICK : BlockFaceShape.CENTER_BIG;
    }

    @Override
    public boolean canBeConnectedTo(IBlockAccess world, BlockPos pos, EnumFacing facing) {
        BlockPos other = pos.offset(facing);
        return canConnectTo(world, other, facing.getOpposite());
    }

    private boolean canWallConnectTo(IBlockAccess world, BlockPos pos, EnumFacing facing) {
        BlockPos other = pos.offset(facing);
        Block block = world.getBlockState(other).getBlock();
        return block.canBeConnectedTo(world, other, facing.getOpposite()) || canConnectTo(world, other, facing.getOpposite());
    }

    public enum EnumType implements IStringSerializable {
        NORMAL(0, "cobblestone", "normal"),
        MOSSY(1, "mossy_cobblestone", "mossy");

        private final int meta;
        private final String name;
        private final String unlocalizedName;

        EnumType(int meta, String name, String unlocalizedName) {
            this.meta = meta;
            this.name = name;
            this.unlocalizedName = unlocalizedName;
        }

        public int getMetadata() {
            return meta;
        }

        @Override
        public String toString() {
            return name;
        }

        public static EnumType byMetadata(int meta) {
            return meta == 1 ? MOSSY : NORMAL;
        }

        @Override
        public String getName() {
            return name;
        }

        public String getTranslationKey() {
            return unlocalizedName;
        }

        public String getUnlocalizedName() {
            return unlocalizedName;
        }
    }
}
