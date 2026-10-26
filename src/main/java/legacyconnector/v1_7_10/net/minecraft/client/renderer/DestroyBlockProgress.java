package legacyconnector.v1_7_10.net.minecraft.client.renderer;

/** One block being broken: who breaks it, where, and the crack stage (0-9). */
public class DestroyBlockProgress {
    private final int miningPlayerEntId;
    private final int partialBlockX;
    private final int partialBlockY;
    private final int partialBlockZ;
    private int partialBlockProgress;
    private int createdAtCloudUpdateTick;

    public DestroyBlockProgress(int breakerId, int x, int y, int z) {
        this.miningPlayerEntId = breakerId;
        this.partialBlockX = x;
        this.partialBlockY = y;
        this.partialBlockZ = z;
    }

    public int getPartialBlockX() {
        return partialBlockX;
    }

    public int getPartialBlockY() {
        return partialBlockY;
    }

    public int getPartialBlockZ() {
        return partialBlockZ;
    }

    public void setPartialBlockDamage(int damage) {
        this.partialBlockProgress = Math.min(damage, 10);
    }

    public int getPartialBlockDamage() {
        return partialBlockProgress;
    }

    public void setCloudUpdateTick(int tick) {
        this.createdAtCloudUpdateTick = tick;
    }

    public int getCreationCloudUpdateTick() {
        return createdAtCloudUpdateTick;
    }

    public int mcc$breakerId() {
        return miningPlayerEntId;
    }
}
