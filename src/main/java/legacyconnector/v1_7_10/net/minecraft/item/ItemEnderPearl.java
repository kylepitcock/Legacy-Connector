package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The ender pearl: thrown (as a modern thrown pearl) unless the player rides something; stacks to 16. */
public class ItemEnderPearl extends Item {
    public ItemEnderPearl(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemEnderPearl() {
        super();
        this.maxStackSize = 16;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabMisc);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (mcc$vanilla != null) return super.onItemRightClick(stack, world, player);
        if (legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) return stack;
        var modern = player.mcc$player();
        if (modern == null || modern.isPassenger()) return stack;
        stack.mcc$setCount(stack.mcc$count() - 1);
        world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
        if (!world.isRemote) {
            var pearl = new net.minecraft.world.entity.projectile.ThrownEnderpearl(modern.level(), modern);
            pearl.setItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL));
            pearl.shootFromRotation(modern, modern.getXRot(), modern.getYRot(), 0.0F, 1.5F, 1.0F);
            modern.level().addFreshEntity(pearl);
        }
        return stack;
    }
}
