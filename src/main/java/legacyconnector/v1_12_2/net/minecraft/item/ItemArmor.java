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

    // ------------------------------------------------------------------ leather colour, attribute modifiers, repair

    private static final java.util.UUID[] ARMOR_MODIFIERS = {java.util.UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"),
            java.util.UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), java.util.UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"),
            java.util.UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")};

    /** Whether the stack is drawn with an undyed overlay (leather, or any armour with a colour). */
    public boolean hasOverlay(ItemStack stack) {
        return material == ArmorMaterial.LEATHER || getColor(stack) != 0x00FFFFFF;
    }

    public boolean hasColor(ItemStack stack) {
        if (material != ArmorMaterial.LEATHER) return false;
        var display = stack.getSubCompound("display");
        return display != null && display.hasKey("color", 3);
    }

    /** The dyed colour of leather armour (display.color, else leather brown); white for other materials. */
    public int getColor(ItemStack stack) {
        if (material != ArmorMaterial.LEATHER) return 16777215;
        var display = stack.getSubCompound("display");
        return display != null && display.hasKey("color", 3) ? display.getInteger("color") : 10511680;
    }

    public void removeColor(ItemStack stack) {
        if (material != ArmorMaterial.LEATHER) return;
        var display = stack.getSubCompound("display");
        if (display != null && display.hasKey("color")) display.removeTag("color");
    }

    public void setColor(ItemStack stack, int color) {
        if (material != ArmorMaterial.LEATHER) throw new UnsupportedOperationException("Can't dye non-leather!");
        stack.getOrCreateSubCompound("display").setInteger("color", color);
    }

    /** Armour points and toughness while worn in its own slot. */
    @Override
    public com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> getItemAttributeModifiers(
            EntityEquipmentSlot equipmentSlot) {
        com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> multimap = super.getItemAttributeModifiers(equipmentSlot);
        if (equipmentSlot == armorType) {
            multimap.put("generic.armor", new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier(ARMOR_MODIFIERS[equipmentSlot.getIndex()],
                    "Armor modifier", damageReduceAmount, 0));
            multimap.put("generic.armorToughness", new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier(ARMOR_MODIFIERS[equipmentSlot.getIndex()],
                    "Armor toughness", toughness, 0));
        }
        return multimap;
    }

    /** Repaired in an anvil by the material's repair item (ore dictionary match), as in Forge. */
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        ItemStack mat = material.getRepairItemStack();
        if (!mat.isEmpty() && legacyconnector.v1_12_2.net.minecraftforge.oredict.OreDictionary.itemMatches(mat, repair, false)) return true;
        return super.getIsRepairable(toRepair, repair);
    }

    /** The empty-slot outline sprites of the four armour slots (feet, legs, chest, head). */
    public static final String[] EMPTY_SLOT_NAMES = {"minecraft:items/empty_armor_slot_boots", "minecraft:items/empty_armor_slot_leggings",
            "minecraft:items/empty_armor_slot_chestplate", "minecraft:items/empty_armor_slot_helmet"};
}
