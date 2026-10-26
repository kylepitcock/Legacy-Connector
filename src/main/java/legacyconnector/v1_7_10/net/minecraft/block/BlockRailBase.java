package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Base of rails; {@code field_150053_a} marks powered rails. */
public class BlockRailBase extends Block {
    protected final boolean field_150053_a;

    public BlockRailBase(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_150053_a = false;
    }

    protected BlockRailBase(boolean powered) {
        super(Material.circuits);
        this.field_150053_a = powered;
    }

    public static boolean func_150051_a(Block block) {
        return block instanceof BlockRailBase;
    }

    public static boolean isRailBlock(Block block) {
        return block instanceof BlockRailBase;
    }

    public boolean isFlexibleRail(IBlockAccess world, int x, int y, int z) {
        return !field_150053_a;
    }

    public boolean canMakeSlopes(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    public int getBasicRailMetadata(IBlockAccess world, Object cart, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        return field_150053_a ? meta & 7 : meta;
    }

    public float getRailMaxSpeed(World world, Object cart, int x, int y, int z) {
        return 0.4F;
    }

    public static boolean isRailBlockAt(World world, int x, int y, int z) {
        return isRailBlock(world.getBlock(x, y, z));
    }

    /**
     * A rail and its two connections, recomputed from the neighbouring rails (vanilla's track-shape logic, the same as the
     * modern {@code RailState} but over metadata: 0/1 straight, 2-5 ascending, 6-9 curves; powered rails keep bit 8).
     */
    public class Rail {
        public World field_150660_b;
        public int field_150661_c;
        public int field_150658_d;
        public int field_150659_e;
        public final boolean field_150656_f;
        public java.util.List<legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition> field_150657_g = new java.util.ArrayList<>();
        private final boolean canMakeSlopes;

        public Rail(World world, int x, int y, int z) {
            this.field_150660_b = world;
            this.field_150661_c = x;
            this.field_150658_d = y;
            this.field_150659_e = z;
            Block block = world.getBlock(x, y, z);
            BlockRailBase rail = block instanceof BlockRailBase r ? r : BlockRailBase.this;
            int meta = rail.getBasicRailMetadata(world, null, x, y, z);
            this.field_150656_f = !rail.isFlexibleRail(world, x, y, z);
            this.canMakeSlopes = rail.canMakeSlopes(world, x, y, z);
            setConnections(meta);
        }

        private void add(int x, int y, int z) {
            field_150657_g.add(new legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition(x, y, z));
        }

        private void setConnections(int meta) {
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

        private void removeSoftConnections() {
            for (int i = 0; i < field_150657_g.size(); i++) {
                Rail other = getRail(field_150657_g.get(i));
                if (other != null && other.isConnectedTo(this)) {
                    field_150657_g.set(i, new legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition(other.field_150661_c, other.field_150658_d, other.field_150659_e));
                } else {
                    field_150657_g.remove(i--);
                }
            }
        }

        private boolean hasRail(int x, int y, int z) {
            return isRailBlockAt(field_150660_b, x, y, z) || isRailBlockAt(field_150660_b, x, y + 1, z) || isRailBlockAt(field_150660_b, x, y - 1, z);
        }

        private Rail getRail(legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition p) {
            for (int dy : new int[] {0, 1, -1}) {
                if (isRailBlockAt(field_150660_b, p.chunkPosX, p.chunkPosY + dy, p.chunkPosZ)) return new Rail(field_150660_b, p.chunkPosX, p.chunkPosY + dy, p.chunkPosZ);
            }
            return null;
        }

        private boolean isConnectedTo(Rail other) {
            return hasConnection(other.field_150661_c, other.field_150659_e);
        }

        private boolean hasConnection(int x, int z) {
            for (var p : field_150657_g) if (p.chunkPosX == x && p.chunkPosZ == z) return true;
            return false;
        }

        /** The neighbouring rails this one could connect to. */
        protected int countAdjacentRails() {
            int x = field_150661_c, y = field_150658_d, z = field_150659_e;
            int n = 0;
            if (hasRail(x, y, z - 1)) n++;
            if (hasRail(x, y, z + 1)) n++;
            if (hasRail(x - 1, y, z)) n++;
            if (hasRail(x + 1, y, z)) n++;
            return n;
        }

        private boolean canConnectTo(Rail other) {
            return isConnectedTo(other) || field_150657_g.size() != 2;
        }

        private int slope(int shape) {
            int x = field_150661_c, y = field_150658_d, z = field_150659_e;
            if (shape == 0 && canMakeSlopes) {
                if (isRailBlockAt(field_150660_b, x, y + 1, z - 1)) shape = 4;
                if (isRailBlockAt(field_150660_b, x, y + 1, z + 1)) shape = 5;
            }
            if (shape == 1 && canMakeSlopes) {
                if (isRailBlockAt(field_150660_b, x + 1, y + 1, z)) shape = 2;
                if (isRailBlockAt(field_150660_b, x - 1, y + 1, z)) shape = 3;
            }
            return Math.max(shape, 0);
        }

        private int metaFor(int shape) {
            return field_150656_f ? field_150660_b.getBlockMetadata(field_150661_c, field_150658_d, field_150659_e) & 8 | shape : shape;
        }

        private void connectTo(Rail other) {
            field_150657_g.add(new legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition(other.field_150661_c, other.field_150658_d, other.field_150659_e));
            int x = field_150661_c, z = field_150659_e;
            boolean n = hasConnection(x, z - 1), s = hasConnection(x, z + 1), w = hasConnection(x - 1, z), e = hasConnection(x + 1, z);
            int shape = -1;
            if (n || s) shape = 0;
            if (w || e) shape = 1;
            if (!field_150656_f) {
                if (s && e && !n && !w) shape = 6;
                if (s && w && !n && !e) shape = 7;
                if (n && w && !s && !e) shape = 8;
                if (n && e && !s && !w) shape = 9;
            }
            field_150660_b.setBlockMetadataWithNotify(field_150661_c, field_150658_d, field_150659_e, metaFor(slope(shape)), 3);
        }

        private boolean hasNeighborRail(int x, int y, int z) {
            Rail other = getRail(new legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition(x, y, z));
            if (other == null) return false;
            other.removeSoftConnections();
            return other.canConnectTo(this);
        }

        /** Recomputes this rail's shape from its neighbours ({@code powered} picks the curve a redstone signal selects) and updates them. */
        public void func_150655_a(boolean powered, boolean force) {
            int x = field_150661_c, y = field_150658_d, z = field_150659_e;
            boolean n = hasNeighborRail(x, y, z - 1), s = hasNeighborRail(x, y, z + 1), w = hasNeighborRail(x - 1, y, z), e = hasNeighborRail(x + 1, y, z);
            int shape = -1;
            if ((n || s) && !w && !e) shape = 0;
            if ((w || e) && !n && !s) shape = 1;
            if (!field_150656_f) {
                if (s && e && !n && !w) shape = 6;
                if (s && w && !n && !e) shape = 7;
                if (n && w && !s && !e) shape = 8;
                if (n && e && !s && !w) shape = 9;
            }
            if (shape == -1) {
                if (n || s) shape = 0;
                if (w || e) shape = 1;
                if (!field_150656_f) {
                    if (powered) {
                        if (s && e) shape = 6;
                        if (w && s) shape = 7;
                        if (e && n) shape = 9;
                        if (n && w) shape = 8;
                    } else {
                        if (n && w) shape = 8;
                        if (e && n) shape = 9;
                        if (w && s) shape = 7;
                        if (s && e) shape = 6;
                    }
                }
            }
            shape = slope(shape);
            setConnections(shape);
            int meta = metaFor(shape);
            if (force || field_150660_b.getBlockMetadata(x, y, z) != meta) {
                field_150660_b.setBlockMetadataWithNotify(x, y, z, meta, 3);
                for (int i = 0; i < field_150657_g.size(); i++) {
                    Rail other = getRail(field_150657_g.get(i));
                    if (other == null) continue;
                    other.removeSoftConnections();
                    if (other.canConnectTo(this)) other.connectTo(this);
                }
            }
        }
    }
}
