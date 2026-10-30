package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttributeInstance;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/**
 * An entity's attribute values for its watchers. Sent as the game's attribute update of that entity: the entity's modern attributes are
 * what the client needs, so the update carries its syncable ones (the legacy instances name which changed; the values are the same).
 */
public class SPacketEntityProperties implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private final List<String> names = new ArrayList<>();
    private final List<Double> baseValues = new ArrayList<>();

    public SPacketEntityProperties() {
    }

    public SPacketEntityProperties(int entityIdIn, Collection<IAttributeInstance> instances) {
        this.entityId = entityIdIn;
        if (instances != null) {
            for (IAttributeInstance i : instances) {
                names.add(i.getAttribute() == null ? "" : i.getAttribute().getName());
                baseValues.add(i.getBaseValue());
            }
        }
    }

    public int getEntityId() {
        return entityId;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        entityId = buf.readVarInt();
        int n = buf.readInt();
        names.clear();
        baseValues.clear();
        for (int i = 0; i < n; i++) {
            names.add(buf.readString(64));
            baseValues.add(buf.readDouble());
            int modifiers = buf.readVarInt();
            for (int j = 0; j < modifiers; j++) {
                buf.readLong();
                buf.readLong();
                buf.readDouble();
                buf.readByte();
            }
        }
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(entityId);
        buf.writeInt(names.size());
        for (int i = 0; i < names.size(); i++) {
            buf.writeString(names.get(i));
            buf.writeDouble(baseValues.get(i));
            buf.writeVarInt(0);
        }
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (var level : server.getAllLevels()) {
            if (level.getEntity(entityId) instanceof net.minecraft.world.entity.LivingEntity living) {
                return new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(entityId, living.getAttributes().getSyncableAttributes());
            }
        }
        return null;
    }
}
