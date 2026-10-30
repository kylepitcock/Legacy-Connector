package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityMinecart;

/** 1.12.2 minecart item of a cart kind: used on a rail, places that cart (the modern minecart item of the kind). */
public class ItemMinecart extends Item {
    private final EntityMinecart.Type minecartType;

    public ItemMinecart(EntityMinecart.Type typeIn) {
        this.maxStackSize = 1;
        this.minecartType = typeIn;
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.TRANSPORTATION);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUse(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY,
            float hitZ) {
        return mcc$useOnModern(mcc$modernItem(), player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    private net.minecraft.world.item.Item mcc$modernItem() {
        if (minecartType == null) return net.minecraft.world.item.Items.MINECART;
        return switch (minecartType) {
            case CHEST -> net.minecraft.world.item.Items.CHEST_MINECART;
            case FURNACE -> net.minecraft.world.item.Items.FURNACE_MINECART;
            case TNT -> net.minecraft.world.item.Items.TNT_MINECART;
            case HOPPER -> net.minecraft.world.item.Items.HOPPER_MINECART;
            case COMMAND_BLOCK -> net.minecraft.world.item.Items.COMMAND_BLOCK_MINECART;
            default -> net.minecraft.world.item.Items.MINECART;
        };
    }
}
