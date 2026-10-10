package mccompat.v1_7_10.net.minecraftforge.event.entity.living;

import mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;

@Cancelable
public class LivingFallEvent extends LivingEvent {
    public float distance;

    public LivingFallEvent(EntityLivingBase entity, float distance) {
        super(entity);
        this.distance = distance;
    }
}
