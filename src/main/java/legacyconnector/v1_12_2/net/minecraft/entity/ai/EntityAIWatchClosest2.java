package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Looks at the nearest entity of a class, claiming the movement too (villagers while trading). */
public class EntityAIWatchClosest2 extends EntityAIWatchClosest {
    public EntityAIWatchClosest2(EntityLiving entity, Class<? extends Entity> watchTargetClass, float maxDistance, float chance) {
        super(entity, watchTargetClass, maxDistance, chance);
        setMutexBits(3);
    }
}
