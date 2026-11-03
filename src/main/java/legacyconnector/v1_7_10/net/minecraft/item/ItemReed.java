package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** An item that places a block when used (sugar cane, cauldrons, brewing stands, repeaters). */
public class ItemReed extends Item {
    private final Block field_150935_a;

    public ItemReed(Block block) {
        this.field_150935_a = block;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        Block target = world.getBlock(x, y, z);
        if (target == Blocks.snow_layer && (world.getBlockMetadata(x, y, z) & 7) < 1) {
            side = 1;
        } else if (target != Blocks.vine && target != Blocks.tallgrass && target != Blocks.deadbush && !target.isReplaceable(world, x, y, z)) {
            switch (side) {
                case 0 -> y--;
                case 1 -> y++;
                case 2 -> z--;
                case 3 -> z++;
                case 4 -> x--;
                case 5 -> x++;
                default -> { }
            }
        }
        if (!player.canPlayerEdit(x, y, z, side, stack)) return false;
        if (stack.mcc$count() == 0) return false;
        if (!world.canPlaceEntityOnSide(field_150935_a, x, y, z, false, side, null, stack)) return false;
        int meta = field_150935_a.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, 0);
        if (world.setBlock(x, y, z, field_150935_a, meta, 3)) {
            if (world.getBlock(x, y, z) == field_150935_a) {
                field_150935_a.onBlockPlacedBy(world, x, y, z, player, stack);
                field_150935_a.onPostBlockPlaced(world, x, y, z, meta);
            }
            world.mcc$playPlaceSound(field_150935_a, x, y, z);
            stack.mcc$setCount(stack.mcc$count() - 1);
        }
        return true;
    }
}
