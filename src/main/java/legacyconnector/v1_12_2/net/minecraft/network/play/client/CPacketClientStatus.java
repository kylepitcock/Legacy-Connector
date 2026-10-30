package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;

/** Respawning or asking for the statistics (sent as the game's client command packet). */
public class CPacketClientStatus implements Packet<INetHandlerPlayServer> {
    private State status;

    public CPacketClientStatus() {
    }

    public CPacketClientStatus(State status) {
        this.status = status;
    }

    public State getStatus() {
        return status;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        status = buf.readEnumValue(State.class);
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeEnumValue(status);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(status == State.REQUEST_STATS
                ? net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.REQUEST_STATS
                : net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN);
    }

    public enum State {
        PERFORM_RESPAWN,
        REQUEST_STATS
    }
}
