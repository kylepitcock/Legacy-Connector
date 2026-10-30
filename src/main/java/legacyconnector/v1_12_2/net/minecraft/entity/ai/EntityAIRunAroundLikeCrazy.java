package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.AbstractHorse;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/** An untamed horse with a rider runs around and either accepts the rider (by temper) or bucks it off. */
public class EntityAIRunAroundLikeCrazy extends EntityAIBase {
    private final AbstractHorse horseHost;
    private final double speed;
    private double targetX;
    private double targetY;
    private double targetZ;

    public EntityAIRunAroundLikeCrazy(AbstractHorse horse, double speed) {
        this.horseHost = horse;
        this.speed = speed;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (horseHost.isTame() || !horseHost.isBeingRidden()) return false;
        Vec3d v = RandomPositionGenerator.findRandomTarget(horseHost, 5, 4);
        if (v == null) return false;
        targetX = v.x;
        targetY = v.y;
        targetZ = v.z;
        return true;
    }

    @Override
    public void startExecuting() {
        horseHost.getNavigator().tryMoveToXYZ(targetX, targetY, targetZ, speed);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !horseHost.isTame() && !horseHost.getNavigator().noPath() && horseHost.isBeingRidden();
    }

    @Override
    public void updateTask() {
        if (horseHost.isTame() || horseHost.getRNG().nextInt(50) != 0) return;
        var passengers = horseHost.getPassengers();
        Entity rider = passengers.isEmpty() ? null : passengers.get(0);
        if (rider == null) return;
        if (rider instanceof EntityPlayer player) {
            int temper = horseHost.getTemper();
            int max = horseHost.getMaxTemper();
            if (max > 0 && horseHost.getRNG().nextInt(max) < temper
                    && !legacyconnector.v1_12_2.net.minecraftforge.event.ForgeEventFactory.onAnimalTame(horseHost, player)) {
                horseHost.setTamedBy(player);
                return;
            }
            horseHost.increaseTemper(5);
        }
        horseHost.removePassengers();
        horseHost.makeMad();
        var world = Entity.mcc$get$world(horseHost);
        if (world != null) world.setEntityState(horseHost, (byte) 6);
    }
}
