package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The sign item: placed on the top of a block as a standing sign (16 rotations) or on a side as a wall sign; opens the text editor. */
public class ItemSign extends Item {
    public ItemSign(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemSign() {
        super();
        this.maxStackSize = 16;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabDecorations);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (mcc$vanilla != null) return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        if (side == 0 || !world.getBlock(x, y, z).getMaterial().isSolid()) return false;
        switch (side) {
            case 1 -> ++y;
            case 2 -> --z;
            case 3 -> ++z;
            case 4 -> --x;
            case 5 -> ++x;
            default -> {
            }
        }
        if (!player.canPlayerEdit(x, y, z, side, stack) || !Blocks.standing_sign.canPlaceBlockAt(world, x, y, z)) return false;
        if (world.isRemote) return true;
        if (side == 1) {
            int rotation = MathHelper.floor_double((legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$rotationYaw(player) + 180.0F) * 16.0F / 360.0F + 0.5D) & 15;
            world.setBlock(x, y, z, Blocks.standing_sign, rotation, 3);
        } else {
            world.setBlock(x, y, z, Blocks.wall_sign, side, 3);
        }
        stack.mcc$setCount(stack.mcc$count() - 1);
        TileEntity sign = world.getTileEntity(x, y, z);
        if (sign instanceof legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntitySign) player.displayGUIEditSign(sign);
        return true;
    }
}
