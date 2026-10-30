package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Villager children chase each other (or run around) for a while. */
public class EntityAIPlay extends EntityAIBase {
    private final EntityVillager villager;
    private EntityLivingBase targetVillager;
    private final double speed;
    private int playTime;

    public EntityAIPlay(EntityVillager villager, double speed) {
        this.villager = villager;
        this.speed = speed;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (villager.getGrowingAge() >= 0 || villager.getRNG().nextInt(400) != 0) return false;
        var level = villager.mcc$entity == null ? null : villager.mcc$entity.level();
        double best = Double.MAX_VALUE;
        for (EntityVillager v : EntityQueries1122.within(level, villager.getEntityBoundingBox().grow(6.0D, 3.0D, 6.0D), EntityVillager.class, villager, null)) {
            if (v != villager && !v.isPlaying() && v.getGrowingAge() < 0) {
                double d = v.getDistanceSq(villager);
                if (d <= best) {
                    best = d;
                    targetVillager = v;
                }
            }
        }
        return targetVillager != null || RandomPositionGenerator.findRandomTarget(villager, 16, 3) != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return playTime > 0;
    }

    @Override
    public void startExecuting() {
        if (targetVillager != null) villager.setPlaying(true);
        playTime = 1000;
    }

    @Override
    public void resetTask() {
        villager.setPlaying(false);
        targetVillager = null;
    }

    @Override
    public void updateTask() {
        --playTime;
        if (targetVillager != null) {
            if (villager.getDistanceSq(targetVillager) > 4.0D) villager.getNavigator().tryMoveToEntityLiving(targetVillager, speed);
        } else if (villager.getNavigator().noPath()) {
            Vec3d v = RandomPositionGenerator.findRandomTarget(villager, 16, 3);
            if (v != null) villager.getNavigator().tryMoveToXYZ(v.x, v.y, v.z, speed);
        }
    }
}
