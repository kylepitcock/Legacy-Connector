package legacyconnector.v1_12_2.net.minecraft.world.border;

import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** 1.12.2 world border: a view over a level's modern border (one a mod builds itself is its own modern border). */
public class WorldBorder {
    private final net.minecraft.world.level.border.WorldBorder modern;

    public WorldBorder() {
        this(new net.minecraft.world.level.border.WorldBorder());
    }

    private WorldBorder(net.minecraft.world.level.border.WorldBorder modern) {
        this.modern = modern;
    }

    private static final java.util.Map<net.minecraft.world.level.border.WorldBorder, WorldBorder> VIEWS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static WorldBorder mcc$of(net.minecraft.world.level.border.WorldBorder modern) {
        return modern == null ? new WorldBorder() : VIEWS.computeIfAbsent(modern, WorldBorder::new);
    }

    public net.minecraft.world.level.border.WorldBorder mcc$modern() {
        return modern;
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() + 1 > minX() && pos.getX() < maxX() && pos.getZ() + 1 > minZ() && pos.getZ() < maxZ();
    }

    public boolean contains(legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos range) {
        return range.getXEnd() > minX() && range.getXStart() < maxX() && range.getZEnd() > minZ() && range.getZStart() < maxZ();
    }

    public boolean contains(AxisAlignedBB bb) {
        return bb.maxX > minX() && bb.minX < maxX() && bb.maxZ > minZ() && bb.minZ < maxZ();
    }

    public double getClosestDistance(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return getClosestDistance(entity.posX, entity.posZ);
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
        return modern.getLerpSpeed();
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

    public java.util.List<Object> getListeners() {
        return new java.util.ArrayList<>();
    }
}
