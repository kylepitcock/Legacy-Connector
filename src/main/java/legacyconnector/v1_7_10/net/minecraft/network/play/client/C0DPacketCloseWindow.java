package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The client closed a container window. */
public class C0DPacketCloseWindow extends Packet {
    public int windowId;

    public C0DPacketCloseWindow() {
    }

    public C0DPacketCloseWindow(int windowId) {
        this.windowId = windowId;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        windowId = data.readByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(windowId);
    }

    @Override
    public void mcc$sendToServer() {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientNetwork1710.send(new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(windowId));
    }
}
