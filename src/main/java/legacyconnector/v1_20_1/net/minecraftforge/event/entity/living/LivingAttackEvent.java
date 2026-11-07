package legacyconnector.v1_20_1.net.minecraftforge.event.entity.living;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/** Forge 1.20.1's attack event (before damage is applied); posted from NeoForge's {@code LivingIncomingDamageEvent}. */
public class LivingAttackEvent extends LivingEvent implements ICancellableEvent {
    private final DamageSource source;
    private final float amount;

    public LivingAttackEvent(LivingEntity entity, DamageSource source, float amount) {
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
