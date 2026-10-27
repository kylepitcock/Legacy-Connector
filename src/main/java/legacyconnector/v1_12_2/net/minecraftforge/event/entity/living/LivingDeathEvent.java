package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;

@legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
public class LivingDeathEvent extends LivingEvent {
    private final DamageSource source;

    public LivingDeathEvent(EntityLivingBase entity, DamageSource source) {
        super(entity);
        this.source = source;
    }

    public DamageSource getSource() {
        return source;
    }
}
