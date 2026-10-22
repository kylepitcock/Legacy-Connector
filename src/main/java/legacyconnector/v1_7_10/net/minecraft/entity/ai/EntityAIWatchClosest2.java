package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;

public class EntityAIWatchClosest2 extends EntityAIWatchClosest {
    public EntityAIWatchClosest2(EntityLiving entity, Class<?> watchTarget, float distance, float chance) {
        super(entity, watchTarget, distance, chance);
        setMutexBits(3);
    }
}
