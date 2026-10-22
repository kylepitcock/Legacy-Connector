package legacyconnector.v1_7_10.net.minecraft.util;

public class ChunkCoordinates implements Comparable<ChunkCoordinates> {
    public int posX;
    public int posY;
    public int posZ;

    public ChunkCoordinates() {
    }

    public ChunkCoordinates(int x, int y, int z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public ChunkCoordinates(ChunkCoordinates other) {
        this(other.posX, other.posY, other.posZ);
    }

    public void set(int x, int y, int z) {
        posX = x;
        posY = y;
        posZ = z;
    }

    public float getDistanceSquared(int x, int y, int z) {
        float dx = posX - x, dy = posY - y, dz = posZ - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public float getDistanceSquaredToChunkCoordinates(ChunkCoordinates c) {
        return getDistanceSquared(c.posX, c.posY, c.posZ);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChunkCoordinates c && c.posX == posX && c.posY == posY && c.posZ == posZ;
    }

    @Override
    public int hashCode() {
        return posX + posZ << 8 + posY << 16;
    }

    @Override
    public int compareTo(ChunkCoordinates o) {
        return posY == o.posY ? (posZ == o.posZ ? posX - o.posX : posZ - o.posZ) : posY - o.posY;
    }

    @Override
    public String toString() {
        return "Pos{x=" + posX + ", y=" + posY + ", z=" + posZ + '}';
    }
}
