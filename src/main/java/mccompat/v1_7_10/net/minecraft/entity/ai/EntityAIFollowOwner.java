package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraft.entity.passive.EntityTameable;

/** Tamed: walk to the owner when further than {@code minDist}; teleport next to them when far away. */
public class EntityAIFollowOwner extends EntityAIBase {
    private final EntityTameable thePet;
    private EntityLivingBase theOwner;
    private final double speed;
    private final float minDist;
    private final float maxDist;
    private int timeToRecalcPath;

    public EntityAIFollowOwner(EntityTameable pet, double speed, float minDist, float maxDist) {
        this.thePet = pet;
        this.speed = speed;
        this.minDist = minDist;
        this.maxDist = maxDist;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase owner = thePet.getOwner();
        if (owner == null || thePet.isSitting() || thePet.getDistanceSqToEntity(owner) < minDist * minDist) return false;
        theOwner = owner;
        return true;
    }

    @Override
    public boolean continueExecuting() {
        return !thePet.getNavigator().noPath() && thePet.getDistanceSqToEntity(theOwner) > maxDist * maxDist && !thePet.isSitting();
    }

    @Override
    public void startExecuting() {
        timeToRecalcPath = 0;
    }

    @Override
    public void resetTask() {
        theOwner = null;
        thePet.getNavigator().clearPathEntity();
    }

    @Override
    public void updateTask() {
        thePet.getLookHelper().setLookPositionWithEntity(theOwner, 10.0F, thePet.getVerticalFaceSpeed());
        if (thePet.isSitting() || --timeToRecalcPath > 0) return;
        timeToRecalcPath = 10;
        if (thePet.getNavigator().tryMoveToEntityLiving(theOwner, speed)) return;
        if (thePet.getDistanceSqToEntity(theOwner) >= 144.0 && thePet.mcc$entity != null) {
            int x = (int) Math.floor(theOwner.mcc$x()) - 2;
            int z = (int) Math.floor(theOwner.mcc$z()) - 2;
            int y = (int) Math.floor(theOwner.mcc$y());
            var level = thePet.mcc$entity.level();
            for (int dx = 0; dx <= 4; dx++) {
                for (int dz = 0; dz <= 4; dz++) {
                    if (dx >= 1 && dz >= 1 && dx <= 3 && dz <= 3) continue;
                    var pos = new net.minecraft.core.BlockPos(x + dx, y, z + dz);
                    if (level.getBlockState(pos.below()).isSolidRender(level, pos.below()) && level.getBlockState(pos).isAir()
                            && level.getBlockState(pos.above()).isAir()) {
                        thePet.setLocationAndAngles(x + dx + 0.5, y, z + dz + 0.5, thePet.mcc$get$rotationYaw(thePet), thePet.mcc$get$rotationPitch(thePet));
                        thePet.getNavigator().clearPathEntity();
                        return;
                    }
                }
            }
        }
    }
}
