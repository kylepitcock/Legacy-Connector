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
        var own = org.kylepitcock.legacyconnector.v1_12_2.entity.Effects1122.holder(this);
        if (own != null) return own;
        var name = getRegistryName();
        if (name == null) return null;
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(name.mcc$modern())
                .map(h -> (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) h).orElse(null);
    }

    // ------------------------------------------------------------------ effects on entities (area E, 1.12.2)

    /** One tick of a lasting effect, when {@link #isReady} says so (vanilla effects tick as their modern ones; a mod potion overrides this). */
    public void performEffect(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity, int amplifier) {
        var modern = mcc$modern();
        if (modern != null && !(modern.value() instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Effects1122.LegacyEffect)
                && entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            modern.value().applyEffectTick(l, amplifier);
        }
    }

    /** Whether {@link #performEffect} runs this tick (vanilla effects by their modern intervals; mod potions never unless they override this). */
    public boolean isReady(int duration, int amplifier) {
        var modern = mcc$modern();
        if (modern != null && !(modern.value() instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Effects1122.LegacyEffect)) {
            return modern.value().shouldApplyEffectTickThisTick(duration, amplifier);
        }
        return false;
    }

    /** An instant effect (splash potions, area clouds); a lasting one just ticks once. */
    public void affectEntity(legacyconnector.v1_12_2.net.minecraft.entity.Entity source, legacyconnector.v1_12_2.net.minecraft.entity.Entity indirectSource,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity, int amplifier, double health) {
        var modern = mcc$modern();
        if (modern != null && !(modern.value() instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Effects1122.LegacyEffect)
                && entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            modern.value().applyInstantenousEffect(source == null ? null : source.mcc$entity, indirectSource == null ? null : indirectSource.mcc$entity, l, amplifier, health);
            return;
        }
        performEffect(entity, amplifier);
    }

    /** "m:ss", or "**:**" for an effect without an end. */
    public static String getPotionDurationString(PotionEffect effect, float durationFactor) {
        if (effect.getIsPotionDurationMax()) return "**:**";
        return legacyconnector.v1_12_2.net.minecraft.util.StringUtils.ticksToElapsedTime(net.minecraft.util.Mth.floor(effect.getDuration() * durationFactor));
    }

    public void removeAttributesModifiersFromEntity(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity,
            legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AbstractAttributeMap map, int amplifier) {
        for (var e : attributeModifierMap.entrySet()) {
            var instance = map.getAttributeInstance(e.getKey());
            if (instance != null) instance.removeModifier(e.getValue());
        }
    }

    public void applyAttributesModifiersToEntity(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity,
            legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AbstractAttributeMap map, int amplifier) {
        for (var e : attributeModifierMap.entrySet()) {
            var instance = map.getAttributeInstance(e.getKey());
            if (instance == null) continue;
            var mod = e.getValue();
            instance.removeModifier(mod);
            instance.applyModifier(new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier(mod.getID(), getName() + " " + amplifier,
                    getAttributeModifierAmount(amplifier, mod), mod.getOperation()));
        }
    }

    /** Forge: what cures the effect (a milk bucket). */
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getCurativeItems() {
        java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> list = new java.util.ArrayList<>();
        list.add(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.MILK_BUCKET));
        return list;
    }

    /** Forge: whether the effect shows in the inventory and HUD (mods hide their own). */
    public boolean shouldRender(PotionEffect effect) {
        return true;
    }

    public boolean shouldRenderInvText(PotionEffect effect) {
        return true;
    }

    public boolean shouldRenderHUD(PotionEffect effect) {
        return true;
    }

    /** Forge client hooks for drawing a mod's own icon; vanilla effects are drawn by the modern screens. */
    public void renderInventoryEffect(int x, int y, PotionEffect effect, legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc) {
    }

    public void renderInventoryEffect(PotionEffect effect, legacyconnector.v1_12_2.net.minecraft.client.gui.Gui gui, int x, int y, float z) {
    }

    public void renderHUDEffect(PotionEffect effect, legacyconnector.v1_12_2.net.minecraft.client.gui.Gui gui, int x, int y, float z, float alpha) {
    }
}
