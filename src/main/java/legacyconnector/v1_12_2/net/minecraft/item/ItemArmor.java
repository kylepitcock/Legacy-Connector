package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.init.SoundEvents;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;

/** Armour: the material gives its protection per slot. */
public class ItemArmor extends Item {
    public final EntityEquipmentSlot armorType;
    public final int damageReduceAmount;
    public final float toughness;
    public final int renderIndex;
    private final ArmorMaterial material;

    public ItemArmor(ArmorMaterial material, int renderIndex, EntityEquipmentSlot equipmentSlot) {
        this.material = material;
        this.armorType = equipmentSlot;
        this.renderIndex = renderIndex;
        this.damageReduceAmount = material.getDamageReductionAmount(equipmentSlot);
        this.toughness = material.getToughness();
        setMaxDamage(material.getDurability(equipmentSlot));
        this.maxStackSize = 1;
    }

    public ArmorMaterial getArmorMaterial() {
        return material;
    }

    public EntityEquipmentSlot getEquipmentSlot() {
        return armorType;
    }

    public int getItemEnchantability() {
        return material.getEnchantability();
    }

    /** Armour tiers (an enum in 1.12.2; a class here so mods can add their own through EnumHelper). */
    public static final class ArmorMaterial {
        private static final java.util.List<ArmorMaterial> ALL = new java.util.ArrayList<>();
        private final int ordinal;

        public static final ArmorMaterial LEATHER = new ArmorMaterial("leather", 5, new int[] {1, 2, 3, 1}, 15, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0.0F);
        public static final ArmorMaterial CHAIN = new ArmorMaterial("chainmail", 15, new int[] {1, 4, 5, 2}, 12, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, 0.0F);
        public static final ArmorMaterial IRON = new ArmorMaterial("iron", 15, new int[] {2, 5, 6, 2}, 9, SoundEvents.ITEM_ARMOR_EQUIP_IRON, 0.0F);
        public static final ArmorMaterial GOLD = new ArmorMaterial("gold", 7, new int[] {1, 3, 5, 2}, 25, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, 0.0F);
        public static final ArmorMaterial DIAMOND = new ArmorMaterial("diamond", 33, new int[] {3, 6, 8, 3}, 10, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, 2.0F);
        private static final int[] MAX_DAMAGE_ARRAY = {13, 15, 16, 11};

        private final String name;
        private final int maxDamageFactor;
        private final int[] damageReductionAmountArray;
        private final int enchantability;
        private final SoundEvent soundEvent;
        private final float toughness;
        private ItemStack repairMaterial = ItemStack.EMPTY;

        public ArmorMaterial(String name, int maxDamageFactor, int[] damageReductionAmountArray, int enchantability, SoundEvent soundEvent, float toughness) {
            this.ordinal = ALL.size();
            ALL.add(this);
            this.name = name;
            this.maxDamageFactor = maxDamageFactor;
            this.damageReductionAmountArray = damageReductionAmountArray;
            this.enchantability = enchantability;
            this.soundEvent = soundEvent;
            this.toughness = toughness;
        }

        public int getDurability(EntityEquipmentSlot slot) {
            return MAX_DAMAGE_ARRAY[slot.getIndex()] * maxDamageFactor;
        }

        public int getDamageReductionAmount(EntityEquipmentSlot slot) {
            return damageReductionAmountArray[slot.getIndex()];
        }

        public int getEnchantability() {
            return enchantability;
        }

        public SoundEvent getSoundEvent() {
            return soundEvent;
        }

        public Item getRepairItem() {
            return repairMaterial.getItem();
        }

        public ArmorMaterial setRepairItem(ItemStack stack) {
            this.repairMaterial = stack;
            return this;
        }

        public ItemStack getRepairItemStack() {
            return repairMaterial;
        }

        public String getName() {
            return name;
        }

        public float getToughness() {
            return toughness;
        }

        public String name() {
            return name.toUpperCase(java.util.Locale.ROOT);
        }

        public static ArmorMaterial[] values() {
            synchronized (ALL) {
                return ALL.toArray(new ArmorMaterial[0]);
            }
        }

        /** {@code Enum.compareTo}: legacy calls taking an Enum are linked here. */
        public int compareTo(Object other) {
            return Integer.compare(ordinal(), ((ArmorMaterial) other).ordinal());
        }

        public int ordinal() {
            return ordinal;
        }

        public static ArmorMaterial valueOf(String name) {
            for (ArmorMaterial v : values()) if (v.name().equals(name)) return v;
            throw new IllegalArgumentException("No enum constant " + name);
        }
    }
}
