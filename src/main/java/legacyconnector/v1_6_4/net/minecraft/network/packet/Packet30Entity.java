package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.6.4's entity packet and base of the relative move / look packets ({@link Packet31RelEntityMove}, {@link Packet32EntityLook},
 * {@link Packet33RelEntityMoveLook}): the position delta is in 1/32 blocks, angles are bytes (1/256 turns). Delivered as the modern
 * entity move / rotate packets; the bare packet 30 only carries the id and does nothing.
 */
public class Packet30Entity extends Packet {
    public int entityId;
    public byte xPosition;
    public byte yPosition;
    public byte zPosition;
    public byte yaw;
    public byte pitch;
    public boolean rotating;

    public Packet30Entity() {}

    public Packet30Entity(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        entityId = in.readInt();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(entityId);
    }

    @Override
    public int getPacketSize() {
        return 4;
    }

    /** True for the packets that carry a position delta. */
    protected boolean moves() {
        return false;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        boolean moves = moves();
        if (!moves && !rotating) return;
        var entity = player.serverLevel().getEntity(entityId);
        boolean ground = entity != null && entity.onGround();
        net.minecraft.network.protocol.Packet<?> modern;
        if (moves && rotating) {
            modern = new net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.PosRot(entityId, (short) (xPosition * 128), (short) (yPosition * 128),
                    (short) (zPosition * 128), yaw, pitch, ground);
        } else if (moves) {
            modern = new net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Pos(entityId, (short) (xPosition * 128), (short) (yPosition * 128),
                    (short) (zPosition * 128), ground);
        } else {
            modern = new net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Rot(entityId, yaw, pitch, ground);
        }
        player.connection.send(modern);
    }
}
