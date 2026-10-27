package legacyconnector.v1_12_2.net.minecraft.util.math;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;

public class BlockPos extends Vec3i {
    public static final BlockPos ORIGIN = new BlockPos(0, 0, 0);
    private static final int NUM_X_BITS = 26;
    private static final int NUM_Z_BITS = 26;
    private static final int NUM_Y_BITS = 12;
    private static final int Y_SHIFT = NUM_Z_BITS;
    private static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS;
    private static final long X_MASK = (1L << NUM_X_BITS) - 1L;
    private static final long Y_MASK = (1L << NUM_Y_BITS) - 1L;
    private static final long Z_MASK = (1L << NUM_Z_BITS) - 1L;

    public BlockPos(int x, int y, int z) {
        super(x, y, z);
    }

    public BlockPos(double x, double y, double z) {
        super(x, y, z);
    }

    public BlockPos(Vec3d vec) {
        this(vec.x, vec.y, vec.z);
    }

    public BlockPos(Vec3i source) {
        this(source.getX(), source.getY(), source.getZ());
    }

    public BlockPos add(double x, double y, double z) {
        return x == 0.0D && y == 0.0D && z == 0.0D ? this : new BlockPos(getX() + x, getY() + y, getZ() + z);
    }

    public BlockPos add(int x, int y, int z) {
        return x == 0 && y == 0 && z == 0 ? this : new BlockPos(getX() + x, getY() + y, getZ() + z);
    }

    public BlockPos add(Vec3i vec) {
        return add(vec.getX(), vec.getY(), vec.getZ());
    }

    public BlockPos subtract(Vec3i vec) {
        return add(-vec.getX(), -vec.getY(), -vec.getZ());
    }

    public BlockPos up() {
        return up(1);
    }

    public BlockPos up(int n) {
        return offset(EnumFacing.UP, n);
    }

    public BlockPos down() {
        return down(1);
    }

    public BlockPos down(int n) {
        return offset(EnumFacing.DOWN, n);
    }

    public BlockPos north() {
        return north(1);
    }

    public BlockPos north(int n) {
        return offset(EnumFacing.NORTH, n);
    }

    public BlockPos south() {
        return south(1);
    }

    public BlockPos south(int n) {
        return offset(EnumFacing.SOUTH, n);
    }

    public BlockPos west() {
        return west(1);
    }

    public BlockPos west(int n) {
        return offset(EnumFacing.WEST, n);
    }

    public BlockPos east() {
        return east(1);
    }

    public BlockPos east(int n) {
        return offset(EnumFacing.EAST, n);
    }

    public BlockPos offset(EnumFacing facing) {
        return offset(facing, 1);
    }

    public BlockPos offset(EnumFacing facing, int n) {
        return n == 0 ? this : new BlockPos(getX() + facing.getXOffset() * n, getY() + facing.getYOffset() * n, getZ() + facing.getZOffset() * n);
    }

