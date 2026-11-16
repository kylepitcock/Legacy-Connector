package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * The client's player changed stance. Actions: 1 start sneaking, 2 stop sneaking, 3 leave bed, 4 start sprinting, 5 stop sprinting, 6 start horse
 * jump, 7 stop horse jump, 8 open the horse inventory.
 */
public class C0BPacketEntityAction extends Packet {
    private int entityId;
    private int action;
    private int auxData;
    private Entity entity;

    public C0BPacketEntityAction() {
    }

    public C0BPacketEntityAction(Entity entity, int action) {
        this(entity, action, 0);
    }

    public C0BPacketEntityAction(Entity entity, int action, int auxData) {
        this.entity = entity;
        this.entityId = entity == null ? 0 : entity.getEntityId();
        this.action = action;
        this.auxData = auxData;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        entityId = data.readInt();
        action = data.readByte();
        auxData = data.readInt();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(entityId);
        data.writeByte(action);
        data.writeInt(auxData);
    }

    public int func_149512_e() {
        return entityId;
    }

    public int func_149513_d() {
        return action;
    }

    public int func_149509_c() {
        return auxData;
    }

    @Override
    public void mcc$sendToServer() {
        if (entity == null || entity.mcc$entity == null) return;
        var type = switch (action) {
            case 1 -> net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY;
            case 2 -> net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY;
            case 3 -> net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.STOP_SLEEPING;
            case 4 -> net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_SPRINTING;
            case 5 -> net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
            case 6 -> net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP;
            case 7 -> net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.STOP_RIDING_JUMP;
            case 8 -> net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY;
            default -> null;
        };
        if (type != null) {
            org.kylepitcock.legacyconnector.v1_7_10.client.ClientNetwork1710.send(
                    new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(entity.mcc$entity, type, auxData));
        }
    }
}
