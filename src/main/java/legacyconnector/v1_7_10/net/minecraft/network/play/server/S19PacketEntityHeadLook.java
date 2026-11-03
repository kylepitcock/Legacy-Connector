package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** An entity's head yaw, in 1/256 turns. */
public class S19PacketEntityHeadLook extends Packet {
    public int field_149384_a;
    public byte field_149383_b;

    public S19PacketEntityHeadLook() {
    }

    public S19PacketEntityHeadLook(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, byte yaw) {
        this.field_149384_a = entity.getEntityId();
        this.field_149383_b = yaw;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149384_a = data.readInt();
        field_149383_b = data.readByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(field_149384_a);
        data.writeByte(field_149383_b);
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        var entity = player.level().getEntity(field_149384_a);
        if (entity != null) player.connection.send(new net.minecraft.network.protocol.game.ClientboundRotateHeadPacket(entity, field_149383_b));
    }
}
