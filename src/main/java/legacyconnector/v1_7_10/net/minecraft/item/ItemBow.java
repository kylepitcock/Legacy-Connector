package legacyconnector.v1_7_10.net.minecraft.item;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.item.ItemBow}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Hand-written now: the bow's enchantability (1) is ported.
 */
public class ItemBow extends legacyconnector.v1_7_10.net.minecraft.item.Item {
    public ItemBow(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected ItemBow() {
        super();
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }
}
