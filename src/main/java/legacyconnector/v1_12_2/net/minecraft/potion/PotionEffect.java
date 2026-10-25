package legacyconnector.v1_12_2.net.minecraft.potion;

/** A potion with a duration and amplifier (brews, food effects, mob effects). */
public class PotionEffect implements Comparable<PotionEffect> {
    private final Potion potion;
    private int duration;
    private int amplifier;
    private boolean isAmbient;
    private boolean showParticles;
    private boolean isPotionDurationMax;

    public PotionEffect(Potion potion) {
        this(potion, 0, 0);
    }

    public PotionEffect(Potion potion, int duration) {
        this(potion, duration, 0);
    }

    public PotionEffect(Potion potion, int duration, int amplifier) {
        this(potion, duration, amplifier, false, true);
    }

    public PotionEffect(Potion potion, int duration, int amplifier, boolean ambient, boolean showParticles) {
        this.potion = potion;
        this.duration = duration;
        this.amplifier = amplifier;
        this.isAmbient = ambient;
        this.showParticles = showParticles;
    }

    public PotionEffect(PotionEffect other) {
        this(other.potion, other.duration, other.amplifier, other.isAmbient, other.showParticles);
    }

    public Potion getPotion() {
        return potion;
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

    public boolean doesShowParticles() {
        return showParticles;
    }

    public boolean getIsPotionDurationMax() {
        return isPotionDurationMax;
    }

    public void setPotionDurationMax(boolean max) {
        this.isPotionDurationMax = max;
    }

    public String getEffectName() {
        return potion == null ? "" : potion.getName();
    }

    /** Keeps the stronger amplifier and the longer duration, like vanilla. */
    public boolean combine(PotionEffect other) {
        boolean changed = false;
        if (other.amplifier > amplifier) {
            amplifier = other.amplifier;
            duration = other.duration;
            changed = true;
        } else if (other.amplifier == amplifier && duration < other.duration) {
            duration = other.duration;
            changed = true;
        }
        if (!other.isAmbient && isAmbient || changed) {
            isAmbient = other.isAmbient;
            changed = true;
        }
        if (other.showParticles != showParticles) {
            showParticles = other.showParticles;
            changed = true;
        }
        return changed;
    }

    @Override
    public int compareTo(PotionEffect other) {
        if (duration > 32147 && other.duration > 32147 || isAmbient && other.isAmbient) {
            return Boolean.compare(isAmbient, other.isAmbient) * -1;
        }
        return Integer.compare(duration, other.duration);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PotionEffect e && duration == e.duration && amplifier == e.amplifier && isAmbient == e.isAmbient
                && java.util.Objects.equals(potion, e.potion);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(potion, duration, amplifier, isAmbient);
    }

    @Override
    public String toString() {
        return getEffectName() + (amplifier > 0 ? " x " + (amplifier + 1) : "") + ", Duration: " + duration;
    }
}
