package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.village.Village;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Two willing adult villagers in a village with room (more doors than villagers allow) make a child. */
public class EntityAIVillagerMate extends EntityAIBase {
    private final EntityVillager villager;
    private EntityVillager mate;
    private final World world;
    private int matingTimeout;
    Village village;

    public EntityAIVillagerMate(EntityVillager villager) {
        this.villager = villager;
        this.world = Entity.mcc$get$world(villager);
        setMutexBits(3);
    }

    private World mcc$world() {
        return world != null ? world : Entity.mcc$get$world(villager);
    }

    @Override
    public boolean shouldExecute() {
        if (villager.getGrowingAge() != 0 || villager.getRNG().nextInt(500) != 0) return false;
        World w = mcc$world();
        if (w == null || w.getVillageCollection() == null) return false;
        village = w.getVillageCollection().getNearestVillage(new BlockPos(villager), 0);
        if (village == null || !checkSufficientDoorsPresentForNewVillager() || !villager.getIsWillingToMate(true)) return false;
        Entity e = w.findNearestEntityWithinAABB(EntityVillager.class, villager.getEntityBoundingBox().grow(8.0D, 3.0D, 8.0D), villager);
        if (!(e instanceof EntityVillager v)) return false;
        mate = v;
        return mate.getGrowingAge() == 0 && mate.getIsWillingToMate(true);
    }

    @Override
    public void startExecuting() {
        matingTimeout = 300;
        villager.setMating(true);
    }

    @Override
    public void resetTask() {
        village = null;
        mate = null;
        villager.setMating(false);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return matingTimeout >= 0 && village != null && checkSufficientDoorsPresentForNewVillager() && villager.getGrowingAge() == 0
                && villager.getIsWillingToMate(false);
    }

    @Override
    public void updateTask() {
        --matingTimeout;
        villager.getLookHelper().setLookPositionWithEntity(mate, 10.0F, 30.0F);
        if (villager.getDistanceSq(mate) > 2.25D) villager.getNavigator().tryMoveToEntityLiving(mate, 0.25D);
        else if (matingTimeout == 0 && mate.isMating()) giveBirth();
        if (villager.getRNG().nextInt(35) == 0 && mcc$world() != null) mcc$world().setEntityState(villager, (byte) 12);
    }

    private boolean checkSufficientDoorsPresentForNewVillager() {
        if (!village.isMatingSeason()) return false;
        int max = (int) (village.getNumVillageDoors() * 0.35D);
        return village.getNumVillagers() < max;
    }

    private void giveBirth() {
        World w = mcc$world();
        if (w == null) return;
        EntityVillager child = villager.createChild(mate);
        mate.setGrowingAge(6000);
        villager.setGrowingAge(6000);
        mate.setIsWillingToMate(false);
        villager.setIsWillingToMate(false);
        child.setGrowingAge(-24000);
        child.setLocationAndAngles(villager.mcc$x(), villager.mcc$y(), villager.mcc$z(), 0.0F, 0.0F);
        w.spawnEntity(child);
        w.setEntityState(child, (byte) 12);
    }
}
