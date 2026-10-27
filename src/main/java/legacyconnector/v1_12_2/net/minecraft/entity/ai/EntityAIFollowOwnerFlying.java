package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityTameable;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Follows the owner (flying pets). */
public class EntityAIFollowOwnerFlying extends EntityAIFollowOwner {
    public EntityAIFollowOwnerFlying(EntityTameable tameable, double followSpeed, float minDist, float maxDist) {
        super(tameable, followSpeed, minDist, maxDist);
    }
}
