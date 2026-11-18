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
        return worldIn.isRemote ? state : new Rail(worldIn, pos, state).place(worldIn.isBlockPowered(pos), initialPlacement).getBlockState();
    }

    /** 1.12's rail connection logic: finds the neighbouring rails of a rail and picks its shape from them (mods' carts and rails use it). */
    public class Rail {
        private final World world;
        private final BlockPos pos;
        private final BlockRailBase block;
        private IBlockState state;
        private final boolean isPowered;
        private final java.util.List<BlockPos> connectedRails = new java.util.ArrayList<>();
        private final boolean canMakeSlopes;

        public Rail(World worldIn, BlockPos pos, IBlockState state) {
            this.world = worldIn;
            this.pos = pos;
            this.state = state;
            this.block = (BlockRailBase) state.getBlock();
            EnumRailDirection dir = block.getRailDirection(worldIn, pos, state, null);
            this.isPowered = !block.isFlexibleRail(worldIn, pos);
            this.canMakeSlopes = block.canMakeSlopes(worldIn, pos);
            updateConnectedRails(dir);
        }

        public java.util.List<BlockPos> getConnectedRails() {
            return connectedRails;
        }

        private void updateConnectedRails(EnumRailDirection dir) {
            connectedRails.clear();
            switch (dir) {
                case NORTH_SOUTH: connectedRails.add(pos.north()); connectedRails.add(pos.south()); break;
                case EAST_WEST: connectedRails.add(pos.west()); connectedRails.add(pos.east()); break;
                case ASCENDING_EAST: connectedRails.add(pos.west()); connectedRails.add(pos.east().up()); break;
                case ASCENDING_WEST: connectedRails.add(pos.west().up()); connectedRails.add(pos.east()); break;
                case ASCENDING_NORTH: connectedRails.add(pos.north().up()); connectedRails.add(pos.south()); break;
                case ASCENDING_SOUTH: connectedRails.add(pos.north()); connectedRails.add(pos.south().up()); break;
                case SOUTH_EAST: connectedRails.add(pos.east()); connectedRails.add(pos.south()); break;
                case SOUTH_WEST: connectedRails.add(pos.west()); connectedRails.add(pos.south()); break;
                case NORTH_WEST: connectedRails.add(pos.west()); connectedRails.add(pos.north()); break;
                case NORTH_EAST: connectedRails.add(pos.east()); connectedRails.add(pos.north()); break;
                default: break;
            }
        }

        private void removeSoftConnections() {
            for (int i = 0; i < connectedRails.size(); ++i) {
                Rail rail = findRailAt(connectedRails.get(i));
                if (rail != null && rail.isConnectedToRail(this)) connectedRails.set(i, rail.pos);
                else connectedRails.remove(i--);
            }
        }

        private boolean hasRailAt(BlockPos p) {
            return isRailBlock(world, p) || isRailBlock(world, p.up()) || isRailBlock(world, p.down());
        }

        private Rail findRailAt(BlockPos p) {
            IBlockState s = world.getBlockState(p);
            if (isRailBlock(s)) return new Rail(world, p, s);
            BlockPos l = p.up();
            s = world.getBlockState(l);
            if (isRailBlock(s)) return new Rail(world, l, s);
            l = p.down();
            s = world.getBlockState(l);
            return isRailBlock(s) ? new Rail(world, l, s) : null;
        }

        private boolean isConnectedToRail(Rail rail) {
            return isConnectedTo(rail.pos);
        }

        private boolean isConnectedTo(BlockPos p) {
            for (BlockPos c : connectedRails) {
                if (c.getX() == p.getX() && c.getZ() == p.getZ()) return true;
            }
            return false;
        }

        protected int countAdjacentRails() {
            int n = 0;
            for (EnumFacing f : EnumFacing.Plane.HORIZONTAL.facings()) {
                if (hasRailAt(pos.offset(f))) ++n;
            }
            return n;
        }

        private boolean canConnectTo(Rail rail) {
            return isConnectedToRail(rail) || connectedRails.size() != 2;
        }

        private EnumRailDirection slopes(EnumRailDirection dir, BlockPos n, BlockPos s, BlockPos w, BlockPos e) {
            if (dir == EnumRailDirection.NORTH_SOUTH && canMakeSlopes) {
                if (isRailBlock(world, n.up())) dir = EnumRailDirection.ASCENDING_NORTH;
                if (isRailBlock(world, s.up())) dir = EnumRailDirection.ASCENDING_SOUTH;
            }
            if (dir == EnumRailDirection.EAST_WEST && canMakeSlopes) {
                if (isRailBlock(world, e.up())) dir = EnumRailDirection.ASCENDING_EAST;
                if (isRailBlock(world, w.up())) dir = EnumRailDirection.ASCENDING_WEST;
            }
            return dir == null ? EnumRailDirection.NORTH_SOUTH : dir;
        }

        private void connectTo(Rail rail) {
            connectedRails.add(rail.pos);
            BlockPos n = pos.north(), s = pos.south(), w = pos.west(), e = pos.east();
            boolean cn = isConnectedTo(n), cs = isConnectedTo(s), cw = isConnectedTo(w), ce = isConnectedTo(e);
            EnumRailDirection dir = null;
            if (cn || cs) dir = EnumRailDirection.NORTH_SOUTH;
            if (cw || ce) dir = EnumRailDirection.EAST_WEST;
            if (!isPowered) {
                if (cs && ce && !cn && !cw) dir = EnumRailDirection.SOUTH_EAST;
                if (cs && cw && !cn && !ce) dir = EnumRailDirection.SOUTH_WEST;
                if (cn && cw && !cs && !ce) dir = EnumRailDirection.NORTH_WEST;
                if (cn && ce && !cs && !cw) dir = EnumRailDirection.NORTH_EAST;
            }
            dir = slopes(dir, n, s, w, e);
            state = state.withProperty(block.getShapeProperty(), dir);
            world.setBlockState(pos, state, 3);
        }

        private boolean hasNeighborRail(BlockPos p) {
            Rail rail = findRailAt(p);
            if (rail == null) return false;
            rail.removeSoftConnections();
            return rail.canConnectTo(this);
        }

        public Rail place(boolean powered, boolean initialPlacement) {
            BlockPos n = pos.north(), s = pos.south(), w = pos.west(), e = pos.east();
            boolean hn = hasNeighborRail(n), hs = hasNeighborRail(s), hw = hasNeighborRail(w), he = hasNeighborRail(e);
            EnumRailDirection dir = null;
            if ((hn || hs) && !hw && !he) dir = EnumRailDirection.NORTH_SOUTH;
            if ((hw || he) && !hn && !hs) dir = EnumRailDirection.EAST_WEST;
            if (!isPowered) {
                if (hs && he && !hn && !hw) dir = EnumRailDirection.SOUTH_EAST;
                if (hs && hw && !hn && !he) dir = EnumRailDirection.SOUTH_WEST;
                if (hn && hw && !hs && !he) dir = EnumRailDirection.NORTH_WEST;
                if (hn && he && !hs && !hw) dir = EnumRailDirection.NORTH_EAST;
            }
            if (dir == null) {
                if (hn || hs) dir = EnumRailDirection.NORTH_SOUTH;
                if (hw || he) dir = EnumRailDirection.EAST_WEST;
                if (!isPowered) {
                    if (powered) {
                        if (hs && he) dir = EnumRailDirection.SOUTH_EAST;
                        if (hw && hs) dir = EnumRailDirection.SOUTH_WEST;
                        if (he && hn) dir = EnumRailDirection.NORTH_EAST;
                        if (hn && hw) dir = EnumRailDirection.NORTH_WEST;
                    } else {
                        if (hn && hw) dir = EnumRailDirection.NORTH_WEST;
                        if (he && hn) dir = EnumRailDirection.NORTH_EAST;
                        if (hw && hs) dir = EnumRailDirection.SOUTH_WEST;
                        if (hs && he) dir = EnumRailDirection.SOUTH_EAST;
                    }
                }
            }
            dir = slopes(dir, n, s, w, e);
            updateConnectedRails(dir);
            state = state.withProperty(block.getShapeProperty(), dir);
            if (initialPlacement || !world.getBlockState(pos).equals(state)) {
                world.setBlockState(pos, state, 3);
                for (int i = 0; i < connectedRails.size(); ++i) {
                    Rail rail = findRailAt(connectedRails.get(i));
                    if (rail != null) {
                        rail.removeSoftConnections();
                        if (rail.canConnectTo(this)) rail.connectTo(this);
                    }
                }
            }
            return this;
        }

        public IBlockState getBlockState() {
            return state;
        }
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
