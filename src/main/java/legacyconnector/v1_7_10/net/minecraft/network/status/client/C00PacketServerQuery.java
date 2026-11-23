package legacyconnector.v1_7_10.net.minecraft.network.status.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The server list ping's first packet: asks for the server's status. It has no body. */
public class C00PacketServerQuery extends Packet {
    public C00PacketServerQuery() {
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
    }
}
