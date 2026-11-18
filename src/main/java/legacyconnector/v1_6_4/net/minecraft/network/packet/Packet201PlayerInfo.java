package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_6_4.forward.Packets164;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/**
 * 1.6.4's player list entry: the name, whether the player is online and the ping in milliseconds. The tab list is kept by the modern game for every
 * player that joins or leaves, so a delivered packet has nothing to add and is not forwarded.
 */
public class Packet201PlayerInfo extends Packet {
    public String playerName = "";
    public boolean isConnected;
    public int ping;

    public Packet201PlayerInfo() {}

    public Packet201PlayerInfo(String playerName, boolean isConnected, int ping) {
        this.playerName = playerName;
        this.isConnected = isConnected;
        this.ping = ping;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        playerName = Packets164.readString(in, 16);
        isConnected = in.readByte() != 0;
        ping = in.readShort();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        Packets164.writeString(playerName, out);
        out.writeByte(isConnected ? 1 : 0);
        out.writeShort(ping);
    }

    @Override
    public int getPacketSize() {
        return playerName.length() + 2 + 1 + 2;
    }
}
