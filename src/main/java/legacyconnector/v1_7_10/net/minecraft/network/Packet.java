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
    /** 1.5.2 / 1.6.4 flag of packets that carry chunk data (the legacy network threads sorted by it); no effect here. */
    public boolean isChunkDataPacket;

    /**
     * The 1.5.2 / 1.6.4 wire format ({@code DataInput}/{@code DataOutput}) of a mod's own {@code Packet} subclass. The 1.7.10 buffer methods
     * below delegate to it, so a legacy subclass overriding these is read and written whichever way the layer carries it (see {@code Network164}).
     */
    public void readPacketData(java.io.DataInput in) throws IOException {
    }

    public void writePacketData(java.io.DataOutput out) throws IOException {
    }

    /** 1.5.2 / 1.6.4 size estimate of the packet in bytes. */
    public int getPacketSize() {
        return 0;
    }

    public void readPacketData(PacketBuffer data) throws IOException {
        readPacketData((java.io.DataInput) new java.io.DataInputStream(new io.netty.buffer.ByteBufInputStream(data)));
    }

    public void writePacketData(PacketBuffer data) throws IOException {
        writePacketData((java.io.DataOutput) new java.io.DataOutputStream(new io.netty.buffer.ByteBufOutputStream(data)));
    }

    /** 1.6.4's running total of the bytes written through {@link #writePacket} (the network threads' statistics). */
    public static long sentSize;

    /**
     * 1.6.4's {@code Packet.writePacket(packet, out)}: the packet id byte, then the packet data. The id is the number in the class name of the
     * numbered vanilla packets ({@code Packet250CustomPayload} is 250); other packets are written as custom payloads (250).
     */
    public static void writePacket(Packet packet, java.io.DataOutput out) throws IOException {
        if (packet == null) return;
        out.writeByte(mcc$idOf(packet.getClass()));
        packet.writePacketData(out);
        sentSize += 1 + packet.getPacketSize();
    }

    /** The legacy packet id for a class: the digits after {@code Packet} in the name of itself or of a superclass, else 250. */
    public static int mcc$idOf(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            String n = c.getSimpleName();
            if (n.startsWith("Packet")) {
                int end = 6;
                while (end < n.length() && Character.isDigit(n.charAt(end))) end++;
                if (end > 6) return Integer.parseInt(n.substring(6, end));
            }
        }
        return 250;
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
