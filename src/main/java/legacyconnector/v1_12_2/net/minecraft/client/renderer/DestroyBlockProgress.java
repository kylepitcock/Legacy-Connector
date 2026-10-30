package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** 1.12.2: one block being broken: who breaks it, where, and the crack stage (0-9). */
public class DestroyBlockProgress {
    private final int miningPlayerEntId;
    private final BlockPos position;
    private int partialBlockProgress;
    private int createdAtCloudUpdateTick;

    public DestroyBlockProgress(int miningPlayerEntIdIn, BlockPos positionIn) {
        this.miningPlayerEntId = miningPlayerEntIdIn;
        this.position = positionIn;
    }

    public BlockPos getPosition() {
        return position;
    }

    public void setPartialBlockDamage(int damage) {
        this.partialBlockProgress = Math.min(damage, 10);
    }

    public int getPartialBlockDamage() {
        return partialBlockProgress;
    }

    public void setCloudUpdateTick(int createdAtCloudUpdateTickIn) {
        this.createdAtCloudUpdateTick = createdAtCloudUpdateTickIn;
    }

    public int getCreationCloudUpdateTick() {
        return createdAtCloudUpdateTick;
    }

    public int mcc$breakerId() {
        return miningPlayerEntId;
    }
}
