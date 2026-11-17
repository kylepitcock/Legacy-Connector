package legacyconnector.v1_6_4.facade.cpw.mods.fml.client;

/** Facade of the 1.7.10 shim's {@code FMLClientHandler} for 1.4.7 and 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class FMLClientHandler {
    private FMLClientHandler() {}

    /** {@code sendPacket(packet)}: queues a packet on the client's connection to the server (a no-op outside a world). */
    public static void sendPacket(legacyconnector.v1_7_10.cpw.mods.fml.client.FMLClientHandler self, legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
        if (packet == null) return;
        legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient.INSTANCE.addToSendQueue(packet);
    }
}
