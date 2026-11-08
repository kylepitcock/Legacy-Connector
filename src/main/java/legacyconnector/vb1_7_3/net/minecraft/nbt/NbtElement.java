package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Beta 1.7.3 NBT ({@code NBTBase}): named tags with the binary format of the time (tag type byte, UTF name, payload). Tile entities save
 * through it; the layer stores the result as a modern compound ({@code NbtB173}).
 */
public abstract class NbtElement {
    private String key;

    public NbtElement() {}

    abstract void write(DataOutput out) throws IOException;

    abstract void read(DataInput in) throws IOException;

    public abstract byte getType();

    public String getKey() {
        return key == null ? "" : key;
    }

    public NbtElement setKey(String key) {
        this.key = key;
        return this;
    }

    public static NbtElement readTag(DataInput in) throws IOException {
        byte type = in.readByte();
        if (type == 0) return new NbtEnd();
        NbtElement e = createTypeFromId(type);
        e.key = in.readUTF();
        e.read(in);
        return e;
    }

    public static void writeTag(NbtElement e, DataOutput out) throws IOException {
        out.writeByte(e.getType());
        if (e.getType() != 0) {
            out.writeUTF(e.getKey());
            e.write(out);
        }
    }

    public static NbtElement createTypeFromId(byte type) {
        return switch (type) {
            case 0 -> new NbtEnd();
            case 1 -> new NbtByte();
            case 2 -> new NbtShort();
            case 3 -> new NbtInt();
            case 4 -> new NbtLong();
            case 5 -> new NbtFloat();
            case 6 -> new NbtDouble();
            case 7 -> new NbtByteArray();
            case 8 -> new NbtString();
            case 9 -> new NbtList();
            case 10 -> new NbtCompound();
            default -> null;
        };
    }

    public static String getTypeNameFromId(byte type) {
        return switch (type) {
            case 0 -> "TAG_End";
            case 1 -> "TAG_Byte";
            case 2 -> "TAG_Short";
            case 3 -> "TAG_Int";
            case 4 -> "TAG_Long";
            case 5 -> "TAG_Float";
            case 6 -> "TAG_Double";
            case 7 -> "TAG_Byte_Array";
            case 8 -> "TAG_String";
            case 9 -> "TAG_List";
            case 10 -> "TAG_Compound";
            default -> "UNKNOWN";
        };
    }
}
