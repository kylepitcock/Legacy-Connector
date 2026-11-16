package legacyconnector.v1_6_4.net.minecraft.src.forge;

/** Forge 3.x's armour absorption record: the shim's own class (fields {@code Priority}, {@code AbsorbMax}, {@code AbsorbRatio}, {@code Slot}) under the 3.x name. */
public class ArmorProperties extends legacyconnector.v1_7_10.net.minecraftforge.common.ISpecialArmor.ArmorProperties {
    public ArmorProperties(int priority, double ratio, int max) {
        super(priority, ratio, max);
    }

    private static final java.util.Map<Object, Integer> CARRY = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * Forge 3.3's {@code ApplyArmor} (checked in the real source): the damage left after the armour as an {@code int}, with the part of a heart it cannot
     * express kept in the entity's {@code carryoverDamage} and added to its next hit. The shared 1.7.10 method returns the float left (nothing is lost
     * there), so this is that value in 1/25 units plus the carry: {@code damage += carry; carry = (int) damage % 25; return (int) (damage / 25)}.
     * The loader points {@code ApplyArmor(...)I} calls of a 1.2.5 jar here.
     */
    public static int mcc125$applyArmor(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, legacyconnector.v1_7_10.net.minecraft.item.ItemStack[] inventory,
            legacyconnector.v1_7_10.net.minecraft.util.DamageSource source, double damage) {
        double left = ApplyArmor(entity, inventory, source, damage) * 25.0D;
        return withCarry(entity, left);
    }

    /** The carry rule alone: {@code left} is the damage in 1/25 heart units. */
    static int withCarry(Object entity, double left) {
        Object key = entity == null ? NULL_KEY : entity;
        Integer carry = CARRY.get(key);
        left += carry == null ? 0 : carry;
        CARRY.put(key, (int) left % 25);
        return (int) (left / 25.0D);
    }

    private static final Object NULL_KEY = new Object();

    @Override
    public ArmorProperties copy() {
        ArmorProperties c = new ArmorProperties(Priority, AbsorbRatio, AbsorbMax);
        c.Armor = Armor;
        c.Toughness = Toughness;
        c.Slot = Slot;
        return c;
    }

    @Override
    public String toString() {
        return String.format("%d, %d, %f, %d", Priority, AbsorbMax, AbsorbRatio, Slot);
    }
}
