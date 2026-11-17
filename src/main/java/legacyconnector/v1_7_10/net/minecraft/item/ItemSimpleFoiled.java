package legacyconnector.v1_7_10.net.minecraft.item;

/** An item that always shows the enchantment glint (the nether star in 1.7.10). */
public class ItemSimpleFoiled extends Item {
    public ItemSimpleFoiled(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemSimpleFoiled() {
        super();
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
