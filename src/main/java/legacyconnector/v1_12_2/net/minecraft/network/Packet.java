package legacyconnector.v1_12_2.net.minecraft.network;

/**
 * A 1.12.2 packet. The vanilla packets mods build and send ({@code player.connection.sendPacket(...)}) are not written to the
 * wire in their 1.12 format: each one the shim implements turns itself into the modern packet with the same effect
 * ({@link #mcc$modern}); the others are dropped (null).
 */
public interface Packet<T extends INetHandler> {
    default void readPacketData(PacketBuffer buf) throws java.io.IOException {
    }

    default void writePacketData(PacketBuffer buf) throws java.io.IOException {
    }

    default void processPacket(T handler) {
    }

    /** The modern packet with this packet's effect for the given receiving player (null: nothing to send). */
    default net.minecraft.network.protocol.Packet<?> mcc$modern(net.minecraft.server.level.ServerPlayer receiver) {
        return null;
    }
}
