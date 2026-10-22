package legacyconnector.v1_7_10.net.minecraftforge.event.entity.living;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;

@Cancelable
public class LivingDeathEvent extends LivingEvent {
    public final DamageSource source;

    public LivingDeathEvent(EntityLivingBase entity, DamageSource source) {
        super(entity);
        this.source = source;
    }
}
