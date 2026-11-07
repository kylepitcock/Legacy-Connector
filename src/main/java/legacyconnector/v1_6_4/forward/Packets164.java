package legacyconnector.v1_6_4.forward;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/**
 * The static helpers of the legacy {@code net.minecraft.network.Packet} class (1.4.7 to 1.6.4; 1.4.7 and 1.5.2 pass {@code DataInputStream} /
 * {@code DataOutputStream}, which are {@code DataInput}/{@code DataOutput}): strings, NBT and byte arrays in the legacy wire format, and the
 * id to class registry mods add their own packets to. Reached through the {@code R ... static} rules of the forward tables.
 */
public final class Packets164 {
    private Packets164() {}

    /** {@code Packet.readString(in, max)}: a short length, then that many UTF-16 chars; longer than {@code max} is an error. */
    public static String readString(DataInput in, int max) throws IOException {
        short length = in.readShort();
        if (length > max) throw new IOException("Received string length longer than maximum allowed (" + length + " > " + max + ")");
        if (length < 0) throw new IOException("Received string length is less than zero! Weird string!");
        StringBuilder out = new StringBuilder(length);
        for (int i = 0; i < length; i++) out.append(in.readChar());
        return out.toString();
    }

    /** {@code Packet.writeString(text, out)}: the inverse of {@link #readString}; the text may not be longer than 32767 chars. */
    public static void writeString(String text, DataOutput out) throws IOException {
        if (text.length() > 32767) throw new IOException("String too big (was " + text.length() + " bytes encoded, max 32767)");
        out.writeShort(text.length());
        out.writeChars(text);
    }

    /** {@code Packet.readNBTTagCompound(in)}: a short length (-1 for none), then the gzipped tag. */
    public static NBTTagCompound readNBTTagCompound(DataInput in) throws IOException {
        short length = in.readShort();
        if (length < 0) return null;
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new NBTTagCompound(net.minecraft.nbt.NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes),
                net.minecraft.nbt.NbtAccounter.unlimitedHeap()));
    }

    /** {@code Packet.writeNBTTagCompound(tag, out)}. */
    public static void writeNBTTagCompound(NBTTagCompound tag, DataOutput out) throws IOException {
        if (tag == null) {
            out.writeShort(-1);
            return;
        }
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        net.minecraft.nbt.NbtIo.writeCompressed(tag.mcc$modern(), bytes);
        out.writeShort((short) bytes.size());
        out.write(bytes.toByteArray());
    }

    /** {@code Packet.readBytesFromStream(in)}: a short length, then the bytes. */
    public static byte[] readBytesFromStream(DataInput in) throws IOException {
        short length = in.readShort();
        if (length < 0) throw new IOException("Key was smaller than nothing!  Weird key!");
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return bytes;
    }

    /** {@code Packet.writeByteArray(out, bytes)}. */
    public static void writeByteArray(DataOutput out, byte[] bytes) throws IOException {
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    /** {@code Packet.addIdClassMapping(id, clientToServer, serverToClient, type)}: lets {@link #getNewPacket} build the class and {@code writePacket} write its id. */
    public static void addIdClassMapping(int id, boolean clientPacket, boolean serverPacket, Class<?> type) {
        if (Packet.mcc$classById.containsKey(id)) throw new IllegalArgumentException("Duplicate packet id:" + id);
        Packet.mcc$classById.put(id, type);
        Packet.mcc$idByClass.put(type, id);
    }

    private static final Class<?>[] BUILTIN = {
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet1Login.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet3Chat.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet7UseEntity.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet9Respawn.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet10Flying.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet11PlayerPosition.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet12PlayerLook.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet13PlayerLookMove.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet14BlockDig.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet15Place.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet28EntityVelocity.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet30Entity.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet31RelEntityMove.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet32EntityLook.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet33RelEntityMoveLook.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet41EntityEffect.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet43Experience.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet51MapChunk.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet53BlockChange.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet62LevelSound.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet70GameEvent.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet100OpenWindow.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet102WindowClick.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet103SetSlot.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet131MapData.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet132TileEntityData.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet250CustomPayload.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet254ServerPing.class,
        legacyconnector.v1_6_4.net.minecraft.network.packet.Packet255KickDisconnect.class,
    };

    /** The packet class with an id: the ones the layer implements, then the ones mods registered. */
    public static Class<?> classOf(int id) {
        Class<?> registered = Packet.mcc$classById.get(id);
        if (registered != null) return registered;
        for (Class<?> c : BUILTIN) if (Packet.mcc$idOf(c) == id) return c;
        return null;
    }

    /** {@code Packet.getNewPacket(logAgent, id)}: the log agent is ignored. */
    public static Packet getNewPacket(Object logAgent, int id) {
        return getNewPacket(id);
    }

    /** {@code Packet.readPacket(logAgent, in, serverSide, socket)}: the id byte, then the packet's data; null at the end of the stream, an error for an unknown id. */
    public static Packet readPacket(Object logAgent, DataInput in, boolean serverSide, java.net.Socket socket) throws IOException {
        return readPacket(in, serverSide, socket);
    }

    /** 1.4.7 {@code Packet.readPacket(in, serverSide, socket)}. */
    public static Packet readPacket(DataInput in, boolean serverSide, java.net.Socket socket) throws IOException {
        int id;
        try {
            id = in.readUnsignedByte();
        } catch (java.io.EOFException e) {
            return null;
        }
        Packet packet = getNewPacket(id);
        if (packet == null) throw new IOException("Bad packet id " + id);
        packet.readPacketData(in);
        return packet;
    }

    /** {@code Packet.getNewPacket(id)}: a new instance of the class registered for the id, null for none. */
    public static Packet getNewPacket(int id) {
        Class<?> type = classOf(id);
        if (type == null) return null;
        try {
            return (Packet) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }
}
