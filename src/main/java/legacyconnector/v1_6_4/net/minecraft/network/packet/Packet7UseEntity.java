package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/** 1.6.4's use-entity packet, sent by the client: {@code isLeftClick} 1 attacks the target, 0 interacts with it. */
public class Packet7UseEntity extends Packet {
    public int playerEntityId;
    public int targetEntity;
    public int isLeftClick;

    public Packet7UseEntity() {}

    public Packet7UseEntity(int player, int target, int leftClick) {
        this.playerEntityId = player;
        this.targetEntity = target;
        this.isLeftClick = leftClick;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        playerEntityId = in.readInt();
        targetEntity = in.readInt();
        isLeftClick = in.readByte();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(playerEntityId);
        out.writeInt(targetEntity);
        out.writeByte(isLeftClick);
    }

    @Override
    public int getPacketSize() {
        return 9;
    }

    @Override
    public void mcc$sendToServer() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null || mc.getConnection() == null) return;
        net.minecraft.world.entity.Entity target = mc.level.getEntity(targetEntity);
        if (target == null) return;
        boolean sneaking = mc.player != null && mc.player.isShiftKeyDown();
        mc.getConnection().send(isLeftClick == 1
                ? net.minecraft.network.protocol.game.ServerboundInteractPacket.createAttackPacket(target, sneaking)
                : net.minecraft.network.protocol.game.ServerboundInteractPacket.createInteractionPacket(target, sneaking,
                        net.minecraft.world.InteractionHand.MAIN_HAND));
    }
}
