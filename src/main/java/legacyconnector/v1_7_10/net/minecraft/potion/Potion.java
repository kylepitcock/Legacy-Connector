package legacyconnector.v1_7_10.net.minecraft.potion;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.AttributeModifier;
import legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.BaseAttributeMap;
import legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.IAttribute;
import legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.IAttributeInstance;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.PotionBridge;

/**
 * Legacy potion type. Ids 1-23 are the vanilla effects (mapped onto modern {@code MobEffects}); any other potion a
 * mod constructs is registered as a modern mob effect whose ticking calls back into {@link #performEffect}.
 * Mods traditionally enlarge {@link #potionTypes} by reflection before adding ids above 31; that keeps working.
 */
public class Potion {
    public static Potion[] potionTypes = new Potion[32];
    public static final Potion field_76423_b = null;
    public static final Potion moveSpeed = new Potion(1, false, 8171462).setPotionName("potion.moveSpeed").setIconIndex(0, 0)
            .registerPotionAttributeModifier(SharedMonsterAttributes.movementSpeed, "91AEAA56-376B-4498-935B-2F7F68070635", 0.20000000298023224D, 2);
    public static final Potion moveSlowdown = new Potion(2, true, 5926017).setPotionName("potion.moveSlowdown").setIconIndex(1, 0)
            .registerPotionAttributeModifier(SharedMonsterAttributes.movementSpeed, "7107DE5E-7CE8-4030-940E-514C1F160890", -0.15000000596046448D, 2);
    public static final Potion digSpeed = new Potion(3, false, 14270531).setPotionName("potion.digSpeed").setIconIndex(2, 0).setEffectiveness(1.5D);
    public static final Potion digSlowdown = new Potion(4, true, 4866583).setPotionName("potion.digSlowDown").setIconIndex(3, 0);
    public static final Potion damageBoost = new Potion(5, false, 9643043).setPotionName("potion.damageBoost").setIconIndex(4, 0)
            .registerPotionAttributeModifier(SharedMonsterAttributes.attackDamage, "648D7064-6A60-4F59-8ABE-C2C23A6DD7A9", 3.0D, 2);
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
    public static final Potion weakness = new Potion(18, true, 4738376).setPotionName("potion.weakness").setIconIndex(5, 0)
            .registerPotionAttributeModifier(SharedMonsterAttributes.attackDamage, "22653B89-116E-49DC-9B6B-9971489B5BE5", 2.0D, 0);
    public static final Potion poison = new Potion(19, true, 5149489).setPotionName("potion.poison").setIconIndex(6, 0).setEffectiveness(0.25D);
    public static final Potion wither = new Potion(20, true, 3484199).setPotionName("potion.wither").setIconIndex(1, 2).setEffectiveness(0.25D);
    public static final Potion healthBoost = new Potion(21, false, 16284963).setPotionName("potion.healthBoost").setIconIndex(2, 2)
            .registerPotionAttributeModifier(SharedMonsterAttributes.maxHealth, "5D6F0BA2-1186-46AC-B896-C61C5CEE99CC", 4.0D, 0);
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

    /** The attribute modifiers the effect applies while active (scaled by {@link #func_111183_a} for the amplifier). */
    public java.util.Map<IAttribute, AttributeModifier> attributeModifierMap = new java.util.LinkedHashMap<>();

    /**
     * Adds an attribute modifier to the effect. A mod potion's modifiers on vanilla attributes are also given to its modern
     * effect, which applies them for as long as the effect lasts (amount times amplifier + 1).
     */
    public Potion registerPotionAttributeModifier(IAttribute attribute, String uuid, double amount, int operation) {
        AttributeModifier modifier = new AttributeModifier(parseLegacyUuid(uuid), getName(), amount, operation);
        attributeModifierMap.put(attribute, modifier);
        if (!PotionBridge.isVanilla(id)) PotionBridge.addModifier(this, attribute, modifier);
        return this;
    }

    /**
     * UUID.fromString as Java 8 ran it: each dash-separated group is parsed on its own and the overflow shifts out, so a
     * typo'd modifier id such as More Planets' "9623E0072-7CE8-..." (a 9 digit first group) is accepted instead of
     * failing with "UUID string too large" on Java 21.
     */
    static java.util.UUID parseLegacyUuid(String name) {
        try {
            return java.util.UUID.fromString(name);
        } catch (IllegalArgumentException e) {
            String[] c = name.split("-");
            if (c.length != 5) throw e;
            try {
                long[] v = new long[5];
                for (int i = 0; i < 5; i++) v[i] = Long.parseUnsignedLong(c[i], 16);
                long most = v[0];
                most <<= 16;
                most |= v[1];
                most <<= 16;
                most |= v[2];
                long least = v[3];
                least <<= 48;
                least |= v[4];
                return new java.util.UUID(most, least);
            } catch (NumberFormatException nfe) {
                throw e;
            }
        }
    }

    public java.util.Map<IAttribute, AttributeModifier> func_111186_k() {
        return attributeModifierMap;
    }

    /** The modifier amount at {@code amplifier}: the base amount times (amplifier + 1); strength and weakness have their own curves. */
    public double func_111183_a(int amplifier, AttributeModifier modifier) {
        if (id == 5) return 1.3D * (amplifier + 1);
        if (id == 18) return -0.5F * (amplifier + 1);
        return modifier.getAmount() * (amplifier + 1);
    }

    public void removeAttributesModifiersFromEntity(EntityLivingBase entity, BaseAttributeMap attributes, int amplifier) {
        for (var e : attributeModifierMap.entrySet()) {
            IAttributeInstance instance = attributes.getAttributeInstance(e.getKey());
            if (instance != null) instance.removeModifier(e.getValue());
        }
    }

    public void applyAttributesModifiersToEntity(EntityLivingBase entity, BaseAttributeMap attributes, int amplifier) {
        for (var e : attributeModifierMap.entrySet()) {
            IAttributeInstance instance = attributes.getAttributeInstance(e.getKey());
            if (instance == null) continue;
            AttributeModifier m = e.getValue();
            instance.removeModifier(m);
            instance.applyModifier(new AttributeModifier(m.getID(), getName() + " " + amplifier, func_111183_a(amplifier, m), m.getOperation()));
        }
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
