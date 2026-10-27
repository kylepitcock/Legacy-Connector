package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;

@legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
public class LivingHurtEvent extends LivingEvent {
    private final DamageSource source;
    private float amount;

    public LivingHurtEvent(EntityLivingBase entity, DamageSource source, float amount) {
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

    public void setAmount(float amount) {
        this.amount = amount;
    }
}
