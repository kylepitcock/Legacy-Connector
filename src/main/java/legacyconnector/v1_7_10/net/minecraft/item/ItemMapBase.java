package legacyconnector.v1_7_10.net.minecraft.item;

/** Base of map items: {@link #isMap} is true, and a map can make the packet that sends its contents to a player. */
public class ItemMapBase extends legacyconnector.v1_7_10.net.minecraft.item.Item {
    public ItemMapBase(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected ItemMapBase() {
        super();
    }

    @Override
    public boolean isMap() {
        return true;
    }

    /** {@code func_150911_c}: the packet with the map's contents for a player, or null (the base item sends nothing). */
    public legacyconnector.v1_7_10.net.minecraft.network.Packet createMapDataPacket(ItemStack stack, legacyconnector.v1_7_10.net.minecraft.world.World world,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        return null;
    }
}
