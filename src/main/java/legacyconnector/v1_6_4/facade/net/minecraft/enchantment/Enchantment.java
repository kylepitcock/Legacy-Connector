package legacyconnector.v1_6_4.facade.net.minecraft.enchantment;

/** Facade of the 1.7.10 shim's {@code Enchantment} for 1.6.4 mods: the extra damage of an enchantment against the living target. Never instantiated. */
public final class Enchantment {
    private Enchantment() {}

    /** 1.6.4 {@code calcModifierLiving(level, target)}: the extra damage against the target's creature type (1.7.10 asks the attribute, not the entity). */
    public static float calcModifierLiving(legacyconnector.v1_7_10.net.minecraft.enchantment.Enchantment self, int level, legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase target) {
        return target == null ? 0.0F : self.func_152376_a(level, target.getCreatureAttribute());
    }
}
