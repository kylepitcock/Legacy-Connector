package legacyconnector.v1_12_2.net.minecraft.enchantment;

import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

public abstract class Enchantment extends IForgeRegistryEntry.Impl<Enchantment> {
    public EnumEnchantmentType type;
    private final EntityEquipmentSlot[] applicableEquipmentTypes;
    private final Rarity rarity;
    protected String name;

    protected Enchantment(Rarity rarity, EnumEnchantmentType type, EntityEquipmentSlot[] slots) {
        this.rarity = rarity;
        this.type = type;
        this.applicableEquipmentTypes = slots;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public int getMinLevel() {
        return 1;
    }

    public int getMaxLevel() {
        return 1;
    }

    public int getMinEnchantability(int level) {
        return 1 + level * 10;
    }

    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 5;
    }

    public Enchantment setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return "enchantment." + name;
    }

    public boolean isTreasureEnchantment() {
        return false;
    }

    public boolean isCurse() {
        return false;
    }

    public enum Rarity {
        COMMON(10), UNCOMMON(5), RARE(2), VERY_RARE(1);

        private final int weight;

        Rarity(int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }
    }
}
