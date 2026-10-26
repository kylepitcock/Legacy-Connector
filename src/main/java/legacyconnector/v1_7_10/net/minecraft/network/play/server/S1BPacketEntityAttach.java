package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * An entity rides (leash 0) or is leashed to (leash 1) another, or stops (vehicle null). Riding is sent as the vehicle's
 * current passengers (the server's riding state, which the sender has just changed); leashes as the leash link.
 */
public class S1BPacketEntityAttach extends Packet {
    public int field_149408_a;
    public int field_149406_b;
    public int field_149407_c;

    public S1BPacketEntityAttach() {
    }

    public S1BPacketEntityAttach(int leash, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, legacyconnector.v1_7_10.net.minecraft.entity.Entity vehicle) {
        this.field_149408_a = leash;
        this.field_149406_b = entity.getEntityId();
        this.field_149407_c = vehicle != null ? vehicle.getEntityId() : -1;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149406_b = data.readInt();
        field_149407_c = data.readInt();
        field_149408_a = data.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(field_149406_b);
        data.writeInt(field_149407_c);
        data.writeByte(field_149408_a);
    }

    public int func_149404_c() {
        return field_149408_a;
    }

    public int func_149403_d() {
        return field_149406_b;
    }

    public int func_149402_e() {
        return field_149407_c;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        var level = player.serverLevel();
        var entity = level.getEntity(field_149406_b);
        var other = field_149407_c < 0 ? null : level.getEntity(field_149407_c);
        if (field_149408_a == 1) {
            if (entity != null) player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket(entity, other));
            return;
        }
        if (other != null) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(other));
        } else if (entity != null && entity.getVehicle() != null) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(entity.getVehicle()));
        }
    }
}
