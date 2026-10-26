package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Base of rails, with 1.7.10's track shape logic ({@link Rail}): metadata 0-1 straight (north-south, east-west), 2-5 ascending
 * (east, west, north, south), 6-9 curves; powered rails ({@link #isPowered}) keep bit 3 for their state and cannot curve.
 */
public class BlockRailBase extends Block {
    /** {@code field_150053_a}: powered, detector and activator rails (no curves, bit 3 is the powered state). */
    protected final boolean isPowered;
    /** Forge: the render type, changeable by mods. */
    protected int renderType = 9;

    public BlockRailBase(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        String name = info.legacyName();
        this.isPowered = name != null && !name.equals("minecraft:rail") && name.endsWith("_rail");
    }

    protected BlockRailBase(boolean powered) {
        super(Material.circuits);
        this.isPowered = powered;
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabTransport);
    }

    /** {@code func_150049_b_}: whether the block at the position is a rail. */
    public static boolean isRailBlockAt(World world, int x, int y, int z) {
        return isRailBlock(world.getBlock(x, y, z));
    }

    public static boolean func_150049_b_(World world, int x, int y, int z) {
        return isRailBlockAt(world, x, y, z);
    }

    /** {@code func_150051_a}. */
    public static boolean isRailBlock(Block block) {
        return block instanceof BlockRailBase;
    }

    public static boolean func_150051_a(Block block) {
        return isRailBlock(block);
    }

    /** {@code func_150050_e}: whether this is a powered-type rail. */
    public boolean isPowered() {
        return isPowered;
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return renderType;
    }

    public void setRenderType(int type) {
        this.renderType = type;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        if (isPowered) meta &= 7;
        if (meta >= 2 && meta <= 5) setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.625F, 1.0F);
        else setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public int getMobilityFlag() {
        return 0;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (mcc$vanilla != null || world.isRemote) return;
        refreshTrackShape(world, x, y, z, true);
        if (isPowered) onNeighborBlockChange(world, x, y, z, this);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (mcc$vanilla != null || world.isRemote) return;
        int meta = world.getBlockMetadata(x, y, z);
        int shape = isPowered ? meta & 7 : meta;
        boolean unsupported = !World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)
                || shape == 2 && !World.doesBlockHaveSolidTopSurface(world, x + 1, y, z)
                || shape == 3 && !World.doesBlockHaveSolidTopSurface(world, x - 1, y, z)
                || shape == 4 && !World.doesBlockHaveSolidTopSurface(world, x, y, z - 1)
                || shape == 5 && !World.doesBlockHaveSolidTopSurface(world, x, y, z + 1);
        if (unsupported) {
            dropBlockAsItem(world, x, y, z, meta, 0);
            world.setBlockToAir(x, y, z);
        } else {
            onRedstoneSignal(world, x, y, z, meta, shape, neighbor);
        }
    }

    /** {@code func_150048_a}: a neighbour changed; powered rails and plain rails react to redstone here. */
    protected void onRedstoneSignal(World world, int x, int y, int z, int meta, int shape, Block neighbor) {
    }

    /** {@code func_150052_a}: recomputes the shape of the track at the position from its neighbours. */
    protected void refreshTrackShape(World world, int x, int y, int z, boolean initial) {
        if (!world.isRemote) new Rail(world, x, y, z).func_150655_a(world.isBlockIndirectlyGettingPowered(x, y, z), initial);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        int shape = isPowered ? meta & 7 : meta;
        super.breakBlock(world, x, y, z, block, meta);
        if (mcc$vanilla != null) return;
        if (shape == 2 || shape == 3 || shape == 4 || shape == 5) world.notifyBlocksOfNeighborChange(x, y + 1, z, block);
        if (isPowered) {
            world.notifyBlocksOfNeighborChange(x, y, z, block);
            world.notifyBlocksOfNeighborChange(x, y - 1, z, block);
        }
    }

    // ------------------------------------------------------------------ Forge

    /** Forge: whether the rail can curve (powered rails cannot). */
    public boolean isFlexibleRail(IBlockAccess world, int x, int y, int z) {
        return !isPowered();
    }

    public boolean canMakeSlopes(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    /** Forge: the track shape (0-9) for this cart, without the powered bit. */
    public int getBasicRailMetadata(IBlockAccess world, EntityMinecart cart, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        return isPowered() ? meta & 7 : meta;
    }

    /** Forge: the top speed of carts on this rail. */
    public float getRailMaxSpeed(World world, EntityMinecart cart, int x, int y, int z) {
        return 0.4F;
    }

    /** Forge: called every tick a cart moves over the rail. */
    public void onMinecartPass(World world, EntityMinecart cart, int x, int y, int z) {
    }

    /** 1.7.10's rail-shape solver: connects a track to up to two neighbouring tracks and updates both. */
    public class Rail {
        private World field_150660_b;
        private int field_150661_c;
        private int field_150658_d;
        private int field_150659_e;
        /** Not flexible: cannot curve (powered rails). */
        private final boolean field_150656_f;
        private List field_150657_g = new ArrayList();
        private final boolean canMakeSlopes;

        public Rail(World world, int x, int y, int z) {
            this.field_150660_b = world;
            this.field_150661_c = x;
            this.field_150658_d = y;
            this.field_150659_e = z;
            Block block = world.getBlock(x, y, z);
            BlockRailBase target = block instanceof BlockRailBase r ? r : BlockRailBase.this;
            int meta = target.getBasicRailMetadata(world, null, x, y, z);
            this.field_150656_f = !target.isFlexibleRail(world, x, y, z);
            this.canMakeSlopes = target.canMakeSlopes(world, x, y, z);
            setBasicRail(meta);
        }

        private void setBasicRail(int meta) {
            field_150657_g.clear();
            int x = field_150661_c, y = field_150658_d, z = field_150659_e;
            switch (meta) {
                case 0 -> { add(x, y, z - 1); add(x, y, z + 1); }
                case 1 -> { add(x - 1, y, z); add(x + 1, y, z); }
                case 2 -> { add(x - 1, y, z); add(x + 1, y + 1, z); }
                case 3 -> { add(x - 1, y + 1, z); add(x + 1, y, z); }
                case 4 -> { add(x, y + 1, z - 1); add(x, y, z + 1); }
                case 5 -> { add(x, y, z - 1); add(x, y + 1, z + 1); }
                case 6 -> { add(x + 1, y, z); add(x, y, z + 1); }
                case 7 -> { add(x - 1, y, z); add(x, y, z + 1); }
                case 8 -> { add(x - 1, y, z); add(x, y, z - 1); }
                case 9 -> { add(x + 1, y, z); add(x, y, z - 1); }
                default -> { }
            }
        }

        private void add(int x, int y, int z) {
            field_150657_g.add(new ChunkPosition(x, y, z));
        }

        /** Drops the connections whose track is gone or no longer points back here. */
        private void refreshConnectedTracks() {
            for (int i = 0; i < field_150657_g.size(); ++i) {
                Rail rail = getRail((ChunkPosition) field_150657_g.get(i));
                if (rail != null && rail.isConnectedTo(this)) {
                    field_150657_g.set(i, new ChunkPosition(rail.field_150661_c, rail.field_150658_d, rail.field_150659_e));
                } else {
                    field_150657_g.remove(i--);
                }
            }
        }

        private boolean hasRail(int x, int y, int z) {
            World w = field_150660_b;
            return isRailBlockAt(w, x, y, z) || isRailBlockAt(w, x, y + 1, z) || isRailBlockAt(w, x, y - 1, z);
        }

        private Rail getRail(ChunkPosition p) {
            World w = field_150660_b;
            if (isRailBlockAt(w, p.chunkPosX, p.chunkPosY, p.chunkPosZ)) return new Rail(w, p.chunkPosX, p.chunkPosY, p.chunkPosZ);
            if (isRailBlockAt(w, p.chunkPosX, p.chunkPosY + 1, p.chunkPosZ)) return new Rail(w, p.chunkPosX, p.chunkPosY + 1, p.chunkPosZ);
            if (isRailBlockAt(w, p.chunkPosX, p.chunkPosY - 1, p.chunkPosZ)) return new Rail(w, p.chunkPosX, p.chunkPosY - 1, p.chunkPosZ);
            return null;
        }

        private boolean isConnectedTo(Rail other) {
            for (Object o : field_150657_g) {
                ChunkPosition p = (ChunkPosition) o;
                if (p.chunkPosX == other.field_150661_c && p.chunkPosZ == other.field_150659_e) return true;
            }
            return false;
        }

        private boolean isPartOfTrack(int x, int y, int z) {
            for (Object o : field_150657_g) {
                ChunkPosition p = (ChunkPosition) o;
                if (p.chunkPosX == x && p.chunkPosZ == z) return true;
            }
            return false;
        }

        /** {@code func_150649_b}: how many of the four horizontal neighbours (at any of three heights) are rails. */
        protected int countAdjacentRails() {
            int x = field_150661_c, y = field_150658_d, z = field_150659_e;
            int n = 0;
            if (hasRail(x, y, z - 1)) ++n;
            if (hasRail(x, y, z + 1)) ++n;
            if (hasRail(x - 1, y, z)) ++n;
            if (hasRail(x + 1, y, z)) ++n;
            return n;
        }

        private boolean canConnectTo(Rail other) {
            return isConnectedTo(other) || field_150657_g.size() != 2;
        }

        private void connectTo(Rail other) {
            int x = field_150661_c, y = field_150658_d, z = field_150659_e;
            field_150657_g.add(new ChunkPosition(other.field_150661_c, other.field_150658_d, other.field_150659_e));
            boolean north = isPartOfTrack(x, y, z - 1);
            boolean south = isPartOfTrack(x, y, z + 1);
            boolean west = isPartOfTrack(x - 1, y, z);
            boolean east = isPartOfTrack(x + 1, y, z);
            int shape = -1;
            if (north || south) shape = 0;
            if (west || east) shape = 1;
            if (!field_150656_f) {
                if (south && east && !north && !west) shape = 6;
                if (south && west && !north && !east) shape = 7;
                if (north && west && !south && !east) shape = 8;
                if (north && east && !south && !west) shape = 9;
            }
            shape = slope(shape);
            if (shape < 0) shape = 0;
            int meta = shape;
            if (field_150656_f) meta = field_150660_b.getBlockMetadata(x, y, z) & 8 | shape;
            field_150660_b.setBlockMetadataWithNotify(x, y, z, meta, 3);
        }

        private int slope(int shape) {
            World w = field_150660_b;
            int x = field_150661_c, y = field_150658_d, z = field_150659_e;
            if (shape == 0 && canMakeSlopes) {
                if (isRailBlockAt(w, x, y + 1, z - 1)) shape = 4;
                if (isRailBlockAt(w, x, y + 1, z + 1)) shape = 5;
            }
            if (shape == 1 && canMakeSlopes) {
                if (isRailBlockAt(w, x + 1, y + 1, z)) shape = 2;
                if (isRailBlockAt(w, x - 1, y + 1, z)) shape = 3;
            }
            return shape;
        }

        private boolean canConnectFrom(int x, int y, int z) {
            Rail rail = getRail(new ChunkPosition(x, y, z));
            if (rail == null) return false;
            rail.refreshConnectedTracks();
            return rail.canConnectTo(this);
        }

        /** Recomputes this track's shape ({@code powered} breaks ties between curves) and reconnects its neighbours. */
        public void func_150655_a(boolean powered, boolean initial) {
            int x = field_150661_c, y = field_150658_d, z = field_150659_e;
            boolean north = canConnectFrom(x, y, z - 1);
            boolean south = canConnectFrom(x, y, z + 1);
            boolean west = canConnectFrom(x - 1, y, z);
            boolean east = canConnectFrom(x + 1, y, z);
            int shape = -1;
            if ((north || south) && !west && !east) shape = 0;
            if ((west || east) && !north && !south) shape = 1;
            if (!field_150656_f) {
                if (south && east && !north && !west) shape = 6;
                if (south && west && !north && !east) shape = 7;
                if (north && west && !south && !east) shape = 8;
                if (north && east && !south && !west) shape = 9;
            }
            if (shape == -1) {
                if (north || south) shape = 0;
                if (west || east) shape = 1;
                if (!field_150656_f) {
                    if (powered) {
                        if (south && east) shape = 6;
                        if (west && south) shape = 7;
                        if (east && north) shape = 9;
                        if (north && west) shape = 8;
                    } else {
                        if (north && west) shape = 8;
                        if (east && north) shape = 9;
                        if (west && south) shape = 7;
                        if (south && east) shape = 6;
                    }
                }
            }
            shape = slope(shape);
            if (shape < 0) shape = 0;
            setBasicRail(shape);
            int meta = shape;
            if (field_150656_f) meta = field_150660_b.getBlockMetadata(x, y, z) & 8 | shape;
            if (initial || field_150660_b.getBlockMetadata(x, y, z) != meta) {
                field_150660_b.setBlockMetadataWithNotify(x, y, z, meta, 3);
                for (Object o : new ArrayList<>(field_150657_g)) {
                    Rail rail = getRail((ChunkPosition) o);
                    if (rail != null) {
                        rail.refreshConnectedTracks();
                        if (rail.canConnectTo(this)) rail.connectTo(this);
                    }
                }
            }
        }
    }
}
