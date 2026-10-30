package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 rails: a thin block on a solid top, shaped by {@link EnumRailDirection} (the metadata). Carts ride the modern rails; this base
 * keeps the shapes, the support rule and Forge's rail queries for mods' rails.
 */
public abstract class BlockRailBase extends Block {
    protected static final AxisAlignedBB FLAT_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);
    protected static final AxisAlignedBB ASCENDING_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    protected final boolean isPowered;

    public static boolean isRailBlock(World worldIn, BlockPos pos) {
        return isRailBlock(worldIn.getBlockState(pos));
    }

    public static boolean isRailBlock(IBlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockRailBase || Block.mcc$named(block, "rail", "golden_rail", "detector_rail", "activator_rail");
    }

    protected BlockRailBase(boolean isPowered) {
        super(Material.CIRCUITS);
        this.isPowered = isPowered;
        setCreativeTab(CreativeTabs.TRANSPORTATION);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        EnumRailDirection dir = state.getBlock() == this ? getRailDirection(source, pos, state, null) : null;
        return dir != null && dir.isAscending() ? ASCENDING_AABB : FLAT_AABB;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos.down()).isSideSolid(worldIn, pos.down(), EnumFacing.UP);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            state = updateDir(worldIn, pos, state, true);
            if (isPowered) state.neighborChanged(worldIn, pos, this, pos);
        }
    }

    /** Drops the rail when its support (or the one of its raised end) is gone; otherwise lets the rail react. */
    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (worldIn.isRemote || worldIn.getBlockState(pos).getBlock() != this) return;
        EnumRailDirection dir = getRailDirection(worldIn, pos, worldIn.getBlockState(pos), null);
        boolean drop = !worldIn.getBlockState(pos.down()).isSideSolid(worldIn, pos.down(), EnumFacing.UP);
        if (dir == EnumRailDirection.ASCENDING_EAST && !worldIn.getBlockState(pos.east()).isSideSolid(worldIn, pos.east(), EnumFacing.UP)) drop = true;
        else if (dir == EnumRailDirection.ASCENDING_WEST && !worldIn.getBlockState(pos.west()).isSideSolid(worldIn, pos.west(), EnumFacing.UP)) drop = true;
        else if (dir == EnumRailDirection.ASCENDING_NORTH && !worldIn.getBlockState(pos.north()).isSideSolid(worldIn, pos.north(), EnumFacing.UP)) drop = true;
        else if (dir == EnumRailDirection.ASCENDING_SOUTH && !worldIn.getBlockState(pos.south()).isSideSolid(worldIn, pos.south(), EnumFacing.UP)) drop = true;
        if (drop && !worldIn.isAirBlock(pos)) {
            dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
        } else {
            updateState(state, worldIn, pos, blockIn);
        }
    }

    protected void updateState(IBlockState state, World worldIn, BlockPos pos, Block blockIn) {
    }

    /** Connects the rail to its neighbours (the modern rail places itself; a mod rail keeps the shape it was given). */
    protected IBlockState updateDir(World worldIn, BlockPos pos, IBlockState state, boolean initialPlacement) {
        return state;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.NORMAL;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        if (getRailDirection(worldIn, pos, state, null).isAscending()) worldIn.notifyNeighborsOfStateChange(pos.up(), this, false);
        if (isPowered) {
            worldIn.notifyNeighborsOfStateChange(pos, this, false);
            worldIn.notifyNeighborsOfStateChange(pos.down(), this, false);
        }
    }

    public abstract IProperty<EnumRailDirection> getShapeProperty();

    /** Forge: the rail's shape for a cart (the shape property). */
    public EnumRailDirection getRailDirection(IBlockAccess world, BlockPos pos, IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.entity.item.EntityMinecart cart) {
        return state.getPropertyKeys().contains(getShapeProperty()) ? state.getValue(getShapeProperty()) : EnumRailDirection.NORTH_SOUTH;
    }

    /** Forge: whether the rail can turn corners (plain rails). */
    public boolean isFlexibleRail(IBlockAccess world, BlockPos pos) {
        return !isPowered;
    }

    /** Forge: whether the rail can go up slopes. */
    public boolean canMakeSlopes(IBlockAccess world, BlockPos pos) {
        return true;
    }

    /** Forge: the top speed of carts on it. */
    public float getRailMaxSpeed(World world, legacyconnector.v1_12_2.net.minecraft.entity.item.EntityMinecart cart, BlockPos pos) {
        return 0.4F;
    }

    /** Forge: called each tick a cart is on the rail. */
    public void onMinecartPass(World world, legacyconnector.v1_12_2.net.minecraft.entity.item.EntityMinecart cart, BlockPos pos) {
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(getShapeProperty(), state.getValue(getShapeProperty()).rotate(rot));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withProperty(getShapeProperty(), state.getValue(getShapeProperty()).mirror(mirrorIn));
    }

    public enum EnumRailDirection implements IStringSerializable {
        NORTH_SOUTH(0, "north_south"),
        EAST_WEST(1, "east_west"),
        ASCENDING_EAST(2, "ascending_east"),
        ASCENDING_WEST(3, "ascending_west"),
        ASCENDING_NORTH(4, "ascending_north"),
        ASCENDING_SOUTH(5, "ascending_south"),
        SOUTH_EAST(6, "south_east"),
        SOUTH_WEST(7, "south_west"),
        NORTH_WEST(8, "north_west"),
        NORTH_EAST(9, "north_east");

        private static final EnumRailDirection[] META_LOOKUP = new EnumRailDirection[values().length];
        private final int meta;
        private final String name;

        EnumRailDirection(int meta, String name) {
            this.meta = meta;
            this.name = name;
        }

        public int getMetadata() {
            return meta;
        }

        @Override
        public String toString() {
            return name;
        }

        public boolean isAscending() {
            return this == ASCENDING_NORTH || this == ASCENDING_EAST || this == ASCENDING_SOUTH || this == ASCENDING_WEST;
        }

        public static EnumRailDirection byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) meta = 0;
            return META_LOOKUP[meta];
        }

        @Override
        public String getName() {
            return name;
        }

        /** The shape turned by a structure rotation. */
        EnumRailDirection rotate(Rotation rot) {
            if (rot == Rotation.NONE) return this;
            EnumRailDirection[] cw = {EAST_WEST, NORTH_SOUTH, ASCENDING_SOUTH, ASCENDING_NORTH, ASCENDING_EAST, ASCENDING_WEST, SOUTH_WEST, NORTH_WEST,
                    NORTH_EAST, SOUTH_EAST};
            EnumRailDirection out = this;
            int turns = rot == Rotation.CLOCKWISE_90 ? 1 : rot == Rotation.CLOCKWISE_180 ? 2 : 3;
            for (int i = 0; i < turns; i++) out = cw[out.meta];
            return out;
        }

        /** The shape mirrored by a structure mirror. */
        EnumRailDirection mirror(Mirror mirror) {
            if (mirror == Mirror.LEFT_RIGHT) {
                return switch (this) {
                    case ASCENDING_NORTH -> ASCENDING_SOUTH;
                    case ASCENDING_SOUTH -> ASCENDING_NORTH;
                    case SOUTH_EAST -> NORTH_EAST;
                    case SOUTH_WEST -> NORTH_WEST;
                    case NORTH_WEST -> SOUTH_WEST;
                    case NORTH_EAST -> SOUTH_EAST;
                    default -> this;
                };
            }
            if (mirror == Mirror.FRONT_BACK) {
                return switch (this) {
                    case ASCENDING_EAST -> ASCENDING_WEST;
                    case ASCENDING_WEST -> ASCENDING_EAST;
                    case SOUTH_EAST -> SOUTH_WEST;
                    case SOUTH_WEST -> SOUTH_EAST;
                    case NORTH_WEST -> NORTH_EAST;
                    case NORTH_EAST -> NORTH_WEST;
                    default -> this;
                };
            }
            return this;
        }

        static {
            for (EnumRailDirection d : values()) META_LOOKUP[d.getMetadata()] = d;
        }
    }
}
