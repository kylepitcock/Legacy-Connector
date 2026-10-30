package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;

/** Sneaking, sprinting, leaving a bed, horse jumps, opening the horse inventory, elytra flight (sent as the game's player command). */
public class CPacketEntityAction implements Packet<INetHandlerPlayServer> {
    private int entityID;
    private Action action;
    private int auxData;
    private net.minecraft.world.entity.Entity modernEntity;

    public CPacketEntityAction() {
    }

    public CPacketEntityAction(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, Action action) {
        this(entity, action, 0);
    }

    public CPacketEntityAction(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, Action action, int auxData) {
        this.modernEntity = entity == null ? null : entity.mcc$modern();
        this.entityID = modernEntity == null ? 0 : modernEntity.getId();
        this.action = action;
        this.auxData = auxData;
    }

    public Action getAction() {
        return action;
    }

    public int getAuxData() {
        return auxData;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        entityID = buf.readVarInt();
        action = buf.readEnumValue(Action.class);
        auxData = buf.readVarInt();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(entityID);
        buf.writeEnumValue(action);
        buf.writeVarInt(auxData);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        if (modernEntity == null || action == null) return null;
        return new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(modernEntity,
                net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.values()[action.ordinal()], auxData);
    }

    public enum Action {
        START_SNEAKING,
        STOP_SNEAKING,
        STOP_SLEEPING,
        START_SPRINTING,
        STOP_SPRINTING,
        START_RIDING_JUMP,
        STOP_RIDING_JUMP,
        OPEN_INVENTORY,
        START_FALL_FLYING
    }
}
