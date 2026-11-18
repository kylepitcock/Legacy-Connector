package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The bed item (1.7.10 {@code ItemBed}): placed on the top of a block it sets the foot where it was clicked and the head one block ahead (the player's facing). */
public class ItemBed extends Item {
    public ItemBed(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemBed() {
        super();
        setCreativeTab(CreativeTabs.tabDecorations);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        if (side != 1) return false;
        y++;
        var bed = legacyconnector.v1_7_10.net.minecraft.init.Blocks.bed;
        int facing = MathHelper.floor_double(Entity.mcc$get$rotationYaw(player) * 4.0F / 360.0F + 0.5D) & 3;
        int dx = 0;
        int dz = 0;
        if (facing == 0) dz = 1;
        if (facing == 1) dx = -1;
        if (facing == 2) dz = -1;
        if (facing == 3) dx = 1;
        if (!player.canPlayerEdit(x, y, z, side, stack) || !player.canPlayerEdit(x + dx, y, z + dz, side, stack)) return false;
        if (world.isAirBlock(x, y, z) && world.isAirBlock(x + dx, y, z + dz) && World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)
                && World.doesBlockHaveSolidTopSurface(world, x + dx, y - 1, z + dz)) {
            world.setBlock(x, y, z, bed, facing, 3);
            if (world.getBlock(x, y, z) == bed) world.setBlock(x + dx, y, z + dz, bed, facing + 8, 3);
            ItemStack.mcc$set$stackSize(stack, ItemStack.mcc$get$stackSize(stack) - 1);
            return true;
        }
        return false;
    }
}
