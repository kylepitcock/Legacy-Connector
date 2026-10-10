package mccompat.v1_7_10.net.minecraft.potion;

import net.minecraft.world.effect.MobEffectInstance;
import org.kylepitcock.mccompat.v1_7_10.bridge.PotionBridge;

public class PotionEffect {
    private final int potionID;
    private final int duration;
    private final int amplifier;
    private boolean isAmbient;

    public PotionEffect(int id, int duration) {
        this(id, duration, 0);
    }

    public PotionEffect(int id, int duration, int amplifier) {
        this(id, duration, amplifier, false);
    }

    public PotionEffect(int id, int duration, int amplifier, boolean ambient) {
        this.potionID = id;
        this.duration = duration;
        this.amplifier = amplifier;
        this.isAmbient = ambient;
    }

    public PotionEffect(PotionEffect other) {
        this(other.potionID, other.duration, other.amplifier, other.isAmbient);
    }

    public int getPotionID() {
        return potionID;
    }

    public int getDuration() {
        return duration;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public boolean getIsAmbient() {
        return isAmbient;
    }

    public String getEffectName() {
        return "potion." + potionID;
    }

    public MobEffectInstance mcc$modern() {
        return new MobEffectInstance(PotionBridge.effect(potionID), duration, amplifier, isAmbient, true);
    }
}
