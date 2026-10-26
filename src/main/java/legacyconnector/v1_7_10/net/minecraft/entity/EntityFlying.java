package legacyconnector.v1_7_10.net.minecraft.entity;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A mob that flies (ghasts, bats; the base class of flying mods' mobs): no gravity and no fall damage. It moves as its AI pushes it,
 * with air drag (water and lava slow it more), as 1.7.10's EntityFlying did.
 */
public abstract class EntityFlying extends EntityLiving {
    public EntityFlying(World world) {
        super(world);
    }

    /** View of a modern flying mob. */
    protected EntityFlying(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    @Override
    protected void fall(float distance) {
    }

    @Override
    protected void updateFallState(double y, boolean onGround) {
    }

    @Override
    public boolean isOnLadder() {
        return false;
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (!mcc$hosted()) {
            super.moveEntityWithHeading(strafe, forward);
            return;
        }
        if (isInWater()) {
            mcc$swim(strafe, forward, 0.800000011920929D);
        } else if (handleLavaMovement()) {
            mcc$swim(strafe, forward, 0.5D);
        } else {
            float friction = mcc$friction();
            float accel = 0.16277136F / (friction * friction * friction);
            moveFlying(strafe, forward, mcc$get$onGround(this) ? 0.1F * accel : 0.02F);
            friction = mcc$friction();
            moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
            mcc$set$motionX(this, mcc$get$motionX(this) * friction);
            mcc$set$motionY(this, mcc$get$motionY(this) * friction);
            mcc$set$motionZ(this, mcc$get$motionZ(this) * friction);
        }
    }

    private void mcc$swim(float strafe, float forward, double drag) {
        moveFlying(strafe, forward, 0.02F);
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$set$motionX(this, mcc$get$motionX(this) * drag);
        mcc$set$motionY(this, mcc$get$motionY(this) * drag);
        mcc$set$motionZ(this, mcc$get$motionZ(this) * drag);
    }

    /** Air drag 0.91, or the slipperiness of the block below times 0.91 on the ground. */
    private float mcc$friction() {
        if (!mcc$get$onGround(this) || mcc$entity == null) return 0.91F;
        var level = mcc$entity.level();
        var below = net.minecraft.core.BlockPos.containing(mcc$entity.getX(), mcc$entity.getBoundingBox().minY - 1.0D, mcc$entity.getZ());
        return level.getBlockState(below).getFriction(level, below, mcc$entity) * 0.91F;
    }
}
