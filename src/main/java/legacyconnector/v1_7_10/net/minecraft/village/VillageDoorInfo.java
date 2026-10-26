package legacyconnector.v1_7_10.net.minecraft.village;

/**
 * A door of a village: its position, the direction of its inside, and how often villagers were kept from it. Villages built from
 * the modern village's points of interest use each home (bed) as a door whose inside is the bed itself.
 */
public class VillageDoorInfo {
    public final int posX;
    public final int posY;
    public final int posZ;
    public final int insideDirectionX;
    public final int insideDirectionZ;
    public int lastActivityTimestamp;
    public boolean isDetachedFromVillageFlag;
    private int doorOpeningRestrictionCounter;

    public VillageDoorInfo(int x, int y, int z, int insideX, int insideZ, int timestamp) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.insideDirectionX = insideX;
        this.insideDirectionZ = insideZ;
        this.lastActivityTimestamp = timestamp;
    }

    public int getDistanceSquared(int x, int y, int z) {
        int dx = x - posX, dy = y - posY, dz = z - posZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public int getInsideDistanceSquare(int x, int y, int z) {
        int dx = x - posX - insideDirectionX, dy = y - posY, dz = z - posZ - insideDirectionZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public int getInsidePosX() {
        return posX + insideDirectionX;
    }

    public int getInsidePosY() {
        return posY;
    }

    public int getInsidePosZ() {
        return posZ + insideDirectionZ;
    }

    public boolean isInside(int x, int z) {
        int dx = x - posX, dz = z - posZ;
        return dx * insideDirectionX + dz * insideDirectionZ >= 0;
    }

    public void resetDoorOpeningRestrictionCounter() {
        doorOpeningRestrictionCounter = 0;
    }

    public void incrementDoorOpeningRestrictionCounter() {
        doorOpeningRestrictionCounter++;
    }

    public int getDoorOpeningRestrictionCounter() {
        return doorOpeningRestrictionCounter;
    }
}
