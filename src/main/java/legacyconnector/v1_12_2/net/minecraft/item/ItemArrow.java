package legacyconnector.v1_12_2.net.minecraft.item;

/** An arrow item: bows shoot it. (createArrow names the arrow entity, which the entity area provides.) */
public class ItemArrow extends Item {
    public ItemArrow() {
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.COMBAT);
    }

    /** Forge: a bow with infinity does not use up plain arrows (mod arrow subclasses are used up unless they say otherwise). */
    public boolean isInfinite(ItemStack stack, ItemStack bow, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        int level = legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment.getEnchantmentByLocation("infinity"), bow);
        return level > 0 && getClass() == ItemArrow.class;
    }
}
