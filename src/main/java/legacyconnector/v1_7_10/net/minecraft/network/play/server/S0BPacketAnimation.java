package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** An entity animation: 0 swing arm, 1 hurt, 2 wake up, 4 critical hit, 5 magic critical hit. */
public class S0BPacketAnimation extends Packet {
    public int entityId;
    public int type;

    public S0BPacketAnimation() {
    }

    public S0BPacketAnimation(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, int type) {
        this.entityId = entity.getEntityId();
        this.type = type;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        entityId = data.readVarIntFromBuffer();
        type = data.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeVarIntToBuffer(entityId);
        data.writeByte(type);
    }

    public int func_148978_c() {
        return entityId;
    }

    public int func_148977_d() {
        return type;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(entityId);
        if (e == null) return;
        if (type == 1) {
            if (e instanceof net.minecraft.world.entity.LivingEntity living) {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket(living));
            }
        } else if (type == 0 || type == 2 || type == 4 || type == 5) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundAnimatePacket(e, type));
        }
    }
}
