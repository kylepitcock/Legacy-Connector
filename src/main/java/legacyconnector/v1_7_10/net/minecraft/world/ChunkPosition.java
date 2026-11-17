package legacyconnector.v1_7_10.net.minecraft.world;

/** An integer block position (1.7.10's ChunkPosition: structure locations, explosion lists). */
public class ChunkPosition {
    public final int chunkPosX;
    public final int chunkPosY;
    public final int chunkPosZ;

    public ChunkPosition(int x, int y, int z) {
        this.chunkPosX = x;
        this.chunkPosY = y;
        this.chunkPosZ = z;
    }

    /** The block position containing the point. */
    public ChunkPosition(legacyconnector.v1_7_10.net.minecraft.util.Vec3 point) {
        this(legacyconnector.v1_7_10.net.minecraft.util.MathHelper.floor_double(point.xCoord),
                legacyconnector.v1_7_10.net.minecraft.util.MathHelper.floor_double(point.yCoord),
                legacyconnector.v1_7_10.net.minecraft.util.MathHelper.floor_double(point.zCoord));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChunkPosition p && p.chunkPosX == chunkPosX && p.chunkPosY == chunkPosY && p.chunkPosZ == chunkPosZ;
    }

    @Override
    public int hashCode() {
        return chunkPosX * 8976890 + chunkPosY * 981131 + chunkPosZ;
    }
}
