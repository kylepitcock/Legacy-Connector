package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** 1.6.4's relative entity move packet (delta in 1/32 blocks per axis). */
public class Packet31RelEntityMove extends Packet30Entity {
    public Packet31RelEntityMove() {}

    public Packet31RelEntityMove(int entityId, byte x, byte y, byte z) {
        super(entityId);
        this.xPosition = x;
        this.yPosition = y;
        this.zPosition = z;
    }

    @Override
    protected boolean moves() {
        return true;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        super.readPacketData(in);
        xPosition = in.readByte();
        yPosition = in.readByte();
        zPosition = in.readByte();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        super.writePacketData(out);
        out.writeByte(xPosition);
        out.writeByte(yPosition);
        out.writeByte(zPosition);
    }

    @Override
    public int getPacketSize() {
        return 7;
    }
}
