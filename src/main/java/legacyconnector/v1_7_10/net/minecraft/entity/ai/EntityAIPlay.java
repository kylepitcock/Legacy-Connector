package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;

/** Child villagers play: now and then a child walks to another child (or to a random spot nearby) for up to a thousand ticks. */
public class EntityAIPlay extends EntityAIBase {
    private final EntityVillager villagerObj;
    private EntityLivingBase targetVillager;
    private final double field_75261_c;
    private int playTime;

    public EntityAIPlay(EntityVillager villager, double speed) {
        this.villagerObj = villager;
        this.field_75261_c = speed;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (villagerObj.getGrowingAge() >= 0) return false;
        if (villagerObj.getRNG().nextInt(400) != 0) return false;
        var world = Entity.mcc$get$worldObj(villagerObj);
        if (world == null) return false;
        List<?> list = world.getEntitiesWithinAABB(EntityVillager.class, Entity.mcc$get$boundingBox(villagerObj).expand(6.0D, 3.0D, 6.0D));
        double best = Double.MAX_VALUE;
        for (Object o : list) {
            EntityVillager other = (EntityVillager) o;
            if (other != villagerObj && !other.isPlaying() && other.getGrowingAge() < 0) {
                double distance = other.getDistanceSqToEntity(villagerObj);
                if (distance <= best) {
                    best = distance;
                    targetVillager = other;
                }
            }
        }
        if (targetVillager == null) {
            Vec3 spot = RandomPositionGenerator.findRandomTarget(villagerObj, 16, 3);
            if (spot == null) return false;
        }
        return true;
    }

    @Override
    public boolean continueExecuting() {
        return playTime > 0;
    }

    @Override
    public void startExecuting() {
        if (targetVillager != null) villagerObj.setPlaying(true);
        playTime = 1000;
    }

    @Override
    public void resetTask() {
        villagerObj.setPlaying(false);
        targetVillager = null;
    }

    @Override
    public void updateTask() {
        --playTime;
        if (targetVillager != null) {
            if (villagerObj.getDistanceSqToEntity(targetVillager) > 4.0D) villagerObj.getNavigator().tryMoveToEntityLiving(targetVillager, field_75261_c);
        } else if (villagerObj.getNavigator().noPath()) {
            Vec3 spot = RandomPositionGenerator.findRandomTarget(villagerObj, 16, 3);
            if (spot == null) return;
            villagerObj.getNavigator().tryMoveToXYZ(spot.xCoord, spot.yCoord, spot.zCoord, field_75261_c);
        }
    }
}
