package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/** 1.6.4's entity action packet (client to server): 1 crouch, 2 uncrouch, 3 leave bed, 4 start sprinting, 5 stop sprinting, 6 jump with a horse (aux = strength). */
public class Packet19EntityAction extends Packet {
    public int entityId;
    public int state;
    public int auxData;

    public Packet19EntityAction() {}

    public Packet19EntityAction(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, int state) {
        this(entity, state, 0);
    }

    public Packet19EntityAction(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, int state, int aux) {
        this.entityId = entity.getEntityId();
        this.state = state;
        this.auxData = aux;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        entityId = in.readInt();
        state = in.readByte();
        auxData = in.readInt();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(entityId);
        out.writeByte(state);
        out.writeInt(auxData);
    }

    @Override
    public int getPacketSize() {
        return 9;
    }
}
