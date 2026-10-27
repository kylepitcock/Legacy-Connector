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

    /** Whether the enchantment fits the item (its type's rule). */
    public boolean canApply(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        return type != null && type.canEnchantItem(stack.getItem());
    }

    /** Forge: whether the enchanting table may put it on the stack. */
    public boolean canApplyAtEnchantingTable(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        return stack.getItem().canApplyAtEnchantingTable(stack, this);
    }

    /** Whether both may be on one item (neither forbids the other). */
    public final boolean isCompatibleWith(Enchantment other) {
        return canApplyTogether(other) && other.canApplyTogether(this);
    }

    protected boolean canApplyTogether(Enchantment other) {
        return this != other;
    }

    public boolean isAllowedOnBooks() {
        return true;
    }

    public java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getEntityEquipment(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> out = new java.util.ArrayList<>();
        if (entity == null || !(entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l)) return out;
        for (EntityEquipmentSlot slot : applicableEquipmentTypes) {
            var s = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.view(l.getItemBySlot(mcc$slot(slot)));
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static net.minecraft.world.entity.EquipmentSlot mcc$slot(EntityEquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case OFFHAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            case FEET -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD;
        };
    }

    public int calcModifierDamage(int level, legacyconnector.v1_12_2.net.minecraft.util.DamageSource source) {
        return 0;
    }

    /** Extra melee damage against a creature kind at a level. */
    public float calcDamageByCreature(int level, legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute creatureType) {
        return 0.0F;
    }

    public void onEntityDamaged(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase user, legacyconnector.v1_12_2.net.minecraft.entity.Entity target,
            int level) {
    }

    public void onUserHurt(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase user, legacyconnector.v1_12_2.net.minecraft.entity.Entity attacker,
            int level) {
    }

    public String getTranslatedName(int level) {
        String s = legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(getName());
        if (isCurse()) s = legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting.RED + s;
        return level == 1 && getMaxLevel() == 1 ? s
                : s + " " + legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal("enchantment.level." + level);
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
