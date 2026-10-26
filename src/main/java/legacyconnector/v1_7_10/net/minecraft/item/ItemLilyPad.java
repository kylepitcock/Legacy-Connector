package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The lily pad item: placed on the still water the player looks at, not against a block face. */
public class ItemLilyPad extends ItemColored {
    public ItemLilyPad(Block block) {
        super(block, false);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, true);
        if (hit == null || hit.typeOfHit != legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) return stack;
        int x = hit.blockX, y = hit.blockY, z = hit.blockZ;
        if (!world.canMineBlock(player, x, y, z) || !player.canPlayerEdit(x, y, z, hit.sideHit, stack)) return stack;
        if (world.getBlock(x, y, z).getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.water
                && world.getBlockMetadata(x, y, z) == 0 && world.isAirBlock(x, y + 1, z)) {
            world.setBlock(x, y + 1, z, legacyconnector.v1_7_10.net.minecraft.init.Blocks.waterlily);
            if (!legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) {
                stack.mcc$setCount(stack.mcc$count() - 1);
            }
        }
        return stack;
    }

    @Override
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return legacyconnector.v1_7_10.net.minecraft.init.Blocks.waterlily.getRenderColor(stack.getItemDamage());
    }
}
