package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityEgg;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The egg: thrown like a snowball; stacks to 16. */
public class ItemEgg extends Item {
    public ItemEgg(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemEgg() {
        super();
        this.maxStackSize = 16;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabMaterials);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (mcc$vanilla != null) return super.onItemRightClick(stack, world, player);
        if (!PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) stack.mcc$setCount(stack.mcc$count() - 1);
        world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
        if (!world.isRemote) world.spawnEntityInWorld(new EntityEgg(world, player));
        return stack;
    }
}
