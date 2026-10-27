package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.PathfinderMob;

/** 1.12.2 EntityCreature: path weights and the home area (the modern restriction of the mob). */
public abstract class EntityCreature extends EntityLiving {
    public static final java.util.UUID FLEEING_SPEED_MODIFIER_UUID = java.util.UUID.fromString("E199AD21-BA8A-4C53-8D13-6182D5C69D3A");
    private BlockPos mcc$homePosition = BlockPos.ORIGIN;
    private float mcc$maximumHomeDistance = -1.0F;

    public EntityCreature(World world) {
        super(world);
    }

    protected EntityCreature(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    private PathfinderMob mcc$creature() {
        return mcc$entity instanceof PathfinderMob m ? m : null;
    }

    public float getBlockPathWeight(BlockPos pos) {
        PathfinderMob m = mcc$creature();
        if (m != null && !mcc$hosted()) return m.getWalkTargetValue(pos.mcc$modern());
        return 0.0F;
    }

    @Override
    public boolean getCanSpawnHere() {
        return super.getCanSpawnHere() && getBlockPathWeight(new BlockPos(mcc$x(), getEntityBoundingBox().minY, mcc$z())) >= 0.0F;
    }

    public boolean hasPath() {
        return !getNavigator().noPath();
    }

    public boolean isWithinHomeDistanceCurrentPosition() {
        return isWithinHomeDistanceFromPosition(getPosition());
    }

    public boolean isWithinHomeDistanceFromPosition(BlockPos pos) {
        PathfinderMob m = mcc$creature();
        if (m != null) return m.isWithinRestriction(pos.mcc$modern());
        return mcc$maximumHomeDistance == -1.0F || mcc$homePosition.distanceSq(pos) < mcc$maximumHomeDistance * mcc$maximumHomeDistance;
    }

    public void setHomePosAndDistance(BlockPos pos, int distance) {
        mcc$homePosition = pos;
        mcc$maximumHomeDistance = distance;
        PathfinderMob m = mcc$creature();
        if (m != null) m.restrictTo(pos.mcc$modern(), distance);
    }

    public BlockPos getHomePosition() {
        PathfinderMob m = mcc$creature();
        return m != null ? BlockPos.mcc$of(m.getRestrictCenter()) : mcc$homePosition;
    }

    public float getMaximumHomeDistance() {
        PathfinderMob m = mcc$creature();
        return m != null ? m.getRestrictRadius() : mcc$maximumHomeDistance;
    }

    public void detachHome() {
        mcc$maximumHomeDistance = -1.0F;
        PathfinderMob m = mcc$creature();
        if (m != null) m.clearRestriction();
    }

    public boolean hasHome() {
        PathfinderMob m = mcc$creature();
        return m != null ? m.hasRestriction() : mcc$maximumHomeDistance != -1.0F;
    }

    protected double followLeashSpeed() {
        return 1.0D;
    }

    protected void onLeashDistance(float distance) {
    }
}
