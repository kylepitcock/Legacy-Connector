package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** A player started, aborted or finished digging a block (status 0, 1, 2) or dropped an item (3, 4, 5). */
public class C07PacketPlayerDigging extends Packet {
    public int diggedBlockX;
    public int diggedBlockY;
    public int diggedBlockZ;
    public int diggingBlockFace;
    public int status;

    public C07PacketPlayerDigging() {
    }

    public C07PacketPlayerDigging(int status, int x, int y, int z, int side) {
        this.status = status;
        this.diggedBlockX = x;
        this.diggedBlockY = y;
        this.diggedBlockZ = z;
        this.diggingBlockFace = side;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        status = data.readUnsignedByte();
        diggedBlockX = data.readInt();
        diggedBlockY = data.readUnsignedByte();
        diggedBlockZ = data.readInt();
        diggingBlockFace = data.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(status);
        data.writeInt(diggedBlockX);
        data.writeByte(diggedBlockY);
        data.writeInt(diggedBlockZ);
        data.writeByte(diggingBlockFace);
    }

    public int getDiggedBlockX() {
        return diggedBlockX;
    }

    public int getDiggedBlockY() {
        return diggedBlockY;
    }

    public int getDiggedBlockZ() {
        return diggedBlockZ;
    }

    public int getDiggingBlockFace() {
        return diggingBlockFace;
    }

    public int getDiggedBlockStatus() {
        return status;
    }
}
