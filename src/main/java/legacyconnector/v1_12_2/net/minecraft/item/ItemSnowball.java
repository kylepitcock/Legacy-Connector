package legacyconnector.v1_12_2.net.minecraft.item;

/** 1.12.2 snowball: thrown on use (the modern snowball's throw). */
public class ItemSnowball extends Item {
    public ItemSnowball() {
        this.maxStackSize = 16;
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.MISC);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> onItemRightClick(legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn, legacyconnector.v1_12_2.net.minecraft.util.EnumHand handIn) {
        return mcc$useModern(net.minecraft.world.item.Items.SNOWBALL, worldIn, playerIn, handIn);
    }
}
