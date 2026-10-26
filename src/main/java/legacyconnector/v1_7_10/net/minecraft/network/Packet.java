package legacyconnector.v1_7_10.net.minecraft.network;

import io.netty.buffer.ByteBuf;
import java.io.IOException;

/**
 * Base of the vanilla packets legacy mods construct directly. Each keeps the legacy fields and wire format
 * ({@link #readPacketData}/{@link #writePacketData}: mods serialize vanilla packets into their own channels, Twilight
 * Forest its maps) and is delivered by its modern equivalent: {@link #mcc$send} for packets to a player,
 * {@link #mcc$sendToServer} for packets the client sends.
 */
public abstract class Packet {
    public void readPacketData(PacketBuffer data) throws IOException {
    }

    public void writePacketData(PacketBuffer data) throws IOException {
    }

    public void processPacket(INetHandler handler) {
    }

    public boolean hasPriority() {
        return false;
    }

    public String serialize() {
        return "";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /** Writes a byte array prefixed by its length as a short. */
    public static void writeBlob(ByteBuf buffer, byte[] blob) {
        buffer.writeShort(blob.length);
        buffer.writeBytes(blob);
    }

    /** Reads a byte array prefixed by its length as a short. */
    public static byte[] readBlob(ByteBuf buffer) throws IOException {
        short length = buffer.readShort();
        if (length < 0) throw new IOException("Key was smaller than nothing!  Weird key!");
        byte[] blob = new byte[length];
        buffer.readBytes(blob);
        return blob;
    }

    /** Delivers this packet to a player: packets with a modern equivalent send it; the others do nothing. */
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
    }

    /** Sends this packet from the client to the server: packets with a modern equivalent send it; the others do nothing. */
    public void mcc$sendToServer() {
    }
}
