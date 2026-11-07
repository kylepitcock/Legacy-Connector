package legacyconnector.v1_6_4.facade.net.minecraft.tileentity;

/** Facade of the 1.7.10 shim's {@code TileEntity} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class TileEntity {
    private TileEntity() {}

    /**
     * {@code onDataPacket(INetworkManager, Packet132TileEntityData)}: the base implementation does nothing (a mod's override is called by
     * {@code bridge.Tiles164} when the description packet arrives), so {@code super.onDataPacket(...)} in an override is a no-op, as in 1.6.4.
     */
    public static void onDataPacket(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity self, legacyconnector.v1_6_4.net.minecraft.network.INetworkManager manager,
            legacyconnector.v1_6_4.net.minecraft.network.packet.Packet132TileEntityData packet) {
        // intentionally empty: the vanilla/Forge method body is empty
    }
}
