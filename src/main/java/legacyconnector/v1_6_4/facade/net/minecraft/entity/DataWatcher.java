package legacyconnector.v1_6_4.facade.net.minecraft.entity;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_6_4.forward.Forward164;
import legacyconnector.v1_6_4.forward.Packets164;
import legacyconnector.v1_7_10.net.minecraft.entity.DataWatcher.WatchableObject;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;

/**
 * Facade of the 1.7.10 shim's {@code DataWatcher} for 1.6.4 mods: the wire format of the watched values (1.6.4 sent them in entity packets itself):
 * a header byte (type in the top three bits, id in the low five) and the value; 127 ends the list. Types: 0 byte, 1 short, 2 int, 3 float, 4 string,
 * 5 item stack, 6 coordinates. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class DataWatcher {
    private DataWatcher() {}

    /** 1.6.4 {@code DataWatcher.writeObjectsInListToStream(list, out)}: the values, then the end marker. */
    public static void writeObjectsInListToStream(List<?> list, DataOutput out) throws IOException {
        if (list != null) for (Object o : list) writeObject(out, (WatchableObject) o);
        out.writeByte(127);
    }

    /** 1.6.4 {@code writeWatchableObjects(out)}: every watched value of the data watcher. */
    public static void writeWatchableObjects(legacyconnector.v1_7_10.net.minecraft.entity.DataWatcher self, DataOutput out) throws IOException {
        writeObjectsInListToStream(self.getAllWatched(), out);
    }

    /** 1.6.4 {@code DataWatcher.readWatchableObjects(in)}: the values up to the end marker. */
    public static List<WatchableObject> readWatchableObjects(DataInput in) throws IOException {
        List<WatchableObject> out = new ArrayList<>();
        for (byte header = in.readByte(); header != 127; header = in.readByte()) {
            int type = (header & 224) >> 5;
            int id = header & 31;
            Object value = switch (type) {
                case 0 -> in.readByte();
                case 1 -> in.readShort();
                case 2 -> in.readInt();
                case 3 -> in.readFloat();
                case 4 -> Packets164.readString(in, 64);
                case 5 -> Forward164.readItemStack(in);
                case 6 -> new ChunkCoordinates(in.readInt(), in.readInt(), in.readInt());
                default -> throw new IOException("Unknown watchable object type " + type);
            };
            out.add(new WatchableObject(type, id, value));
        }
        return out;
    }

    /** The instance form of {@link #readWatchableObjects(DataInput)}. */
    public static List<WatchableObject> readWatchableObjects(legacyconnector.v1_7_10.net.minecraft.entity.DataWatcher self, DataInput in) throws IOException {
        return readWatchableObjects(in);
    }

    private static void writeObject(DataOutput out, WatchableObject object) throws IOException {
        int type = object.getObjectType();
        out.writeByte((type << 5 | object.getDataValueId() & 31) & 255);
        Object value = object.getObject();
        switch (type) {
            case 0 -> out.writeByte(((Number) value).byteValue());
            case 1 -> out.writeShort(((Number) value).shortValue());
            case 2 -> out.writeInt(((Number) value).intValue());
            case 3 -> out.writeFloat(((Number) value).floatValue());
            case 4 -> Packets164.writeString((String) value, out);
            case 5 -> Forward164.writeItemStack((ItemStack) value, out);
            case 6 -> {
                ChunkCoordinates c = (ChunkCoordinates) value;
                out.writeInt(c.posX);
                out.writeInt(c.posY);
                out.writeInt(c.posZ);
            }
            default -> throw new IOException("Unknown watchable object type " + type);
        }
    }
}
