package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Glass bottle (1.7.10 {@code ItemGlassBottle}): right-clicking water fills it into a water bottle. */
public class ItemGlassBottle extends Item {
    private IIcon field_150920_a;

    public ItemGlassBottle(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemGlassBottle() {
        super();
        setCreativeTab(CreativeTabs.tabBrewing);
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        return field_150920_a != null ? field_150920_a : super.getIconFromDamage(damage);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, true);
        if (hit == null) return stack;
        if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            int x = hit.blockX;
            int y = hit.blockY;
            int z = hit.blockZ;
            if (!world.canMineBlock(player, x, y, z)) return stack;
            if (!player.canPlayerEdit(x, y, z, hit.sideHit, stack)) return stack;
            if (world.getBlock(x, y, z).getMaterial() == Material.water) {
                ItemStack.mcc$set$stackSize(stack, ItemStack.mcc$get$stackSize(stack) - 1);
                if (ItemStack.mcc$get$stackSize(stack) <= 0) return new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.potionitem);
                if (!player.inventory.addItemStackToInventory(new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.potionitem))) {
                    player.dropPlayerItemWithRandomChoice(new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.potionitem, 1, 0), false);
                }
            }
        }
        return stack;
    }

    @Override
    public void registerIcons(IIconRegister register) {
        field_150920_a = register.registerIcon(getIconString() + "_empty");
    }
}
