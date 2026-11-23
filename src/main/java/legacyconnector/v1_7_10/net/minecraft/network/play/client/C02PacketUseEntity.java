package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The client attacked or right-clicked an entity. */
public class C02PacketUseEntity extends Packet {
    public enum Action {
        INTERACT,
        ATTACK
    }

    private int entityId;
    private Action action = Action.ATTACK;

    public C02PacketUseEntity() {
    }

    public C02PacketUseEntity(Entity target, Action action) {
        this.entityId = target.getEntityId();
        this.action = action;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        entityId = data.readInt();
        action = Action.values()[data.readByte() % Action.values().length];
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(entityId);
        data.writeByte(action.ordinal());
    }

    public Action getAction() {
        return action;
    }

    public Entity getEntityFromWorld(World world) {
        return world.getEntityByID(entityId);
    }

    @Override
    public void mcc$sendToServer() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        var target = mc.level.getEntity(entityId);
        if (target == null) return;
        boolean sneaking = mc.player.isShiftKeyDown();
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientNetwork1710.send(action == Action.ATTACK
                ? net.minecraft.network.protocol.game.ServerboundInteractPacket.createAttackPacket(target, sneaking)
                : net.minecraft.network.protocol.game.ServerboundInteractPacket.createInteractionPacket(target, sneaking,
                        net.minecraft.world.InteractionHand.MAIN_HAND));
    }
}
