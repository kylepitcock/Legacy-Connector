package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's animation packet: 1 swing the arm, 2 take damage, 3 leave bed, 5 eat, 6 critical hit, 7 enchanted hit. */
public class Packet18Animation extends Packet {
    public int entityId;
    public int animate;

    public Packet18Animation() {}

    public Packet18Animation(Entity entity, int animate) {
        this.entityId = entity.getEntityId();
        this.animate = animate;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        entityId = in.readInt();
        animate = in.readByte();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(entityId);
        out.writeByte(animate);
    }

    @Override
    public int getPacketSize() {
        return 5;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        net.minecraft.world.entity.Entity entity = player.serverLevel().getEntity(entityId);
        if (entity == null) return;
        switch (animate) {
            case 1 -> player.connection.send(new net.minecraft.network.protocol.game.ClientboundAnimatePacket(entity, 0));
            case 2 -> { if (entity instanceof net.minecraft.world.entity.LivingEntity living) player.connection.send(new net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket(living)); }
            case 3 -> player.connection.send(new net.minecraft.network.protocol.game.ClientboundAnimatePacket(entity, 2));
            case 6 -> player.connection.send(new net.minecraft.network.protocol.game.ClientboundAnimatePacket(entity, 4));
            case 7 -> player.connection.send(new net.minecraft.network.protocol.game.ClientboundAnimatePacket(entity, 5));
            default -> { } // 5 (eating) is part of the entity's use state, which the modern server sends itself
        }
    }
}
