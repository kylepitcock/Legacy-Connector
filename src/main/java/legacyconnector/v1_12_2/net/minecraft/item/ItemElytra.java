package legacyconnector.v1_12_2.net.minecraft.item;

/** 1.12.2 elytra (432 uses, worn on the chest): usable for gliding until one point of durability is left. */
public class ItemElytra extends Item {
    public ItemElytra() {
        this.maxStackSize = 1;
        setMaxDamage(432);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.TRANSPORTATION);
    }

    /** Whether the wings still carry (they break at the last point). */
    public static boolean isUsable(ItemStack stack) {
        return stack.getItemDamage() < stack.getMaxDamage() - 1;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.LEATHER;
    }
}
