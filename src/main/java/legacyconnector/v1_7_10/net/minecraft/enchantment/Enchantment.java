package legacyconnector.v1_7_10.net.minecraft.enchantment;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;

/**
 * Legacy enchantment registry: numeric ids 0-255 with the vanilla enchantments at their 1.7.10 ids. Mod enchantments
 * register here like before; 1.21's data-driven enchantments are not generated from them yet.
 */
public class Enchantment {
    public static final Enchantment[] enchantmentsList = new Enchantment[256];
    public static final Enchantment[] enchantmentsBookList;
    public static final Enchantment protection = new EnchantmentProtection(0, 10, 0);
    public static final Enchantment fireProtection = new EnchantmentProtection(1, 5, 1);
    public static final Enchantment featherFalling = new EnchantmentProtection(2, 5, 2);
    public static final Enchantment blastProtection = new EnchantmentProtection(3, 2, 3);
    public static final Enchantment projectileProtection = new EnchantmentProtection(4, 5, 4);
    // 1.7.10's enchantability windows: min = base + (level - 1) * perLevel, max = min + span
    public static final Enchantment respiration = vanilla(5, 2, EnumEnchantmentType.armor_head, "oxygen", 3, 10, 10, 30);
    public static final Enchantment aquaAffinity = vanilla(6, 2, EnumEnchantmentType.armor_head, "waterWorker", 1, 1, 0, 40);
    public static final Enchantment thorns = vanilla(7, 1, EnumEnchantmentType.armor_torso, "thorns", 3, 10, 20, 50);
    public static final Enchantment sharpness = vanilla(16, 10, EnumEnchantmentType.weapon, "damage.all", 5, 1, 11, 20);
    public static final Enchantment smite = vanilla(17, 5, EnumEnchantmentType.weapon, "damage.undead", 5, 5, 8, 20);
    public static final Enchantment baneOfArthropods = vanilla(18, 5, EnumEnchantmentType.weapon, "damage.arthropods", 5, 5, 8, 20);
    public static final Enchantment knockback = vanilla(19, 5, EnumEnchantmentType.weapon, "knockback", 2, 5, 20, 50);
    public static final Enchantment fireAspect = vanilla(20, 2, EnumEnchantmentType.weapon, "fire", 2, 10, 20, 50);
    public static final Enchantment looting = vanilla(21, 2, EnumEnchantmentType.weapon, "lootBonus", 3, 15, 9, 50);
    public static final Enchantment efficiency = vanilla(32, 10, EnumEnchantmentType.digger, "digging", 5, 1, 10, 50);
    public static final Enchantment silkTouch = vanilla(33, 1, EnumEnchantmentType.digger, "untouching", 1, 15, 0, 50);
    public static final Enchantment unbreaking = new EnchantmentDurability(34, 5);
    public static final Enchantment fortune = vanilla(35, 2, EnumEnchantmentType.digger, "lootBonusDigger", 3, 15, 9, 50);
    public static final Enchantment power = vanilla(48, 10, EnumEnchantmentType.bow, "arrowDamage", 5, 1, 10, 15);
    public static final Enchantment punch = vanilla(49, 2, EnumEnchantmentType.bow, "arrowKnockback", 2, 12, 20, 25);
    public static final Enchantment flame = vanilla(50, 2, EnumEnchantmentType.bow, "arrowFire", 1, 20, 0, 30);
    public static final Enchantment infinity = vanilla(51, 1, EnumEnchantmentType.bow, "arrowInfinite", 1, 20, 0, 30);
    public static final Enchantment luckOfTheSea = vanilla(61, 2, EnumEnchantmentType.fishing_rod, "lootBonusFishing", 3, 15, 9, 50);
    public static final Enchantment lure = vanilla(62, 2, EnumEnchantmentType.fishing_rod, "fishingSpeed", 3, 15, 9, 50);

    private static final List<Enchantment> BOOK_LIST = new ArrayList<>();

    static {
        List<Enchantment> list = new ArrayList<>();
        for (Enchantment e : enchantmentsList) if (e != null) list.add(e);
        enchantmentsBookList = list.toArray(new Enchantment[0]);
    }

