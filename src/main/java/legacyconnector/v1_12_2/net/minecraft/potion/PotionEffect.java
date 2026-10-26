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

    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound writeCustomPotionEffectToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        tag.setByte("Id", (byte) org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.POTIONS.getID(potion));
        tag.setByte("Amplifier", (byte) amplifier);
        tag.setInteger("Duration", duration);
        tag.setBoolean("Ambient", isAmbient);
        tag.setBoolean("ShowParticles", showParticles);
        return tag;
    }

    public static PotionEffect readCustomPotionEffectFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        Potion potion = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.POTIONS.getValue(tag.getByte("Id") & 0xFF);
        if (potion == null) return null;
        int amplifier = Math.max(tag.getByte("Amplifier"), 0);
        boolean particles = !tag.hasKey("ShowParticles", 1) || tag.getBoolean("ShowParticles");
        return new PotionEffect(potion, tag.getInteger("Duration"), amplifier, tag.getBoolean("Ambient"), particles);
    }

    /** The legacy effect of a modern one (vanilla effects by their shared names), or null when its potion has no legacy object. */
    public static PotionEffect mcc$fromModern(net.minecraft.world.effect.MobEffectInstance e) {
        var id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value());
        if (id == null) return null;
        Potion potion = Potion.getPotionFromResourceLocation(id.toString());
        if (potion == null) return null;
        return new PotionEffect(potion, e.getDuration(), e.getAmplifier(), e.isAmbient(), e.isVisible());
    }

    /** The modern effect, or null when the potion has no modern counterpart (mod potions are not bridged yet). */
    public net.minecraft.world.effect.MobEffectInstance mcc$toModern() {
        var modern = potion == null ? null : potion.mcc$modern();
        return modern == null ? null : new net.minecraft.world.effect.MobEffectInstance(modern, duration, amplifier, isAmbient, showParticles);
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
