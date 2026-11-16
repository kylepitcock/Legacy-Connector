package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The client finished editing a sign: its position and four lines. */
public class C12PacketUpdateSign extends Packet {
    private int x;
    private int y;
    private int z;
    private String[] lines = {"", "", "", ""};

    public C12PacketUpdateSign() {
    }

    public C12PacketUpdateSign(int x, int y, int z, String[] lines) {
        this.x = x;
        this.y = y;
        this.z = z;
        for (int i = 0; i < 4; i++) this.lines[i] = lines != null && i < lines.length && lines[i] != null ? lines[i] : "";
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        x = data.readInt();
        y = data.readShort();
        z = data.readInt();
        for (int i = 0; i < 4; i++) lines[i] = data.readStringFromBuffer(15);
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(x);
        data.writeShort(y);
        data.writeInt(z);
        for (int i = 0; i < 4; i++) data.writeStringToBuffer(lines[i]);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String[] getLines() {
        return lines;
    }

    @Override
    public void mcc$sendToServer() {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientNetwork1710.send(new net.minecraft.network.protocol.game.ServerboundSignUpdatePacket(
                new net.minecraft.core.BlockPos(x, y, z), true, lines[0], lines[1], lines[2], lines[3]));
    }
}
