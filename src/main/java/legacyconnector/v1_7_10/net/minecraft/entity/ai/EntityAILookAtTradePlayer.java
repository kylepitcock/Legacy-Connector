package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** A villager that is trading looks at its customer. */
public class EntityAILookAtTradePlayer extends EntityAIBase {
    private final EntityVillager theMerchant;
    private EntityPlayer player;

    public EntityAILookAtTradePlayer(EntityVillager villager) {
        this.theMerchant = villager;
        setMutexBits(2);
    }

    @Override
    public boolean shouldExecute() {
        if (!theMerchant.isTrading()) return false;
        player = theMerchant.getCustomer();
        return player != null && theMerchant.getDistanceSqToEntity(player) <= 64.0D;
    }

    @Override
    public boolean continueExecuting() {
        return theMerchant.isTrading() && player != null && theMerchant.getCustomer() == player;
    }

    @Override
    public void resetTask() {
        player = null;
    }

    @Override
    public void updateTask() {
        if (player != null) theMerchant.getLookHelper().setLookPositionWithEntity(player, 10.0F, theMerchant.getVerticalFaceSpeed());
    }
}
