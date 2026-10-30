package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;

/** A villager stands still while its customer (within four blocks, with a screen open) trades. */
public class EntityAITradePlayer extends EntityAIBase {
    private final EntityVillager villager;

    public EntityAITradePlayer(EntityVillager villager) {
        this.villager = villager;
        setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        if (!villager.isEntityAlive() || villager.isInWater() || !Entity.mcc$get$onGround(villager) || Entity.mcc$get$velocityChanged(villager)) return false;
        EntityPlayer player = villager.getCustomer();
        if (player == null || villager.getDistanceSq(player) > 16.0D) return false;
        return EntityPlayer.mcc$get$openContainer(player) != null;
    }

    @Override
    public void startExecuting() {
        villager.getNavigator().clearPath();
    }

    @Override
    public void resetTask() {
        villager.setCustomer(null);
    }
}
