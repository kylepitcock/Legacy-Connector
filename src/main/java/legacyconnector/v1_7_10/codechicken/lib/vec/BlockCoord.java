package legacyconnector.v1_7_10.codechicken.lib.vec;

/** CodeChickenLib's integer block position. */
public class BlockCoord implements Comparable<BlockCoord> {
    public int x;
    public int y;
    public int z;

    public BlockCoord() {
    }

    public BlockCoord(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockCoord(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity tile) {
        this(tile.xCoord, tile.yCoord, tile.zCoord);
    }

    public BlockCoord(Vector3 v) {
        this((int) Math.floor(v.x), (int) Math.floor(v.y), (int) Math.floor(v.z));
    }

    public BlockCoord(int[] ia) {
        this(ia[0], ia[1], ia[2]);
    }

    public BlockCoord copy() {
        return new BlockCoord(x, y, z);
    }

    public BlockCoord set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public BlockCoord set(BlockCoord o) {
        return set(o.x, o.y, o.z);
    }

    public BlockCoord add(int dx, int dy, int dz) {
        x += dx;
        y += dy;
        z += dz;
        return this;
    }

    public BlockCoord add(BlockCoord o) {
        return add(o.x, o.y, o.z);
    }

    public BlockCoord sub(BlockCoord o) {
        return add(-o.x, -o.y, -o.z);
    }

    public BlockCoord multiply(int i) {
        x *= i;
        y *= i;
        z *= i;
        return this;
    }

    /** Moves one block towards the side (0 down, 1 up, 2 north, 3 south, 4 west, 5 east). */
    public BlockCoord offset(int side) {
        return offset(side, 1);
    }

    public BlockCoord offset(int side, int amount) {
        switch (side) {
            case 0 -> y -= amount;
            case 1 -> y += amount;
            case 2 -> z -= amount;
            case 3 -> z += amount;
            case 4 -> x -= amount;
            case 5 -> x += amount;
            default -> {
            }
        }
        return this;
    }

    public int getSide(int side) {
        return switch (side) {
            case 0, 1 -> y;
            case 2, 3 -> z;
            default -> x;
        };
    }

    public double mag() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public int mag2() {
        return x * x + y * y + z * z;
    }

    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }

    public int[] intArray() {
        return new int[] {x, y, z};
    }

    public int compareTo(BlockCoord o) {
        return x != o.x ? Integer.compare(x, o.x) : y != o.y ? Integer.compare(y, o.y) : Integer.compare(z, o.z);
    }

    public boolean equals(Object o) {
        return o instanceof BlockCoord b && b.x == x && b.y == y && b.z == z;
    }

    public int hashCode() {
        return (x + z * 31) * 31 + y;
    }

    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
