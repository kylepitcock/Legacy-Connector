package legacyconnector.v1_12_2.net.minecraft.potion;

import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

public class Potion extends IForgeRegistryEntry.Impl<Potion> {
    public static final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespaced<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, Potion> REGISTRY = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.POTIONS, "speed");
    private final boolean isBadEffect;

    private static void mcc$vanilla() {
        try {
            Class.forName("legacyconnector.v1_12_2.net.minecraft.init.MobEffects", true, Potion.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            // only mod potions then
        }
    }

    public static Potion getPotionFromResourceLocation(String location) {
        mcc$vanilla();
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.POTIONS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(location));
    }

    public static Potion getPotionById(int id) {
        mcc$vanilla();
        return REGISTRY.getObjectById(id);
    }

    public static int getIdFromPotion(Potion potion) {
        mcc$vanilla();
        return REGISTRY.getIDForObject(potion);
    }
    private final int liquidColor;
    private String name = "";

    protected Potion(boolean isBadEffect, int liquidColor) {
        this.isBadEffect = isBadEffect;
        this.liquidColor = liquidColor;
    }

    protected Potion setPotionName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean isBadEffect() {
        return isBadEffect;
    }

    public int getLiquidColor() {
        return liquidColor;
    }

    public boolean isInstant() {
        return false;
    }

    private int statusIconIndex = -1;
    private boolean beneficial;
    private double effectiveness;
    private final java.util.Map<legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttribute,
            legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> attributeModifierMap = new java.util.LinkedHashMap<>();

    /** The cell of the status icon in the inventory texture (8 per row). */
    protected Potion setIconIndex(int x, int y) {
        this.statusIconIndex = x + y * 8;
        return this;
    }

    public int getStatusIconIndex() {
        return statusIconIndex;
    }

    public boolean hasStatusIcon() {
        return statusIconIndex >= 0;
    }

    public Potion setBeneficial() {
        this.beneficial = true;
        return this;
    }

    public boolean isBeneficial() {
        return beneficial;
    }

    public Potion setEffectiveness(double effectiveness) {
        this.effectiveness = effectiveness;
        return this;
    }

    public double getEffectiveness() {
        return effectiveness;
    }

    /** An attribute change applied while the effect is active (the amount is scaled by the amplifier in {@link #getAttributeModifierAmount}). */
    public Potion registerPotionAttributeModifier(legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttribute attribute, String uniqueId,
            double amount, int operation) {
        attributeModifierMap.put(attribute, new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier(
                java.util.UUID.fromString(uniqueId), getName(), amount, operation));
        return this;
    }

    public java.util.Map<legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttribute,
            legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> getAttributeModifierMap() {
        return attributeModifierMap;
    }

    public double getAttributeModifierAmount(int amplifier, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier modifier) {
        return modifier.getAmount() * (amplifier + 1);
    }

    /** The modern effect of the same registry name (the vanilla effects), or null for mod potions. */
    public net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> mcc$modern() {
        var name = getRegistryName();
        if (name == null) return null;
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(name.mcc$modern())
                .map(h -> (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) h).orElse(null);
    }
}
