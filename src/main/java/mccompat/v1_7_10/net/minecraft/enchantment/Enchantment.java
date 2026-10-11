package mccompat.v1_7_10.net.minecraft.enchantment;

import java.util.ArrayList;
import java.util.List;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.util.DamageSource;
import mccompat.v1_7_10.net.minecraft.util.StatCollector;

/**
 * Legacy enchantment registry: numeric ids 0-255 with the vanilla enchantments at their 1.7.10 ids. Mod enchantments
 * register here like before; 1.21's data-driven enchantments are not generated from them yet.
 */
public class Enchantment {
    public static final Enchantment[] enchantmentsList = new Enchantment[256];
    public static final Enchantment[] enchantmentsBookList;
    public static final Enchantment protection = vanilla(0, 10, EnumEnchantmentType.armor, "protect.all", 4);
    public static final Enchantment fireProtection = vanilla(1, 5, EnumEnchantmentType.armor, "protect.fire", 4);
    public static final Enchantment featherFalling = vanilla(2, 5, EnumEnchantmentType.armor_feet, "protect.fall", 4);
    public static final Enchantment blastProtection = vanilla(3, 2, EnumEnchantmentType.armor, "protect.explosion", 4);
    public static final Enchantment projectileProtection = vanilla(4, 5, EnumEnchantmentType.armor, "protect.projectile", 4);
    public static final Enchantment respiration = vanilla(5, 2, EnumEnchantmentType.armor_head, "oxygen", 3);
    public static final Enchantment aquaAffinity = vanilla(6, 2, EnumEnchantmentType.armor_head, "waterWorker", 1);
    public static final Enchantment thorns = vanilla(7, 1, EnumEnchantmentType.armor_torso, "thorns", 3);
    public static final Enchantment sharpness = vanilla(16, 10, EnumEnchantmentType.weapon, "damage.all", 5);
    public static final Enchantment smite = vanilla(17, 5, EnumEnchantmentType.weapon, "damage.undead", 5);
    public static final Enchantment baneOfArthropods = vanilla(18, 5, EnumEnchantmentType.weapon, "damage.arthropods", 5);
    public static final Enchantment knockback = vanilla(19, 5, EnumEnchantmentType.weapon, "knockback", 2);
    public static final Enchantment fireAspect = vanilla(20, 2, EnumEnchantmentType.weapon, "fire", 2);
    public static final Enchantment looting = vanilla(21, 2, EnumEnchantmentType.weapon, "lootBonus", 3);
    public static final Enchantment efficiency = vanilla(32, 10, EnumEnchantmentType.digger, "digging", 5);
    public static final Enchantment silkTouch = vanilla(33, 1, EnumEnchantmentType.digger, "untouching", 1);
    public static final Enchantment unbreaking = vanilla(34, 5, EnumEnchantmentType.breakable, "durability", 3);
    public static final Enchantment fortune = vanilla(35, 2, EnumEnchantmentType.digger, "lootBonusDigger", 3);
    public static final Enchantment power = vanilla(48, 10, EnumEnchantmentType.bow, "arrowDamage", 5);
    public static final Enchantment punch = vanilla(49, 2, EnumEnchantmentType.bow, "arrowKnockback", 2);
    public static final Enchantment flame = vanilla(50, 2, EnumEnchantmentType.bow, "arrowFire", 1);
    public static final Enchantment infinity = vanilla(51, 1, EnumEnchantmentType.bow, "arrowInfinite", 1);
    public static final Enchantment luckOfTheSea = vanilla(61, 2, EnumEnchantmentType.fishing_rod, "lootBonusFishing", 3);
    public static final Enchantment lure = vanilla(62, 2, EnumEnchantmentType.fishing_rod, "fishingSpeed", 3);

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
        this.effectId = id;
        this.weight = weight;
        this.type = type;
        if (id < 0 || id >= enchantmentsList.length) throw new IllegalArgumentException("Invalid enchantment id " + id);
        if (enchantmentsList[id] != null) throw new IllegalArgumentException("Duplicate enchantment id! " + getClass() + " and " + enchantmentsList[id].getClass() + " Enchantment ID:" + id);
        enchantmentsList[id] = this;
    }

    private static Enchantment vanilla(int id, int weight, EnumEnchantmentType type, String name, int maxLevel) {
        Enchantment e = new Enchantment(id, weight, type) {
            @Override
            public int getMaxLevel() {
                return maxLevel;
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

    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return canApply(stack);
    }

    public boolean isAllowedOnBooks() {
        return true;
    }

    public static void addToBookList(Enchantment enchantment) {
        BOOK_LIST.add(enchantment);
    }
}
