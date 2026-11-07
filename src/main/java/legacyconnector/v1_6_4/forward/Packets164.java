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

    /** {@code Packet.getNewPacket(logAgent, id)} / {@code getNewPacket(id)}: a new instance of the class registered for the id, null for none. */
    public static Packet getNewPacket(int id) {
        Class<?> type = Packet.mcc$classById.get(id);
        if (type == null) return null;
        try {
            return (Packet) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }
}
