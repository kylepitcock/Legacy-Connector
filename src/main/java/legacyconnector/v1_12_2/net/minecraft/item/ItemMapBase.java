package legacyconnector.v1_12_2.net.minecraft.item;

/** 1.12.2 base of map items (filled and empty): a map item is "complex" (it syncs its own data to holders). */
public class ItemMapBase extends Item {
    @Override
    public boolean isMap() {
        return true;
    }

    /** The packet with the map's updates for a player; the modern map item sends its own, so none here. */
    public legacyconnector.v1_12_2.net.minecraft.network.Packet<?> createMapDataPacket(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return null;
    }
}
