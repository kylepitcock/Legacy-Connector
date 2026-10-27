package legacyconnector.v1_12_2.net.minecraft.world.border;

import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** 1.12.2 world border: a view over a level's modern border (a detached one when a mod creates its own). */
public class WorldBorder {
    private final net.minecraft.world.level.border.WorldBorder modern;

    public WorldBorder() {
        this(new net.minecraft.world.level.border.WorldBorder());
    }

    public WorldBorder(net.minecraft.world.level.border.WorldBorder modern) {
        this.modern = modern;
    }

    public net.minecraft.world.level.border.WorldBorder mcc$modern() {
        return modern;
    }

    public boolean contains(BlockPos pos) {
        return modern.isWithinBounds(pos.getX(), pos.getZ()) && pos.getX() + 1 <= maxX() && pos.getZ() + 1 <= maxZ();
    }

    public boolean contains(legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos range) {
        return range.getXEnd() > minX() && range.getXStart() < maxX() && range.getZEnd() > minZ() && range.getZStart() < maxZ();
    }

    public boolean contains(AxisAlignedBB bb) {
        return bb.maxX > minX() && bb.minX < maxX() && bb.maxZ > minZ() && bb.minZ < maxZ();
    }

    public double getClosestDistance(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return getClosestDistance(legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$get$posX(entity),
                legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$get$posZ(entity));
    }

    public double getClosestDistance(double x, double z) {
        return modern.getDistanceToBorder(x, z);
    }

    public double minX() {
        return modern.getMinX();
    }

    public double minZ() {
        return modern.getMinZ();
    }

    public double maxX() {
        return modern.getMaxX();
    }

    public double maxZ() {
        return modern.getMaxZ();
    }

    public double getCenterX() {
        return modern.getCenterX();
    }

    public double getCenterZ() {
        return modern.getCenterZ();
    }

    public void setCenter(double x, double z) {
        modern.setCenter(x, z);
    }

    public double getDiameter() {
        return modern.getSize();
    }

    public long getTimeUntilTarget() {
        return modern.getLerpRemainingTime();
    }

    public double getTargetSize() {
        return modern.getLerpTarget();
    }

    public void setTransition(double newSize) {
        modern.setSize(newSize);
    }

    public void setTransition(double oldSize, double newSize, long time) {
        modern.lerpSizeBetween(oldSize, newSize, time);
    }

    public void setSize(int size) {
        modern.setAbsoluteMaxSize(size);
    }

    public int getSize() {
        return modern.getAbsoluteMaxSize();
    }

    public double getDamageBuffer() {
        return modern.getDamageSafeZone();
    }

    public void setDamageBuffer(double buffer) {
        modern.setDamageSafeZone(buffer);
    }

    public double getDamageAmount() {
        return modern.getDamagePerBlock();
    }

    public void setDamageAmount(double amount) {
        modern.setDamagePerBlock(amount);
    }

    public double getResizeSpeed() {
        long t = modern.getLerpRemainingTime();
        return t <= 0 ? 0.0D : Math.abs(modern.getLerpTarget() - modern.getSize()) / (t / 1000.0D);
    }

    public int getWarningTime() {
        return modern.getWarningTime();
    }

    public void setWarningTime(int time) {
        modern.setWarningTime(time);
    }

    public int getWarningDistance() {
        return modern.getWarningBlocks();
    }

    public void setWarningDistance(int distance) {
        modern.setWarningBlocks(distance);
    }
}
