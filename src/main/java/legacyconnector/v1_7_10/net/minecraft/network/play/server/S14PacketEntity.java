package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * Relative entity movement: position deltas in 1/32 block, rotations in 1/256 turns. Modern deltas are 1/4096 block,
 * so they are scaled by 128 when sent.
 */
public class S14PacketEntity extends Packet {
    public int field_149074_a;
    public byte field_149072_b;
    public byte field_149073_c;
    public byte field_149070_d;
    public byte field_149071_e;
    public byte field_149068_f;
    public boolean field_149069_g;

    public S14PacketEntity() {
    }

    public S14PacketEntity(int entityId) {
        this.field_149074_a = entityId;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149074_a = data.readInt();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(field_149074_a);
    }

    public legacyconnector.v1_7_10.net.minecraft.entity.Entity func_149065_a(legacyconnector.v1_7_10.net.minecraft.world.World world) {
        return world.getEntityByID(field_149074_a);
    }

    public byte func_149062_c() {
        return field_149072_b;
    }

    public byte func_149061_d() {
        return field_149073_c;
    }

    public byte func_149064_e() {
        return field_149070_d;
    }

    public byte func_149066_f() {
        return field_149071_e;
    }

    public byte func_149063_g() {
        return field_149068_f;
    }

    public boolean func_149060_h() {
        return field_149069_g;
    }

    @Override
    public String toString() {
        return "Entity_" + super.toString();
    }

    private boolean mcc$onGround(net.minecraft.server.level.ServerPlayer player) {
        var e = player.serverLevel().getEntity(field_149074_a);
        return e != null && e.onGround();
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        boolean ground = mcc$onGround(player);
        net.minecraft.network.protocol.Packet<?> modern;
        if (field_149069_g && (field_149072_b != 0 || field_149073_c != 0 || field_149070_d != 0)) {
            modern = new net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.PosRot(field_149074_a, (short) (field_149072_b * 128),
                    (short) (field_149073_c * 128), (short) (field_149070_d * 128), field_149071_e, field_149068_f, ground);
        } else if (field_149069_g) {
            modern = new net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Rot(field_149074_a, field_149071_e, field_149068_f, ground);
        } else {
            modern = new net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Pos(field_149074_a, (short) (field_149072_b * 128),
                    (short) (field_149073_c * 128), (short) (field_149070_d * 128), ground);
        }
        player.connection.send(modern);
    }

    /** Position change only. */
    public static class S15PacketEntityRelMove extends S14PacketEntity {
        public S15PacketEntityRelMove() {
        }

        public S15PacketEntityRelMove(int entityId, byte dx, byte dy, byte dz) {
            super(entityId);
            this.field_149072_b = dx;
            this.field_149073_c = dy;
            this.field_149070_d = dz;
        }

        @Override
        public void readPacketData(PacketBuffer data) throws IOException {
            super.readPacketData(data);
            field_149072_b = data.readByte();
            field_149073_c = data.readByte();
            field_149070_d = data.readByte();
        }

        @Override
        public void writePacketData(PacketBuffer data) throws IOException {
            super.writePacketData(data);
            data.writeByte(field_149072_b);
            data.writeByte(field_149073_c);
            data.writeByte(field_149070_d);
        }
    }

    /** Rotation change only. */
    public static class S16PacketEntityLook extends S14PacketEntity {
        public S16PacketEntityLook() {
            this.field_149069_g = true;
        }

        public S16PacketEntityLook(int entityId, byte yaw, byte pitch) {
            super(entityId);
            this.field_149071_e = yaw;
            this.field_149068_f = pitch;
            this.field_149069_g = true;
        }

        @Override
        public void readPacketData(PacketBuffer data) throws IOException {
            super.readPacketData(data);
            field_149071_e = data.readByte();
            field_149068_f = data.readByte();
        }

        @Override
        public void writePacketData(PacketBuffer data) throws IOException {
            super.writePacketData(data);
            data.writeByte(field_149071_e);
            data.writeByte(field_149068_f);
        }
    }

    /** Position and rotation change. */
    public static class S17PacketEntityLookMove extends S14PacketEntity {
        public S17PacketEntityLookMove() {
            this.field_149069_g = true;
        }

        public S17PacketEntityLookMove(int entityId, byte dx, byte dy, byte dz, byte yaw, byte pitch) {
            super(entityId);
            this.field_149072_b = dx;
            this.field_149073_c = dy;
            this.field_149070_d = dz;
            this.field_149071_e = yaw;
            this.field_149068_f = pitch;
            this.field_149069_g = true;
        }

        @Override
        public void readPacketData(PacketBuffer data) throws IOException {
            super.readPacketData(data);
            field_149072_b = data.readByte();
            field_149073_c = data.readByte();
            field_149070_d = data.readByte();
            field_149071_e = data.readByte();
            field_149068_f = data.readByte();
        }

        @Override
        public void writePacketData(PacketBuffer data) throws IOException {
            super.writePacketData(data);
            data.writeByte(field_149072_b);
            data.writeByte(field_149073_c);
            data.writeByte(field_149070_d);
            data.writeByte(field_149071_e);
            data.writeByte(field_149068_f);
        }
    }
}
