package legacyconnector.v1_12_2.net.minecraft.enchantment;

import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

public abstract class Enchantment extends IForgeRegistryEntry.Impl<Enchantment> {
    public static final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespaced<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, Enchantment> REGISTRY = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ENCHANTMENTS, "protection");
    public EnumEnchantmentType type;

    /** The vanilla constants register themselves when their holder class loads. */
    private static void mcc$vanilla() {
        try {
            Class.forName("legacyconnector.v1_12_2.net.minecraft.init.Enchantments", true, Enchantment.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            // only mod enchantments then
        }
    }

    public static Enchantment getEnchantmentByLocation(String location) {
        mcc$vanilla();
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ENCHANTMENTS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(location));
    }

    public static Enchantment getEnchantmentByID(int id) {
        mcc$vanilla();
        return REGISTRY.getObjectById(id);
    }

    public static int getEnchantmentID(Enchantment enchantment) {
        mcc$vanilla();
        return REGISTRY.getIDForObject(enchantment);
    }
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
