package legacyconnector.v1_7_10.net.minecraft.potion;

import net.minecraft.world.effect.MobEffectInstance;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.PotionBridge;

public class PotionEffect {
    private final int potionID;
    private final int duration;
    private final int amplifier;
    private boolean isAmbient;
    private boolean isPotionDurationMax;

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

    public boolean getIsPotionDurationMax() {
        return isPotionDurationMax;
    }

    public void setPotionDurationMax(boolean max) {
        this.isPotionDurationMax = max;
    }

    public String getEffectName() {
        Potion p = potionID >= 0 && potionID < Potion.potionTypes.length ? Potion.potionTypes[potionID] : null;
        return p != null ? p.getName() : "potion." + potionID;
    }

    private java.util.List<legacyconnector.v1_7_10.net.minecraft.item.ItemStack> curativeItems;

    public java.util.List<legacyconnector.v1_7_10.net.minecraft.item.ItemStack> getCurativeItems() {
        if (curativeItems == null) {
            curativeItems = new java.util.ArrayList<>();
            curativeItems.add(new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.milk_bucket));
        }
        return curativeItems;
    }

    public void setCurativeItems(java.util.List<legacyconnector.v1_7_10.net.minecraft.item.ItemStack> items) {
        curativeItems = items;
    }

    public static PotionEffect mcc$wrap(MobEffectInstance modern) {
        int id = PotionBridge.idOf(modern.getEffect());
        return id < 0 ? null : new PotionEffect(id, modern.getDuration(), modern.getAmplifier(), modern.isAmbient());
    }

    public MobEffectInstance mcc$modern() {
        return new MobEffectInstance(PotionBridge.effect(potionID), duration, amplifier, isAmbient, true);
    }
}
