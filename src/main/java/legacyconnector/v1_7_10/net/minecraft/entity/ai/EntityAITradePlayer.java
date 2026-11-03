package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** A villager stands still while a player trades with it. */
public class EntityAITradePlayer extends EntityAIBase {
    private final EntityVillager villager;

    public EntityAITradePlayer(EntityVillager villager) {
        this.villager = villager;
        setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        if (!villager.isEntityAlive() || villager.isInWater() || villager.mcc$entity == null || !villager.mcc$entity.onGround()) return false;
        EntityPlayer customer = villager.getCustomer();
        return customer != null && villager.getDistanceSqToEntity(customer) <= 16.0D && EntityPlayer.mcc$get$openContainer(customer) != null;
    }

    @Override
    public void startExecuting() {
        villager.getNavigator().clearPathEntity();
    }

    @Override
    public void resetTask() {
        villager.setCustomer(null);
    }
}
