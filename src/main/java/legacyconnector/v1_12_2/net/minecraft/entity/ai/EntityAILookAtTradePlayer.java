package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;

/** A trading villager looks at its customer. */
public class EntityAILookAtTradePlayer extends EntityAIWatchClosest {
    private final EntityVillager villager;

    public EntityAILookAtTradePlayer(EntityVillager villager) {
        super(villager, EntityPlayer.class, 8.0F);
        this.villager = villager;
    }

    @Override
    public boolean shouldExecute() {
        if (villager.isTrading()) {
            closestEntity = villager.getCustomer();
            return true;
        }
        return false;
    }
}