    public BlockPos rotate(Rotation rotationIn) {
        return switch (rotationIn) {
            case NONE -> this;
            case CLOCKWISE_90 -> new BlockPos(-getZ(), getY(), getX());
            case CLOCKWISE_180 -> new BlockPos(-getX(), getY(), -getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(getZ(), getY(), -getX());
        };
    }

    @Override
    public BlockPos crossProduct(Vec3i vec) {
        return new BlockPos(getY() * vec.getZ() - getZ() * vec.getY(), getZ() * vec.getX() - getX() * vec.getZ(), getX() * vec.getY() - getY() * vec.getX());
    }

    public long toLong() {
        return (getX() & X_MASK) << X_SHIFT | (getY() & Y_MASK) << Y_SHIFT | (getZ() & Z_MASK);
    }

    public static BlockPos fromLong(long serialized) {
        int x = (int) (serialized << 64 - X_SHIFT - NUM_X_BITS >> 64 - NUM_X_BITS);
        int y = (int) (serialized << 64 - Y_SHIFT - NUM_Y_BITS >> 64 - NUM_Y_BITS);
        int z = (int) (serialized << 64 - NUM_Z_BITS >> 64 - NUM_Z_BITS);
        return new BlockPos(x, y, z);
    }

    public BlockPos toImmutable() {
        return this;
    }

    public static Iterable<BlockPos> getAllInBox(BlockPos from, BlockPos to) {
        return getAllInBox(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()),
                Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
    }

    public static Iterable<BlockPos> getAllInBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        List<BlockPos> out = new ArrayList<>();
        for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) for (int x = x1; x <= x2; x++) out.add(new BlockPos(x, y, z));
        return out;
    }

    public static Iterable<MutableBlockPos> getAllInBoxMutable(BlockPos from, BlockPos to) {
        return getAllInBoxMutable(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()),
                Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
    }

    public static Iterable<MutableBlockPos> getAllInBoxMutable(int x1, int y1, int z1, int x2, int y2, int z2) {
        List<MutableBlockPos> out = new ArrayList<>();
        for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) for (int x = x1; x <= x2; x++) out.add(new MutableBlockPos(x, y, z));
        return out;
    }

    public net.minecraft.core.BlockPos mcc$modern() {
        return new net.minecraft.core.BlockPos(getX(), getY(), getZ());
    }

    public static BlockPos mcc$of(net.minecraft.core.BlockPos pos) {
        return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    public static class MutableBlockPos extends BlockPos {
        protected int x;
        protected int y;
        protected int z;

        public MutableBlockPos() {
            this(0, 0, 0);
        }

        public MutableBlockPos(BlockPos pos) {
            this(pos.getX(), pos.getY(), pos.getZ());
        }

        public MutableBlockPos(int x, int y, int z) {
            super(0, 0, 0);
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getZ() {
            return z;
        }

        public MutableBlockPos setPos(int xIn, int yIn, int zIn) {
            this.x = xIn;
            this.y = yIn;
            this.z = zIn;
            return this;
        }

        public MutableBlockPos setPos(double xIn, double yIn, double zIn) {
            return setPos(MathHelper.floor(xIn), MathHelper.floor(yIn), MathHelper.floor(zIn));
        }

        public MutableBlockPos setPos(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
            Vec3d v = entity.getPositionVector();
            return setPos(v.x, v.y, v.z);
        }

        public MutableBlockPos setPos(Vec3i vec) {
            return setPos(vec.getX(), vec.getY(), vec.getZ());
        }

        public MutableBlockPos move(EnumFacing facing) {
            return move(facing, 1);
        }

        public MutableBlockPos move(EnumFacing facing, int n) {
            return setPos(x + facing.getXOffset() * n, y + facing.getYOffset() * n, z + facing.getZOffset() * n);
        }

        public void setY(int yIn) {
            this.y = yIn;
        }

        @Override
        public BlockPos toImmutable() {
            return new BlockPos(this);
        }
    }

    /**
     * Vanilla kept up to 100 released positions for reuse; a fresh position per {@code retain} behaves the same for callers
     * and needs no locking, so released ones are simply dropped.
     */
    public static final class PooledMutableBlockPos extends MutableBlockPos {
        private PooledMutableBlockPos(int x, int y, int z) {
            super(x, y, z);
        }

        public static PooledMutableBlockPos retain() {
            return retain(0, 0, 0);
        }

        public static PooledMutableBlockPos retain(double xIn, double yIn, double zIn) {
            return retain(MathHelper.floor(xIn), MathHelper.floor(yIn), MathHelper.floor(zIn));
        }

        public static PooledMutableBlockPos retain(Vec3i vec) {
            return retain(vec.getX(), vec.getY(), vec.getZ());
        }

        public static PooledMutableBlockPos retain(int xIn, int yIn, int zIn) {
            return new PooledMutableBlockPos(xIn, yIn, zIn);
        }

        public void release() {
        }

        @Override
        public PooledMutableBlockPos setPos(int xIn, int yIn, int zIn) {
            super.setPos(xIn, yIn, zIn);
            return this;
        }

        @Override
        public PooledMutableBlockPos setPos(double xIn, double yIn, double zIn) {
            super.setPos(xIn, yIn, zIn);
            return this;
        }

        @Override
        public PooledMutableBlockPos setPos(Vec3i vec) {
            super.setPos(vec);
            return this;
        }

        @Override
        public PooledMutableBlockPos setPos(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
            super.setPos(entity);
            return this;
        }

        @Override
        public PooledMutableBlockPos move(EnumFacing facing) {
            super.move(facing);
            return this;
        }

        @Override
        public PooledMutableBlockPos move(EnumFacing facing, int n) {
            super.move(facing, n);
            return this;
        }
    }
}
