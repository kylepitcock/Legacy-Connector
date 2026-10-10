package mccompat.v1_7_10.net.minecraftforge.event.entity.living;

import mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraftforge.event.entity.EntityEvent;

public class LivingEvent extends EntityEvent {
    public final EntityLivingBase entityLiving;

    public LivingEvent(EntityLivingBase entity) {
        super(entity);
        this.entityLiving = entity;
    }

    @Cancelable
    public static class LivingUpdateEvent extends LivingEvent {
        public LivingUpdateEvent(EntityLivingBase e) {
            super(e);
        }
    }

    public static class LivingJumpEvent extends LivingEvent {
        public LivingJumpEvent(EntityLivingBase e) {
            super(e);
        }
    }
}
