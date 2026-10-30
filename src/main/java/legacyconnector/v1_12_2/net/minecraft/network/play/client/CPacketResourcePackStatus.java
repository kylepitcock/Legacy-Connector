package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;

/**
 * The client's answer to the server's resource pack. The game answers the server itself (per pack, by id), so a mod's status packet is
 * not sent.
 */
public class CPacketResourcePackStatus implements Packet<INetHandlerPlayServer> {
    private Action action;

    public CPacketResourcePackStatus() {
    }

    public CPacketResourcePackStatus(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        action = buf.readEnumValue(Action.class);
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeEnumValue(action);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    public enum Action {
        SUCCESSFULLY_LOADED,
        DECLINED,
        FAILED_DOWNLOAD,
        ACCEPTED
    }
}
