package legacyconnector.v1_7_10.net.minecraft.network.status.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The server list ping's second packet: a timestamp the server answers with. */
public class C01PacketPing extends Packet {
    private long clientTime;

    public C01PacketPing() {
    }

    public C01PacketPing(long time) {
        this.clientTime = time;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        clientTime = data.readLong();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeLong(clientTime);
    }

    public long func_149289_c() {
        return clientTime;
    }
}
