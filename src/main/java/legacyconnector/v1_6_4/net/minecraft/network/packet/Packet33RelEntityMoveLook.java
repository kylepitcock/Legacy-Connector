package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** 1.6.4's relative entity move and look packet. */
public class Packet33RelEntityMoveLook extends Packet30Entity {
    public Packet33RelEntityMoveLook() {
        this.rotating = true;
    }

    public Packet33RelEntityMoveLook(int entityId, byte x, byte y, byte z, byte yaw, byte pitch) {
        super(entityId);
        this.xPosition = x;
        this.yPosition = y;
        this.zPosition = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.rotating = true;
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
        yaw = in.readByte();
        pitch = in.readByte();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        super.writePacketData(out);
        out.writeByte(xPosition);
        out.writeByte(yPosition);
        out.writeByte(zPosition);
        out.writeByte(yaw);
        out.writeByte(pitch);
    }

    @Override
    public int getPacketSize() {
        return 9;
    }
}
