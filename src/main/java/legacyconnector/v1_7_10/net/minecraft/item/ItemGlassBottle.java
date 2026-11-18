package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The glass bottle: right-click on water fills it into a water bottle (the item the potion class gives at damage 0). */
public class ItemGlassBottle extends Item {
    public ItemGlassBottle(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemGlassBottle() {
        super();
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabBrewing);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (mcc$vanilla != null) return super.onItemRightClick(stack, world, player);
        MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, true);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return stack;
        int x = hit.blockX;
        int y = hit.blockY;
        int z = hit.blockZ;
        if (!world.canMineBlock(player, x, y, z) || !player.canPlayerEdit(x, y, z, hit.sideHit, stack)) return stack;
        if (world.getBlock(x, y, z).getMaterial() != Material.water) return stack;
        if (PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) return stack;
        stack.mcc$setCount(stack.mcc$count() - 1);
        ItemStack water = new ItemStack(Items.potionitem, 1, 0);
        if (stack.mcc$count() <= 0) return water;
        if (!player.inventory.addItemStackToInventory(water)) player.dropPlayerItemWithRandomChoice(water, false);
        return stack;
    }
}
