package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;

/** Which boat paddles are moving (sent as the game's paddle packet). */
public class CPacketSteerBoat implements Packet<INetHandlerPlayServer> {
    private boolean left;
    private boolean right;

    public CPacketSteerBoat() {
    }

    public CPacketSteerBoat(boolean left, boolean right) {
        this.left = left;
        this.right = right;
    }

    public boolean getLeft() {
        return left;
    }

    public boolean getRight() {
        return right;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        left = buf.readBoolean();
        right = buf.readBoolean();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeBoolean(left);
        buf.writeBoolean(right);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket(left, right);
    }
}
