package legacyconnector.v1_7_10.net.minecraft.potion;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.PotionBridge;

/**
 * An active (or to be applied) effect. Effects read from an entity remember it: changing them ({@link #combine}, curative items)
 * changes the entity's modern effect, as changing the live object did in 1.7.10.
 */
public class PotionEffect {
    private int potionID;
    private int duration;
    private int amplifier;
    private boolean isAmbient;
    private boolean isSplashPotion;
    private boolean isPotionDurationMax;
    /** The entity this effect was read from (null for a new effect). */
    private LivingEntity mcc$owner;

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
        if (other.curativeItems != null) this.curativeItems = new java.util.ArrayList<>(other.curativeItems);
    }

    /**
     * Merges another effect of the same potion into this one: a stronger one replaces it, an equally strong longer one extends it,
     * and a non-ambient one makes it non-ambient.
     */
    public void combine(PotionEffect other) {
        if (other == null) return;
        if (other.amplifier > amplifier) {
            amplifier = other.amplifier;
            duration = other.duration;
        } else if (other.amplifier == amplifier && duration < other.duration) {
            duration = other.duration;
        } else if (!other.isAmbient && isAmbient) {
            isAmbient = other.isAmbient;
        }
        mcc$apply();
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

    public void setSplashPotion(boolean splash) {
        this.isSplashPotion = splash;
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

    @Override
    public String toString() {
        String s = amplifier > 0 ? getEffectName() + " x " + (amplifier + 1) + ", Duration: " + duration : getEffectName() + ", Duration: " + duration;
        if (isSplashPotion) s += ", Splash: true";
        return s;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PotionEffect e && potionID == e.potionID && amplifier == e.amplifier && duration == e.duration
                && isSplashPotion == e.isSplashPotion && isAmbient == e.isAmbient;
    }

    @Override
    public int hashCode() {
        return potionID;
    }

    // ------------------------------------------------------------------ NBT

    public NBTTagCompound writeCustomPotionEffectToNBT(NBTTagCompound tag) {
        tag.setByte("Id", (byte) potionID);
        tag.setByte("Amplifier", (byte) amplifier);
        tag.setInteger("Duration", duration);
        tag.setBoolean("Ambient", isAmbient);
        return tag;
    }

    /** The effect saved in {@code tag}, or null when its potion does not exist. */
    public static PotionEffect readCustomPotionEffectFromNBT(NBTTagCompound tag) {
        int id = tag.getByte("Id") & 0xFF;
        if (id < Potion.potionTypes.length && Potion.potionTypes[id] != null) {
            return new PotionEffect(id, tag.getInteger("Duration"), tag.getByte("Amplifier"), tag.getBoolean("Ambient"));
        }
        return null;
    }

    // ------------------------------------------------------------------ Forge curative items

    private java.util.List<ItemStack> curativeItems;

    public java.util.List<ItemStack> getCurativeItems() {
        if (curativeItems == null) {
            curativeItems = new java.util.ArrayList<>();
            curativeItems.add(new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.milk_bucket));
        }
        return curativeItems;
    }

    public void setCurativeItems(java.util.List<ItemStack> items) {
        curativeItems = items;
        mcc$apply();
    }

    public boolean isCurativeItem(ItemStack stack) {
        if (stack == null) return false;
        for (ItemStack c : getCurativeItems()) if (c != null && c.isItemEqual(stack)) return true;
        return false;
    }

    /** Lets {@code stack} cure the effect too (with {@link legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase#curePotionEffects}). */
    public void addCurativeItem(ItemStack stack) {
        if (stack == null || isCurativeItem(stack)) return;
        getCurativeItems().add(stack);
        mcc$apply();
    }

    /** The modern cure an item stands for: milk for the milk bucket, otherwise one per item. */
    public static EffectCure mcc$cureFor(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        if (stack.getItem() == legacyconnector.v1_7_10.net.minecraft.init.Items.milk_bucket) return EffectCures.MILK;
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.mcc$modern().getItem());
        return EffectCure.get("legacyconnector:item/" + key);
    }

    // ------------------------------------------------------------------ modern

    public static PotionEffect mcc$wrap(MobEffectInstance modern) {
        return mcc$wrap(modern, null);
    }

    /** A legacy view of an effect; with {@code owner}, changes to it are applied to the owner's effect. */
    public static PotionEffect mcc$wrap(MobEffectInstance modern, LivingEntity owner) {
        int id = PotionBridge.idOf(modern.getEffect());
        if (id < 0) return null;
        PotionEffect e = new PotionEffect(id, modern.getDuration(), modern.getAmplifier(), modern.isAmbient());
        e.isPotionDurationMax = modern.isInfiniteDuration();
        e.mcc$owner = owner;
        return e;
    }

    public MobEffectInstance mcc$modern() {
        MobEffectInstance instance = new MobEffectInstance(PotionBridge.effect(potionID), duration, amplifier, isAmbient, true);
        if (curativeItems != null) {
            instance.getCures().remove(EffectCures.MILK); // the legacy list decides about milk; modern cures (totem, honey) stay
            for (ItemStack stack : curativeItems) {
                EffectCure cure = mcc$cureFor(stack);
                if (cure != null) instance.getCures().add(cure);
            }
        }
        return instance;
    }

    /** Pushes a change of an effect read from an entity to the entity. */
    private void mcc$apply() {
        LivingEntity owner = mcc$owner;
        if (owner == null || owner.level().isClientSide()) return;
        var holder = PotionBridge.effectOrNull(potionID);
        if (holder == null || !owner.hasEffect(holder)) return;
        owner.forceAddEffect(mcc$modern(), null);
    }
}
