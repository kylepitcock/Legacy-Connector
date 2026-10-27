package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.EntityEvent;

/** Base of Forge's living-entity events. */
public class LivingEvent extends EntityEvent {
    private final EntityLivingBase entityLiving;

    public LivingEvent(EntityLivingBase entity) {
        super(entity);
        this.entityLiving = entity;
    }

    public EntityLivingBase getEntityLiving() {
        return entityLiving;
    }

    /** Every tick of a living entity; canceling it skips the entity's update. */
    @legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
    public static class LivingUpdateEvent extends LivingEvent {
        public LivingUpdateEvent(EntityLivingBase entity) {
            super(entity);
        }
    }

    public static class LivingJumpEvent extends LivingEvent {
        public LivingJumpEvent(EntityLivingBase entity) {
            super(entity);
        }
    }
}
