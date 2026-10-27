package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityTameable;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Follows the owner, teleporting next to it when it gets too far away. */
public class EntityAIFollowOwner extends EntityAIBase {
    private final EntityTameable tameable;
    private EntityLivingBase owner;
    private final double followSpeed;
    private int timeToRecalcPath;
    float maxDist;
    float minDist;

    public EntityAIFollowOwner(EntityTameable tameable, double followSpeed, float minDist, float maxDist) {
        this.tameable = tameable;
        this.followSpeed = followSpeed;
        this.minDist = minDist;
        this.maxDist = maxDist;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase o = tameable.getOwner();
        if (o == null) return false;
        if (o instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer p && p.isSpectator()) return false;
        if (tameable.isSitting()) return false;
        if (tameable.getDistanceSq(o) < minDist * minDist) return false;
        owner = o;
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !tameable.getNavigator().noPath() && tameable.getDistanceSq(owner) > maxDist * maxDist && !tameable.isSitting();
    }

    @Override
    public void startExecuting() {
        timeToRecalcPath = 0;
    }

    @Override
    public void resetTask() {
        owner = null;
        tameable.getNavigator().clearPath();
    }

    @Override
    public void updateTask() {
        tameable.getLookHelper().setLookPositionWithEntity(owner, 10.0F, tameable.getVerticalFaceSpeed());
        if (tameable.isSitting() || --timeToRecalcPath > 0) return;
        timeToRecalcPath = 10;
        if (tameable.getNavigator().tryMoveToEntityLiving(owner, followSpeed)) return;
        if (tameable.getLeashed() || tameable.isRiding() || tameable.getDistanceSq(owner) < 144.0D) return;
        if (tameable.mcc$entity instanceof net.minecraft.world.entity.LivingEntity self && owner.mcc$entity != null) {
            var target = owner.mcc$entity.blockPosition();
            var level = self.level();
            for (int dx = -2; dx <= 2; ++dx) {
                for (int dz = -2; dz <= 2; ++dz) {
                    if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
                    var pos = target.offset(dx, -1, dz);
                    if (level.getBlockState(pos).isFaceSturdy(level, pos, net.minecraft.core.Direction.UP) && level.isEmptyBlock(pos.above())
                            && level.isEmptyBlock(pos.above(2))) {
                        self.moveTo(pos.getX() + 0.5D, pos.getY() + 1, pos.getZ() + 0.5D, self.getYRot(), self.getXRot());
                        tameable.getNavigator().clearPath();
                        return;
                    }
                }
            }
        }
    }
}
