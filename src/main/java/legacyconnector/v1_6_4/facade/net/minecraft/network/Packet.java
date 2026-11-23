package legacyconnector.v1_6_4.facade.net.minecraft.network;

/** Facade of the 1.7.10 shim's {@code Packet} for 1.6.4 mods: the two flags the 1.6.4 network threads asked of a packet. Never instantiated. */
public final class Packet {
    private Packet() {}

    /** 1.6.4 {@code isRealPacket()}: true for every packet (only stand-ins for another packet answer false; a mod's own packets are real). */
    public static boolean isRealPacket(legacyconnector.v1_7_10.net.minecraft.network.Packet self) {
        return true;
    }

    /** 1.6.4 {@code canProcessAsync()}: whether the packet may be handled off the main thread; none here is (the layer hands every packet to the game thread). */
    public static boolean canProcessAsync(legacyconnector.v1_7_10.net.minecraft.network.Packet self) {
        return false;
    }
}
