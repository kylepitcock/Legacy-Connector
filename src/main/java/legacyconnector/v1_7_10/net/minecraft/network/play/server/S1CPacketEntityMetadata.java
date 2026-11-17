package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.DataWatcher;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * The changed (or, with {@code sendAll}, all) watched data of an entity. The legacy wire format is kept (entity id, then the
 * objects as type and id byte plus value, ended by 127). The modern game syncs an entity's data itself, so sending the packet
 * to a player adds nothing there.
 */
public class S1CPacketEntityMetadata extends Packet {
    public int field_149379_a;
    private List field_149378_b;

    public S1CPacketEntityMetadata() {
    }

    public S1CPacketEntityMetadata(int entityId, DataWatcher watcher, boolean sendAll) {
        this.field_149379_a = entityId;
        this.field_149378_b = sendAll ? watcher.getAllWatched() : watcher.getChanged();
    }

    public List func_149376_c() {
        return field_149378_b;
    }

    public int func_149375_d() {
        return field_149379_a;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149379_a = data.readInt();
        List out = new ArrayList();
        int header;
        while ((header = data.readUnsignedByte()) != 127) {
            int type = (header & 224) >> 5;
            int id = header & 31;
            Object value = switch (type) {
                case 0 -> data.readByte();
                case 1 -> data.readShort();
                case 2 -> data.readInt();
                case 3 -> data.readFloat();
                case 4 -> data.readStringFromBuffer(32767);
                case 5 -> data.readItemStackFromBuffer();
                default -> null;
            };
            out.add(new DataWatcher.WatchableObject(type, id, value));
        }
        field_149378_b = out;
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(field_149379_a);
        if (field_149378_b != null) {
            for (Object o : field_149378_b) {
                DataWatcher.WatchableObject w = (DataWatcher.WatchableObject) o;
                int type = w.getObjectType();
                data.writeByte((type << 5 | w.getDataValueId() & 31) & 255);
                Object v = w.getObject();
                switch (type) {
                    case 0 -> data.writeByte(((Number) v).byteValue());
                    case 1 -> data.writeShort(((Number) v).shortValue());
                    case 2 -> data.writeInt(((Number) v).intValue());
                    case 3 -> data.writeFloat(((Number) v).floatValue());
                    case 4 -> data.writeStringToBuffer((String) v);
                    case 5 -> data.writeItemStackToBuffer((legacyconnector.v1_7_10.net.minecraft.item.ItemStack) v);
                    default -> {
                    }
                }
            }
        }
        data.writeByte(127);
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
    }
}
