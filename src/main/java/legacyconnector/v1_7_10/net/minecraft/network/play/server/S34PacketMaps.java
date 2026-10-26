package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * Map pixels: {@code data[0]} 0 is a column update (x, y, then pixels), 1 the player markers, 2 the scale. Mods with
 * their own maps (Twilight Forest) serialize it into their channels; vanilla maps are synced by the modern game.
 */
public class S34PacketMaps extends Packet {
    public int mapId;
    public byte[] field_149190_b;

    public S34PacketMaps() {
    }

    public S34PacketMaps(int mapId, byte[] data) {
        this.mapId = mapId;
        this.field_149190_b = data;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        mapId = data.readVarIntFromBuffer();
        field_149190_b = new byte[data.readUnsignedShort()];
        data.readBytes(field_149190_b);
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeVarIntToBuffer(mapId);
        data.writeShort(field_149190_b.length);
        data.writeBytes(field_149190_b);
    }

    public int getMapId() {
        return mapId;
    }

    public byte[] getData() {
        return field_149190_b;
    }

    @Override
    public String serialize() {
        return String.format("id=%d, length=%d", mapId, field_149190_b == null ? 0 : field_149190_b.length);
    }
}
