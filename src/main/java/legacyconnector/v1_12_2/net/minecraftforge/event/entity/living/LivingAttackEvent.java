package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;

@legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
public class LivingAttackEvent extends LivingEvent {
    private final DamageSource source;
    private final float amount;

    public LivingAttackEvent(EntityLivingBase entity, DamageSource source, float amount) {
        super(entity);
        this.source = source;
        this.amount = amount;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }
}
