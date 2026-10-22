package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.Mob;

/** Line-of-sight checks, cached per tick by the host's sensing. */
public class EntitySenses {
    private final EntityLiving entity;

    public EntitySenses(EntityLiving entity) {
        this.entity = entity;
    }

    public void clearSensingCache() {
    }

    public boolean canSee(Entity target) {
        if (target == null || target.mcc$entity == null) return false;
        if (entity.mcc$entity instanceof Mob m) return m.getSensing().hasLineOfSight(target.mcc$entity);
        return entity.mcc$entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l && l.hasLineOfSight(target.mcc$entity);
    }
}
