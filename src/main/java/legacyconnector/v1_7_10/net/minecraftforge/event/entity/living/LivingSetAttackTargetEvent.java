package legacyconnector.v1_7_10.net.minecraftforge.event.entity.living;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;

/** A mob picked a new attack target (null when it stops attacking). */
public class LivingSetAttackTargetEvent extends LivingEvent {
    public final EntityLivingBase target;

    public LivingSetAttackTargetEvent(EntityLivingBase entity, EntityLivingBase target) {
        super(entity);
        this.target = target;
    }
}
