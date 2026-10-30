package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;

/** The client closed a container window (sent as the game's container close packet). */
public class CPacketCloseWindow implements Packet<INetHandlerPlayServer> {
    private int windowId;

    public CPacketCloseWindow() {
    }

    public CPacketCloseWindow(int windowIdIn) {
        this.windowId = windowIdIn;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        windowId = buf.readByte();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeByte(windowId);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(windowId);
    }
}
