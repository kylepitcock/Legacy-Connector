package legacyconnector.v1_12_2.net.minecraft.item;

/** 1.12.2 armor stand item: places an armor stand on the clicked face (the modern armor stand item's placement). */
public class ItemArmorStand extends Item {
    public ItemArmorStand() {
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.DECORATIONS);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUse(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY,
            float hitZ) {
        return mcc$useOnModern(net.minecraft.world.item.Items.ARMOR_STAND, player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }
}
