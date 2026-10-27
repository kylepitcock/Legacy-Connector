package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 EntityFlying: no fall damage, flies in the direction it faces (ghasts). */
public abstract class EntityFlying extends EntityLiving {
    public EntityFlying(World world) {
        super(world);
    }

    protected EntityFlying(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        if (!mcc$hosted()) super.fall(distance, damageMultiplier);
    }

    /** 1.12 flying: moves in the facing direction with air friction (water and lava slow it more). */
    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (!mcc$hosted() || mcc$entity == null) {
            super.travel(strafe, vertical, forward);
            return;
        }
        if (isInWater()) {
            moveRelative(strafe, vertical, forward, 0.02F);
            move(MoverType.SELF, mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
            scale(0.800000011920929D);
        } else if (isInLava()) {
            moveRelative(strafe, vertical, forward, 0.02F);
            move(MoverType.SELF, mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
            scale(0.5D);
        } else {
            float f = 0.91F;
            var level = mcc$entity.level();
            if (mcc$get$onGround(this)) {
                var below = net.minecraft.core.BlockPos.containing(mcc$x(), getEntityBoundingBox().minY - 1.0D, mcc$z());
                f = level.getBlockState(below).getFriction(level, below, mcc$entity) * 0.91F;
            }
            float f1 = 0.16277136F / (f * f * f);
            moveRelative(strafe, vertical, forward, mcc$get$onGround(this) ? 0.1F * f1 : 0.02F);
            move(MoverType.SELF, mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
            scale(f);
        }
        if (mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) l.calculateEntityAnimation(false);
    }

    private void scale(double f) {
        var d = mcc$entity.getDeltaMovement();
        mcc$entity.setDeltaMovement(d.x * f, d.y * f, d.z * f);
    }

    @Override
    public boolean isOnLadder() {
        return false;
    }
}
