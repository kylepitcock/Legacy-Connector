package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.village.Village;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Villagers breed: a willing adult in a village with fewer villagers than 35 percent of its doors walks to a willing neighbour and,
 * after a short while together, a child is born.
 */
public class EntityAIVillagerMate extends EntityAIBase {
    private final EntityVillager villagerObj;
    private EntityVillager mate;
    private final World worldObj;
    private int matingTimeout;
    private Village villageObj;

    public EntityAIVillagerMate(EntityVillager villager) {
        this.villagerObj = villager;
        this.worldObj = Entity.mcc$get$worldObj(villager);
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (worldObj == null || villagerObj.getGrowingAge() != 0) return false;
        if (villagerObj.getRNG().nextInt(500) != 0) return false;
        var villages = World.mcc$get$villageCollectionObj(worldObj);
        if (villages == null) return false;
        villageObj = villages.findNearestVillage(MathHelper.floor_double(villagerObj.mcc$x()), MathHelper.floor_double(villagerObj.mcc$y()),
                MathHelper.floor_double(villagerObj.mcc$z()), 0);
        if (villageObj == null) return false;
        if (!checkSufficientDoorsPresentForNewVillager() || !villagerObj.getIsWillingToMate(true)) return false;
        Entity entity = worldObj.findNearestEntityWithinAABB(EntityVillager.class, Entity.mcc$get$boundingBox(villagerObj).expand(8.0D, 3.0D, 8.0D),
                villagerObj);
        if (entity == null) return false;
        mate = (EntityVillager) entity;
        return mate.getGrowingAge() == 0 && mate.getIsWillingToMate(true);
    }

    @Override
    public void startExecuting() {
        matingTimeout = 300;
        villagerObj.setMating(true);
    }

    @Override
    public void resetTask() {
        villageObj = null;
        mate = null;
        villagerObj.setMating(false);
    }

    @Override
    public boolean continueExecuting() {
        return matingTimeout >= 0 && villageObj != null && checkSufficientDoorsPresentForNewVillager() && villagerObj.getGrowingAge() == 0
                && villagerObj.getIsWillingToMate(false);
    }

    @Override
    public void updateTask() {
        --matingTimeout;
        villagerObj.getLookHelper().setLookPositionWithEntity(mate, 10.0F, 30.0F);
        if (villagerObj.getDistanceSqToEntity(mate) > 2.25D) {
            villagerObj.getNavigator().tryMoveToEntityLiving(mate, 0.25D);
        } else if (matingTimeout == 0 && mate.isMating()) {
            giveBirth();
        }
        if (villagerObj.getRNG().nextInt(35) == 0) worldObj.setEntityState(villagerObj, (byte) 12);
    }

    private boolean checkSufficientDoorsPresentForNewVillager() {
        int doors = (int) (villageObj.getNumVillageDoors() * 0.35D);
        return villageObj.getNumVillagers() < doors;
    }

    private void giveBirth() {
        EntityVillager child = villagerObj.createChild(mate);
        mate.setGrowingAge(6000);
        villagerObj.setGrowingAge(6000);
        child.setGrowingAge(-24000);
        child.setLocationAndAngles(villagerObj.mcc$x(), villagerObj.mcc$y(), villagerObj.mcc$z(), 0.0F, 0.0F);
        worldObj.spawnEntityInWorld(child);
        worldObj.setEntityState(child, (byte) 12);
    }
}
