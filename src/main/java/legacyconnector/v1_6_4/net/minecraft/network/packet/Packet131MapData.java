package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.6.4's map data packet, which FML also used as the "tiny packet" (a short tag and a small byte array, {@code ITinyPacketHandler}).
 * A tiny packet made by {@code PacketDispatcher.getTinyPacket} remembers its mod and travels on the layer's own channel
 * ({@code bridge.Network164.TINY_CHANNEL}); a map data packet proper is not sent.
 */
public class Packet131MapData extends Packet {
    public short itemID;
    public short uniqueID;
    public byte[] itemData = new byte[0];
    /** Set for tiny packets: the legacy id of the mod the packet is for. */
    public String mcc$modId;

    public Packet131MapData() {}

    public Packet131MapData(short itemID, short uniqueID, byte[] itemData) {
        this.itemID = itemID;
        this.uniqueID = uniqueID;
        this.itemData = itemData;
    }

    public void readPacketData(DataInput in) throws IOException {
        itemID = in.readShort();
        uniqueID = in.readShort();
        itemData = new byte[in.readUnsignedByte()];
        in.readFully(itemData);
    }

    public void writePacketData(DataOutput out) throws IOException {
        out.writeShort(itemID);
        out.writeShort(uniqueID);
        out.writeByte(itemData.length);
        out.write(itemData);
    }

    public int getPacketSize() {
        return 4 + itemData.length;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        if (mcc$modId != null) org.kylepitcock.legacyconnector.v1_6_4.bridge.Network164.tiny(this).mcc$send(player);
    }

    @Override
    public void mcc$sendToServer() {
        if (mcc$modId != null) org.kylepitcock.legacyconnector.v1_6_4.bridge.Network164.tiny(this).mcc$sendToServer();
    }
}
