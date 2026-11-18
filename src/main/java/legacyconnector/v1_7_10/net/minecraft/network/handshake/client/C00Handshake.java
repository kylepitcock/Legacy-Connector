package legacyconnector.v1_7_10.net.minecraft.network.handshake.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.EnumConnectionState;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * The first packet of a connection: protocol version, the address the client dialled, its port and the state it wants next (status or login).
 * It keeps 1.7.10's fields and wire format, which mods read and write themselves (server pingers, proxies). The layer does not send it: the
 * modern client makes its own handshake.
 */
public class C00Handshake extends Packet {
    private int protocolVersion;
    public String ip;
    public int port;
    private EnumConnectionState requestedState;

    public C00Handshake() {
    }

    public C00Handshake(int protocolVersion, String ip, int port, EnumConnectionState state) {
        this.protocolVersion = protocolVersion;
        this.ip = ip;
        this.port = port;
        this.requestedState = state;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        this.protocolVersion = data.readVarIntFromBuffer();
        this.ip = data.readStringFromBuffer(255);
        this.port = data.readUnsignedShort();
        int state = data.readVarIntFromBuffer();
        this.requestedState = state == 1 ? EnumConnectionState.STATUS : state == 2 ? EnumConnectionState.LOGIN : EnumConnectionState.HANDSHAKING;
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeVarIntToBuffer(this.protocolVersion);
        data.writeStringToBuffer(this.ip);
        data.writeShort(this.port);
        data.writeVarIntToBuffer(this.requestedState == EnumConnectionState.STATUS ? 1 : 2);
    }

    public EnumConnectionState getRequestedState() {
        return requestedState;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public boolean hasPriority() {
        return true;
    }
}
