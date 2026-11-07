package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_6_4.forward.Packets164;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/**
 * 1.6.4's server list ping (id 254): {@code readSuccessfully} is the ping version byte, then the "MC|PingHost" plugin message with the protocol
 * version, host name and port the client connected to. The layer answers pings through the modern status protocol; this holds the data for
 * mods that parse or build it.
 */
public class Packet254ServerPing extends Packet {
    public int readSuccessfully;
    public int field_82559_a;
    public String field_82558_b;
    public int field_82557_c;

    public Packet254ServerPing() {}

    public Packet254ServerPing(int protocol, String host, int port) {
        this.readSuccessfully = 1;
        this.field_82559_a = protocol;
        this.field_82558_b = host;
        this.field_82557_c = port;
    }

    @Override
    public int getPacketId() {
        return 254;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        readSuccessfully = in.readByte();
        if (readSuccessfully < 1) return;
        in.readByte();
        Packets164.readString(in, 255);
        in.readShort();
        field_82559_a = in.readByte();
        field_82558_b = Packets164.readString(in, 255);
        field_82557_c = in.readInt();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeByte(readSuccessfully);
        if (readSuccessfully < 1) return;
        out.writeByte(250);
        Packets164.writeString("MC|PingHost", out);
        out.writeShort(3 + 2 * (field_82558_b == null ? 0 : field_82558_b.length()) + 4);
        out.writeByte(field_82559_a);
        Packets164.writeString(field_82558_b == null ? "" : field_82558_b, out);
        out.writeInt(field_82557_c);
    }

    @Override
    public int getPacketSize() {
        return 3 + (readSuccessfully < 1 ? 0 : 27 + 2 * (field_82558_b == null ? 0 : field_82558_b.length()));
    }
}
