package legacyconnector.v1_7_10.net.minecraft.village;

import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;

/** A 1.7.10 village: a centre and a radius. Modern villages are POI based, so the layer never creates one. */
public class Village {
    private final ChunkCoordinates center;
    private final int villageRadius;

    public Village(int x, int y, int z, int radius) {
        this.center = new ChunkCoordinates(x, y, z);
        this.villageRadius = radius;
    }

    public ChunkCoordinates getCenter() {
        return center;
    }

    public int getVillageRadius() {
        return villageRadius;
    }

    public int getNumVillageDoors() {
        return 0;
    }

    public int getNumVillagers() {
        return 0;
    }

    public boolean isInRange(int x, int y, int z) {
        double dx = center.posX - x;
        double dy = center.posY - y;
        double dz = center.posZ - z;
        return dx * dx + dy * dy + dz * dz < (double) (villageRadius * villageRadius);
    }
}
