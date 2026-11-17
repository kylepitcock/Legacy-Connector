package legacyconnector.v1_12_2.net.minecraft.potion;

/** 1.12.2 base of the instant health potions (Instant Health, Instant Damage): the effect is applied once, never lasting. Mods extend it for instant effects. */
public class PotionHealth extends Potion {
    public PotionHealth(boolean isBadEffectIn, int liquidColorIn) {
        super(isBadEffectIn, liquidColorIn);
    }

    @Override
    public boolean isInstant() {
        return true;
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return duration >= 1;
    }
}
