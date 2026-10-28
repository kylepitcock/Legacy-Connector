package legacyconnector.v1_12_2.net.minecraft.village;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** 1.12.2 village door: a house's door and the side its inside is on (modern villages count beds; each bed stands for a door). */
public class VillageDoorInfo {
    private final BlockPos doorBlockPos;
    private final BlockPos insideBlock;
    private final legacyconnector.v1_12_2.net.minecraft.util.EnumFacing insideDirection;
    private int lastActivityTimestamp;

    public VillageDoorInfo(BlockPos pos, int deltaX, int deltaZ, int timestamp) {
        this(pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.getFacingFromVector(deltaX, 0, deltaZ), timestamp);
    }

    public VillageDoorInfo(BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, int timestamp) {
        this.doorBlockPos = pos;
        this.insideDirection = facing;
        this.insideBlock = pos.offset(facing, 2);
        this.lastActivityTimestamp = timestamp;
    }

    public int getDistanceSquared(int x, int y, int z) {
        return (int) doorBlockPos.distanceSq(x, y, z);
    }

    public int getDistanceToDoorBlockSq(BlockPos pos) {
        return (int) pos.distanceSq(doorBlockPos);
    }

    public int getDistanceToInsideBlockSq(BlockPos pos) {
        return (int) insideBlock.distanceSq(pos);
    }

    public BlockPos getDoorBlockPos() {
        return doorBlockPos;
    }

    public BlockPos getInsideBlockPos() {
        return insideBlock;
    }

    public int getInsideOffsetX() {
        return insideDirection.getXOffset() * 2;
    }

    public int getInsideOffsetZ() {
        return insideDirection.getZOffset() * 2;
    }

    public int getLastActivityTimestamp() {
        return lastActivityTimestamp;
    }

    public void setLastActivityTimestamp(int timestamp) {
        this.lastActivityTimestamp = timestamp;
    }
}
