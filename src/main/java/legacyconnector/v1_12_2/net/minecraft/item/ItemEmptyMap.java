package legacyconnector.v1_12_2.net.minecraft.item;

/** 1.12.2 empty map: used, it becomes a filled map of the area around the player (the modern empty map's behaviour). */
public class ItemEmptyMap extends ItemMapBase {
    protected ItemEmptyMap() {
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.MISC);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> onItemRightClick(legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn, legacyconnector.v1_12_2.net.minecraft.util.EnumHand handIn) {
        return mcc$useModern(net.minecraft.world.item.Items.MAP, worldIn, playerIn, handIn);
    }
}
