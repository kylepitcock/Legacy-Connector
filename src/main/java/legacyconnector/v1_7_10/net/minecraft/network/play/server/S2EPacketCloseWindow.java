package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The server closes a container window on the client. */
public class S2EPacketCloseWindow extends Packet {
    private int windowId;

    public S2EPacketCloseWindow() {
    }

    public S2EPacketCloseWindow(int windowId) {
        this.windowId = windowId;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        windowId = data.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(windowId);
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerClosePacket(windowId));
    }
}