    public final int effectId;
    private final int weight;
    public EnumEnchantmentType type;
    protected String name;

    protected Enchantment(int id, int weight, EnumEnchantmentType type) {
        if (id < 0 || id >= enchantmentsList.length) throw new IllegalArgumentException("Invalid enchantment id " + id);
        if (enchantmentsList[id] != null) {
            // 1.7.10 crashed here; two mods configured with the same id still work when one of them moves.
            int free = -1;
            for (int i = 1; i < enchantmentsList.length && free < 0; i++) {
                int candidate = (id + i) % enchantmentsList.length;
                if (enchantmentsList[candidate] == null) free = candidate;
            }
            if (free < 0) throw new IllegalArgumentException("No free enchantment id for " + getClass());
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Enchantments").warn(
                    "Enchantment id {} of {} is already used by {}; using {} instead (change one of the ids in the mods' configs)",
                    id, getClass().getName(), enchantmentsList[id].getClass().getName(), free);
            id = free;
        }
        this.effectId = id;
        this.weight = weight;
        this.type = type;
        enchantmentsList[id] = this;
    }

    private static Enchantment vanilla(int id, int weight, EnumEnchantmentType type, String name, int maxLevel, int base, int perLevel, int span) {
        Enchantment e = new Enchantment(id, weight, type) {
            @Override
            public int getMaxLevel() {
                return maxLevel;
            }

            @Override
            public int getMinEnchantability(int level) {
                return base + (level - 1) * perLevel;
            }

            @Override
            public int getMaxEnchantability(int level) {
                return getMinEnchantability(level) + span;
            }

            @Override
            public boolean canApplyTogether(Enchantment other) {
                if (other == null || other == this) return other != this;
                int a = effectId, b = other.effectId;
                if (a >= 16 && a <= 18 && b >= 16 && b <= 18) return false; // sharpness, smite, bane
                if ((a == 33 && b == 35) || (a == 35 && b == 33)) return false; // silk touch, fortune
                return true;
            }

            @Override
            public boolean canApply(ItemStack stack) {
                if (stack != null && stack.getItem() != null) {
                    var item = stack.getItem();
                    if (effectId == 7 && item instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemArmor) return true;
                    if (effectId >= 16 && effectId <= 18 && item instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemAxe) return true;
                    if (effectId == 32 && item == legacyconnector.v1_7_10.net.minecraft.init.Items.shears) return true;
                }
                return super.canApply(stack);
            }
        };
        e.setName(name);
        return e;
    }

    public int getWeight() {
        return weight;
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

    public int calcModifierDamage(int level, DamageSource source) {
        return 0;
    }

    /** Extra melee damage against a creature of this attribute (func_152376_a). */
    public float func_152376_a(int level, legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureAttribute attribute) {
        if (effectId == 16) return level * 1.25F;
        if (effectId == 17 && attribute == legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureAttribute.UNDEAD) return level * 2.5F;
        if (effectId == 18 && attribute == legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureAttribute.ARTHROPOD) return level * 2.5F;
        return 0.0F;
    }

    public boolean canApplyTogether(Enchantment other) {
        return this != other;
    }

    public Enchantment setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return "enchantment." + name;
    }

    public String getTranslatedName(int level) {
        return StatCollector.translateToLocal(getName()) + " " + StatCollector.translateToLocal("enchantment.level." + level);
    }

    public boolean canApply(ItemStack stack) {
        return type != null && stack != null && stack.getItem() != null && type.canEnchantItem(stack.getItem());
    }

    /** Called after the user hits an entity. */
    public void func_151368_a(EntityLivingBase user, Entity target, int level) {
    }

    /** Called when the user is hurt. */
    public void func_151367_b(EntityLivingBase user, Entity attacker, int level) {
    }

    /** Forge 1.7.10: the table only offers the enchantment's own type (canApply may allow more, for the anvil). */
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return type != null && stack != null && stack.getItem() != null && type.canEnchantItem(stack.getItem());
    }

    public boolean isAllowedOnBooks() {
        return true;
    }

    public static void addToBookList(Enchantment enchantment) {
        BOOK_LIST.add(enchantment);
    }
}
