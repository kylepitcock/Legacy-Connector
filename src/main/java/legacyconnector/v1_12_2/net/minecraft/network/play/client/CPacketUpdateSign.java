package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** A sign's four edited lines (sent as the game's sign update for the front text). */
public class CPacketUpdateSign implements Packet<INetHandlerPlayServer> {
    private BlockPos pos;
    private String[] lines;

    public CPacketUpdateSign() {
    }

    public CPacketUpdateSign(BlockPos pos, ITextComponent[] lines) {
        this.pos = pos;
        this.lines = new String[] {lines[0].getUnformattedText(), lines[1].getUnformattedText(), lines[2].getUnformattedText(), lines[3].getUnformattedText()};
    }

    public BlockPos getPosition() {
        return pos;
    }

    public String[] getLines() {
        return lines;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        pos = buf.readBlockPos();
        lines = new String[4];
        for (int i = 0; i < 4; i++) lines[i] = buf.readString(384);
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeBlockPos(pos);
        for (int i = 0; i < 4; i++) buf.writeString(lines[i]);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ServerboundSignUpdatePacket(pos.mcc$modern(), true, lines[0], lines[1], lines[2], lines[3]);
    }
}
