package legacyconnector.v1_12_2.net.minecraft.network;

import java.io.IOException;

/**
 * A packet. The vanilla packets mods build are sent as the modern packet they describe ({@link #mcc$modern}); a packet a mod
 * defines itself has no modern counterpart and is not sent.
 */
public interface Packet<T extends INetHandler> {
    void readPacketData(PacketBuffer buf) throws IOException;

    void writePacketData(PacketBuffer buf) throws IOException;

    void processPacket(T handler);

    /** The modern packet this one describes, or null when there is none. */
    default net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return null;
    }
}
