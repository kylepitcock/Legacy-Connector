package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** 1.6.4's entity look packet (yaw and pitch as bytes). */
public class Packet32EntityLook extends Packet30Entity {
    public Packet32EntityLook() {
        this.rotating = true;
    }

    public Packet32EntityLook(int entityId, byte yaw, byte pitch) {
        super(entityId);
        this.yaw = yaw;
        this.pitch = pitch;
        this.rotating = true;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        super.readPacketData(in);
        yaw = in.readByte();
        pitch = in.readByte();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        super.writePacketData(out);
        out.writeByte(yaw);
        out.writeByte(pitch);
    }

    @Override
    public int getPacketSize() {
        return 6;
    }
}
