package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_12_2.net.minecraft.world.storage.MapData;
import legacyconnector.v1_12_2.net.minecraft.world.storage.MapDecoration;

/** A map's icons and a changed rectangle of its colours (sent as the game's map data packet). */
public class SPacketMaps implements Packet<INetHandlerPlayClient> {
    private int mapId;
    private byte mapScale;
    private boolean trackingPosition;
    private MapDecoration[] icons = new MapDecoration[0];
    private int minX;
    private int minZ;
    private int columns;
    private int rows;
    private byte[] mapDataBytes = new byte[0];

    public SPacketMaps() {
    }

    public SPacketMaps(int mapIdIn, byte mapScaleIn, boolean trackingPositionIn, Collection<MapDecoration> iconsIn, byte[] colors, int minXIn, int minZIn,
            int columnsIn, int rowsIn) {
        this.mapId = mapIdIn;
        this.mapScale = mapScaleIn;
        this.trackingPosition = trackingPositionIn;
        this.icons = iconsIn == null ? new MapDecoration[0] : iconsIn.toArray(new MapDecoration[0]);
        this.minX = minXIn;
        this.minZ = minZIn;
        this.columns = columnsIn;
        this.rows = rowsIn;
        this.mapDataBytes = new byte[columnsIn * rowsIn];
        for (int i = 0; i < columnsIn; ++i) {
            for (int j = 0; j < rowsIn; ++j) this.mapDataBytes[i + j * columnsIn] = colors[minXIn + i + (minZIn + j) * 128];
        }
    }

    public int getMapId() {
        return mapId;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        mapId = buf.readVarInt();
        mapScale = buf.readByte();
        trackingPosition = buf.readBoolean();
        icons = new MapDecoration[buf.readVarInt()];
        for (int i = 0; i < icons.length; ++i) {
            short s = buf.readByte();
            icons[i] = new MapDecoration(MapDecoration.Type.byIcon((byte) (s >> 4 & 15)), buf.readByte(), buf.readByte(), (byte) (s & 15));
        }
        columns = buf.readUnsignedByte();
        if (columns > 0) {
            rows = buf.readUnsignedByte();
            minX = buf.readUnsignedByte();
            minZ = buf.readUnsignedByte();
            mapDataBytes = buf.readByteArray();
        }
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(mapId);
        buf.writeByte(mapScale);
        buf.writeBoolean(trackingPosition);
        buf.writeVarInt(icons.length);
        for (MapDecoration d : icons) {
            buf.writeByte((d.getImage() & 15) << 4 | d.getRotation() & 15);
            buf.writeByte(d.getX());
            buf.writeByte(d.getY());
        }
        buf.writeByte(columns);
        if (columns > 0) {
            buf.writeByte(rows);
            buf.writeByte(minX);
            buf.writeByte(minZ);
            buf.writeByteArray(mapDataBytes);
        }
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    /** Applies the icons and the colour rectangle to a map's data (the client's copy). */
    public void setMapdataTo(MapData mapdataIn) {
        if (mapdataIn == null) return;
        mapdataIn.scale = mapScale;
        mapdataIn.trackingPosition = trackingPosition;
        mapdataIn.mapDecorations.clear();
        for (int i = 0; i < icons.length; ++i) mapdataIn.mapDecorations.put("icon-" + i, icons[i]);
        for (int j = 0; j < columns; ++j) {
            for (int k = 0; k < rows; ++k) mapdataIn.colors[minX + j + (minZ + k) * 128] = mapDataBytes[j + k * columns];
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        java.util.List<net.minecraft.world.level.saveddata.maps.MapDecoration> decorations = new java.util.ArrayList<>();
        for (MapDecoration d : icons) {
            decorations.add(new net.minecraft.world.level.saveddata.maps.MapDecoration(d.getType().mcc$modern(), d.getX(), d.getY(), d.getRotation(),
                    java.util.Optional.empty()));
        }
        var patch = columns > 0 ? new net.minecraft.world.level.saveddata.maps.MapItemSavedData.MapPatch(minX, minZ, columns, rows, mapDataBytes) : null;
        return new net.minecraft.network.protocol.game.ClientboundMapItemDataPacket(new net.minecraft.world.level.saveddata.maps.MapId(mapId), mapScale, false,
                decorations, patch);
    }
}
