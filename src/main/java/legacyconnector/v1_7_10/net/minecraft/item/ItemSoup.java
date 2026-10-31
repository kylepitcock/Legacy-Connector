package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A bowl food: stacks to one and leaves the bowl when eaten. */
public class ItemSoup extends ItemFood {
    public ItemSoup(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemSoup(int heal) {
        super(heal, false);
        setMaxStackSize(1);
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        super.onEaten(stack, world, player);
        return new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.bowl);
    }
}
