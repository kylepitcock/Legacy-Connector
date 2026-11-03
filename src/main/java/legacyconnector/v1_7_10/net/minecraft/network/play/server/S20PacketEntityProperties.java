package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.Collection;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * An entity's attributes (health, speed, ...). The modern client takes them from the server's entity itself, so the packet
 * carries the entity id and sends the entity's synchronised attributes.
 */
public class S20PacketEntityProperties extends Packet {
    public int field_149445_a;

    public S20PacketEntityProperties() {
    }

    public S20PacketEntityProperties(int entityId, Collection attributes) {
        this.field_149445_a = entityId;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149445_a = data.readInt();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(field_149445_a);
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        if (player.level().getEntity(field_149445_a) instanceof net.minecraft.world.entity.LivingEntity living) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(field_149445_a,
                    living.getAttributes().getSyncableAttributes()));
        }
    }
}
