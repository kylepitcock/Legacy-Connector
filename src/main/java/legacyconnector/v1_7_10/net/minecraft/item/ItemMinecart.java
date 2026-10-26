package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A minecart item: {@link #minecartType} is the cart it places on a rail (0 rideable, 1 chest, 2 furnace, 3 TNT, 4 spawner,
 * 5 hopper, 6 command block). Mods' cart items usually place their own carts in their {@code onItemUse}.
 */
public class ItemMinecart extends Item {
    public int minecartType;

    public ItemMinecart(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        String name = info.legacyName() == null ? "" : info.legacyName().substring(info.legacyName().indexOf(':') + 1);
        this.minecartType = switch (name) {
            case "chest_minecart" -> 1;
            case "furnace_minecart" -> 2;
            case "tnt_minecart" -> 3;
            case "hopper_minecart" -> 5;
            case "command_block_minecart" -> 6;
            default -> 0;
        };
    }

    public ItemMinecart(int type) {
        super();
        this.maxStackSize = 1;
        this.minecartType = type;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabTransport);
    }

    /** Places this item's vanilla cart on the rail that was clicked. */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (mcc$vanilla != null) return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        if (!legacyconnector.v1_7_10.net.minecraft.block.BlockRailBase.isRailBlock(world.getBlock(x, y, z))) return false;
        if (!world.isRemote && minecartType >= 0 && minecartType < net.minecraft.world.entity.vehicle.AbstractMinecart.Type.values().length) {
            var level = org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps.server(world);
            if (level != null) {
                var cart = net.minecraft.world.entity.vehicle.AbstractMinecart.createMinecart(level, x + 0.5D, y + 0.0625D, z + 0.5D,
                        net.minecraft.world.entity.vehicle.AbstractMinecart.Type.values()[minecartType], stack.mcc$modern(),
                        player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p ? p : null);
                level.addFreshEntity(cart);
            }
        }
        stack.mcc$setCount(stack.mcc$count() - 1);
        return true;
    }
}
