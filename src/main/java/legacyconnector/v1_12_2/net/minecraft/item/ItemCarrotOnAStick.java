package legacyconnector.v1_12_2.net.minecraft.item;

/** 1.12.2 carrot on a stick: boosts the saddled pig the player rides, wearing the stick (the modern item's boost). */
public class ItemCarrotOnAStick extends Item {
    public ItemCarrotOnAStick() {
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.TRANSPORTATION);
        setMaxStackSize(1);
        setMaxDamage(25);
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    @Override
    public boolean shouldRotateAroundWhenRendering() {
        return true;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> onItemRightClick(legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn, legacyconnector.v1_12_2.net.minecraft.util.EnumHand handIn) {
        return mcc$useModern(net.minecraft.world.item.Items.CARROT_ON_A_STICK, worldIn, playerIn, handIn);
    }
}
