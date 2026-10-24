package legacyconnector.v1_7_10.net.minecraft.potion;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.PotionBridge;

/**
 * Legacy potion type. Ids 1-23 are the vanilla effects (mapped onto modern {@code MobEffects}); any other potion a
 * mod constructs is registered as a modern mob effect whose ticking calls back into {@link #performEffect}.
 * Mods traditionally enlarge {@link #potionTypes} by reflection before adding ids above 31; that keeps working.
 */
public class Potion {
    public static Potion[] potionTypes = new Potion[32];
    public static final Potion field_76423_b = null;
    public static final Potion moveSpeed = new Potion(1, false, 8171462).setPotionName("potion.moveSpeed").setIconIndex(0, 0);
    public static final Potion moveSlowdown = new Potion(2, true, 5926017).setPotionName("potion.moveSlowdown").setIconIndex(1, 0);
    public static final Potion digSpeed = new Potion(3, false, 14270531).setPotionName("potion.digSpeed").setIconIndex(2, 0).setEffectiveness(1.5D);
    public static final Potion digSlowdown = new Potion(4, true, 4866583).setPotionName("potion.digSlowDown").setIconIndex(3, 0);
    public static final Potion damageBoost = new Potion(5, false, 9643043).setPotionName("potion.damageBoost").setIconIndex(4, 0);
    public static final Potion heal = new Potion(6, false, 16262179).setPotionName("potion.heal");
    public static final Potion harm = new Potion(7, true, 4393481).setPotionName("potion.harm");
    public static final Potion jump = new Potion(8, false, 7889559).setPotionName("potion.jump").setIconIndex(2, 1);
    public static final Potion confusion = new Potion(9, true, 5578058).setPotionName("potion.confusion").setIconIndex(3, 1).setEffectiveness(0.25D);
    public static final Potion regeneration = new Potion(10, false, 13458603).setPotionName("potion.regeneration").setIconIndex(7, 0).setEffectiveness(0.25D);
    public static final Potion resistance = new Potion(11, false, 10044730).setPotionName("potion.resistance").setIconIndex(6, 1);
    public static final Potion fireResistance = new Potion(12, false, 14981690).setPotionName("potion.fireResistance").setIconIndex(7, 1);
    public static final Potion waterBreathing = new Potion(13, false, 3035801).setPotionName("potion.waterBreathing").setIconIndex(0, 2);
    public static final Potion invisibility = new Potion(14, false, 8356754).setPotionName("potion.invisibility").setIconIndex(0, 1);
    public static final Potion blindness = new Potion(15, true, 2039587).setPotionName("potion.blindness").setIconIndex(5, 1).setEffectiveness(0.25D);
    public static final Potion nightVision = new Potion(16, false, 2039713).setPotionName("potion.nightVision").setIconIndex(4, 1);
    public static final Potion hunger = new Potion(17, true, 5797459).setPotionName("potion.hunger").setIconIndex(1, 1);
    public static final Potion weakness = new Potion(18, true, 4738376).setPotionName("potion.weakness").setIconIndex(5, 0);
    public static final Potion poison = new Potion(19, true, 5149489).setPotionName("potion.poison").setIconIndex(6, 0).setEffectiveness(0.25D);
    public static final Potion wither = new Potion(20, true, 3484199).setPotionName("potion.wither").setIconIndex(1, 2).setEffectiveness(0.25D);
    public static final Potion healthBoost = new Potion(21, false, 16284963).setPotionName("potion.healthBoost").setIconIndex(2, 2);
    public static final Potion absorption = new Potion(22, false, 2445989).setPotionName("potion.absorption").setIconIndex(2, 2);
    public static final Potion saturation = new Potion(23, false, 16262179).setPotionName("potion.saturation");
    public static final Potion field_76409_A = null;
    public static final Potion field_76410_B = null;
    public static final Potion field_76411_C = null;
    public static final Potion field_76405_D = null;
    public static final Potion field_76406_E = null;
    public static final Potion field_76407_F = null;
    public static final Potion field_76408_G = null;

    public final int id;
    private String name = "";
    private int statusIconIndex = -1;
    private final boolean isBadEffect;
    private double effectiveness;
    private boolean usable;
    private final int liquidColor;

    protected Potion(int id, boolean isBadEffect, int liquidColor) {
        this.id = id;
        if (id >= potionTypes.length) {
            // Legacy code crashed here unless the array had been enlarged; enlarging is harmless and friendlier.
            potionTypes = java.util.Arrays.copyOf(potionTypes, id + 1);
        }
        potionTypes[id] = this;
        this.isBadEffect = isBadEffect;
        this.effectiveness = isBadEffect ? 0.5D : 1.0D;
        this.liquidColor = liquidColor;
        if (!PotionBridge.isVanilla(id)) PotionBridge.registerLegacy(this);
    }

    public int getId() {
        return id;
    }

    public void performEffect(EntityLivingBase entity, int amplifier) {
    }

    public void affectEntity(EntityLivingBase source, EntityLivingBase target, int amplifier, double potency) {
    }

    public boolean isInstant() {
        return false;
    }

    public boolean isReady(int duration, int amplifier) {
        if (id == regeneration.id || id == poison.id || id == wither.id) {
            int k = (id == wither.id ? 40 : id == poison.id ? 25 : 50) >> amplifier;
            return k <= 0 || duration % k == 0;
        }
        return id == hunger.id;
    }

    public Potion setPotionName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    protected Potion setIconIndex(int x, int y) {
        this.statusIconIndex = x + y * 8;
        return this;
    }

    public boolean hasStatusIcon() {
        return statusIconIndex >= 0;
    }

    public int getStatusIconIndex() {
        return statusIconIndex;
    }

    public boolean isBadEffect() {
        return isBadEffect;
    }

    public static String getDurationString(PotionEffect effect) {
        if (effect.getIsPotionDurationMax()) return "**:**";
        int seconds = effect.getDuration() / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    protected Potion setEffectiveness(double effectiveness) {
        this.effectiveness = effectiveness;
        return this;
    }

    /** Attribute modifiers registered by legacy code: attribute -> {uuid, amount, operation}. Not applied to modern attributes yet. */
    public final java.util.Map<legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.IAttribute, Object[]> mcc$attributeModifiers = new java.util.HashMap<>();

    public Potion registerPotionAttributeModifier(legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.IAttribute attribute, String uuid, double amount, int operation) {
        mcc$attributeModifiers.put(attribute, new Object[] {uuid, amount, operation});
        return this;
    }

    public double getEffectiveness() {
        return effectiveness;
    }

    public boolean isUsable() {
        return usable;
    }

    public int getLiquidColor() {
        return liquidColor;
    }

    @Override
    public String toString() {
        return "LegacyPotion{" + id + " " + name + "}";
    }
}
