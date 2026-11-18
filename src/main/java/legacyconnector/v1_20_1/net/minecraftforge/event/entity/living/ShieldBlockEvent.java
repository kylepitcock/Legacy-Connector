package legacyconnector.v1_20_1.net.minecraftforge.event.entity.living;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/** Forge 1.20.1's shield block event; posted from NeoForge's {@code LivingShieldBlockEvent}. */
public class ShieldBlockEvent extends LivingEvent implements ICancellableEvent {
    private final DamageSource source;
    private final float originalBlocked;
    private float dmgBlocked;
    private boolean shieldTakesDamage = true;

    public ShieldBlockEvent(LivingEntity blocker, DamageSource source, float blocked) {
        super(blocker);
        this.source = source;
        this.originalBlocked = blocked;
        this.dmgBlocked = blocked;
    }

    public DamageSource getDamageSource() {
        return source;
    }

    public float getOriginalBlockedDamage() {
        return originalBlocked;
    }

    public float getBlockedDamage() {
        return dmgBlocked;
    }

    public void setBlockedDamage(float blockedDamage) {
        this.dmgBlocked = blockedDamage;
    }

    public boolean shieldTakesDamage() {
        return shieldTakesDamage;
    }

    public void setShieldTakesDamage(boolean takesDamage) {
        this.shieldTakesDamage = takesDamage;
    }
}
