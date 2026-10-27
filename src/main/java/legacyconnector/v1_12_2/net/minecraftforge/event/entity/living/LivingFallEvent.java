package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;

@legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
public class LivingFallEvent extends LivingEvent {
    private float distance;
    private float damageMultiplier;

    public LivingFallEvent(EntityLivingBase entity, float distance, float damageMultiplier) {
        super(entity);
        this.distance = distance;
        this.damageMultiplier = damageMultiplier;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
    }
}
