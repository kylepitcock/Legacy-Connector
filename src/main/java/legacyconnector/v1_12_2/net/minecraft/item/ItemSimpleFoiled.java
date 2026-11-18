package legacyconnector.v1_12_2.net.minecraft.item;

/** An item that always shows the enchantment glint (the 1.12 base of the experience bottle and the like). */
public class ItemSimpleFoiled extends Item {
    public ItemSimpleFoiled() {
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
