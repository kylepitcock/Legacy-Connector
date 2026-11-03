package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityIronGolem;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager;

/** A villager child follows an iron golem that holds out a poppy, and takes it. */
public class EntityAIFollowGolem extends EntityAIBase {
    private final EntityVillager theVillager;
    private EntityIronGolem theGolem;
    private int takeGolemRoseTick;
    private boolean tookGolemRose;

    public EntityAIFollowGolem(EntityVillager villager) {
        this.theVillager = villager;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (theVillager.getGrowingAge() >= 0) return false;
        var world = legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$worldObj(theVillager);
        if (world == null || !world.isDaytime()) return false;
        var box = legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$boundingBox(theVillager).expand(6.0D, 2.0D, 6.0D);
        List<?> golems = world.getEntitiesWithinAABB(EntityIronGolem.class, box);
        if (golems.isEmpty()) return false;
        for (Object o : golems) {
            EntityIronGolem golem = (EntityIronGolem) o;
            if (golem.getHoldRoseTick() > 0) {
                theGolem = golem;
                break;
            }
        }
        return theGolem != null;
    }

    @Override
    public boolean continueExecuting() {
        return theGolem != null && theGolem.getHoldRoseTick() > 0;
    }

    @Override
    public void startExecuting() {
        takeGolemRoseTick = theVillager.getRNG().nextInt(320);
        tookGolemRose = false;
        theGolem.getNavigator().clearPathEntity();
    }

    @Override
    public void resetTask() {
        theGolem = null;
        theVillager.getNavigator().clearPathEntity();
    }

    @Override
    public void updateTask() {
        theVillager.getLookHelper().setLookPositionWithEntity(theGolem, 30.0F, 30.0F);
        if (theGolem.getHoldRoseTick() == takeGolemRoseTick) {
            theVillager.getNavigator().tryMoveToEntityLiving(theGolem, 0.5D);
            tookGolemRose = true;
        }
        if (tookGolemRose && theVillager.getDistanceSqToEntity(theGolem) < 4.0D) {
            theGolem.setHoldingRose(false);
            theVillager.getNavigator().clearPathEntity();
        }
    }
}
