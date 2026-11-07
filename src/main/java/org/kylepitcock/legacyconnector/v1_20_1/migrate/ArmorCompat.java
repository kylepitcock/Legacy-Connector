package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Armor materials of 1.20.1 (an interface the mod implements, see {@link LegacyArmorMaterial}) as 1.21.1 wants them: a {@code Holder<ArmorMaterial>} for the
 * {@code ArmorItem} constructor, durability on the item properties (the item took it from the material before), and the way back for {@code getMaterial()}.
 */
public final class ArmorCompat {
    private static final Map<LegacyArmorMaterial, Holder<ArmorMaterial>> CACHE = new ConcurrentHashMap<>();
    private static final Map<ArmorMaterial, LegacyArmorMaterial> BACK = new ConcurrentHashMap<>();

    private ArmorCompat() {}

    public static Holder<ArmorMaterial> holder(LegacyArmorMaterial material) {
        if (material instanceof LegacyArmorMaterials v) return v.holder;
        return CACHE.computeIfAbsent(material, ArmorCompat::build);
    }

    private static Holder<ArmorMaterial> build(LegacyArmorMaterial m) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type t : new ArmorItem.Type[] {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
            defense.put(t, m.getDefenseForType(t));
        }
        defense.put(ArmorItem.Type.BODY, m.getDefenseForType(ArmorItem.Type.CHESTPLATE));
        SoundEvent sound = m.getEquipSound();
        ArmorMaterial built = new ArmorMaterial(defense, m.getEnchantmentValue(), BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), m::getRepairIngredient,
                List.of(new ArmorMaterial.Layer(ResourceLocation.parse(m.getName()))), m.getToughness(), m.getKnockbackResistance());
        BACK.put(built, m);
        return Holder.direct(built);
    }

    /** The item properties of an armor piece: 1.20.1 derived the durability from the material and type. */
    public static Item.Properties properties(LegacyArmorMaterial material, ArmorItem.Type type, Item.Properties props) {
        return props.durability(material.getDurabilityForType(type));
    }

    /** {@code ArmorItem.getMaterial()}. */
    public static LegacyArmorMaterial getMaterial(ArmorItem item) {
        Holder<ArmorMaterial> h = item.getMaterial();
        ArmorMaterial value = h.value();
        LegacyArmorMaterial back = BACK.get(value);
        if (back != null) return back;
        for (LegacyArmorMaterials v : LegacyArmorMaterials.values()) if (v.holder.value() == value) return v;
        return new Wrapped(value, h.unwrapKey().map(k -> k.location().getPath()).orElse("unknown"));
    }

    /** {@code ArmorItem.getEquipSound()}. */
    public static SoundEvent getEquipSound(ArmorItem item) {
        return item.getEquipSound().value();
    }

    /** A 1.21.1 material seen through the 1.20.1 interface. */
    private record Wrapped(ArmorMaterial m, String name) implements LegacyArmorMaterial {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return 0;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return m.getDefense(type);
        }

        @Override
        public int getEnchantmentValue() {
            return m.enchantmentValue();
        }

        @Override
        public SoundEvent getEquipSound() {
            return m.equipSound().value();
        }

        @Override
        public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() {
            return m.repairIngredient().get();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public float getToughness() {
            return m.toughness();
        }

        @Override
        public float getKnockbackResistance() {
            return m.knockbackResistance();
        }
    }
}
