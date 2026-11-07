package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Vanilla flint and steel (mods subclass it for lighters and igniters): lights fire on the face it is used on and wears out by one. */
public class ItemFlintAndSteel extends Item {
    public ItemFlintAndSteel(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected ItemFlintAndSteel() {
        super();
        this.maxStackSize = 1;
        this.setMaxDamage(64);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        switch (side) {
            case 0 -> y--;
            case 1 -> y++;
            case 2 -> z--;
            case 3 -> z++;
            case 4 -> x--;
            case 5 -> x++;
            default -> {
            }
        }
        if (!player.canPlayerEdit(x, y, z, side, stack)) return false;
        if (world.isAirBlock(x, y, z)) {
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "fire.ignite", 1.0F, itemRand.nextFloat() * 0.4F + 0.8F);
            world.setBlock(x, y, z, legacyconnector.v1_7_10.net.minecraft.init.Blocks.fire);
        }
        stack.damageItem(1, player);
        return true;
    }
}
