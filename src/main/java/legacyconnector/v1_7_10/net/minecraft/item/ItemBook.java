package legacyconnector.v1_7_10.net.minecraft.item;

/** The 1.7.10 book item: stacks to 64 but counts as a tool when single, enchantability 1. */
public class ItemBook extends Item {
    public ItemBook(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemBook() {
        super();
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabMisc);
    }

    @Override
    public boolean isItemTool(ItemStack stack) {
        return stack.mcc$count() == 1;
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }
}
