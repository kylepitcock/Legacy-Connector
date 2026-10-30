package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** The client uses the item in a hand on a block face at a hit point (0-1 inside the block); sent as the game's use-item-on packet. */
public class CPacketPlayerTryUseItemOnBlock implements Packet<INetHandlerPlayServer> {
    private BlockPos position;
    private EnumFacing placedBlockDirection;
    private EnumHand hand;
    private float facingX;
    private float facingY;
    private float facingZ;

    public CPacketPlayerTryUseItemOnBlock() {
    }

    public CPacketPlayerTryUseItemOnBlock(BlockPos posIn, EnumFacing placedBlockDirectionIn, EnumHand handIn, float facingXIn, float facingYIn, float facingZIn) {
        this.position = posIn;
        this.placedBlockDirection = placedBlockDirectionIn;
        this.hand = handIn;
        this.facingX = facingXIn;
        this.facingY = facingYIn;
        this.facingZ = facingZIn;
    }

    public BlockPos getPos() {
        return position;
    }

    public EnumFacing getDirection() {
        return placedBlockDirection;
    }

    public EnumHand getHand() {
        return hand;
    }

    public float getFacingX() {
        return facingX;
    }

    public float getFacingY() {
        return facingY;
    }

    public float getFacingZ() {
        return facingZ;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        position = BlockPos.fromLong(buf.readLong());
        placedBlockDirection = EnumFacing.byIndex(buf.readVarInt());
        hand = buf.readVarInt() == 1 ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        facingX = buf.readFloat();
        facingY = buf.readFloat();
        facingZ = buf.readFloat();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeLong(position.toLong());
        buf.writeVarInt(placedBlockDirection.getIndex());
        buf.writeVarInt(hand == EnumHand.OFF_HAND ? 1 : 0);
        buf.writeFloat(facingX);
        buf.writeFloat(facingY);
        buf.writeFloat(facingZ);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        if (position == null || placedBlockDirection == null) return null;
        var pos = position.mcc$modern();
        var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + facingX, pos.getY() + facingY, pos.getZ() + facingZ),
                placedBlockDirection.mcc$modern(), pos, false);
        return new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(hand == null ? net.minecraft.world.InteractionHand.MAIN_HAND : hand.mcc$modern(), hit, 0);
    }

    /** The client player's yaw and pitch (the modern use packets carry them), or zero on a server. */
    static float[] mcc$look() {
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return new float[2];
        return ClientLook.look();
    }

    private static final class ClientLook {
        static float[] look() {
            var p = net.minecraft.client.Minecraft.getInstance().player;
            return p == null ? new float[2] : new float[] {p.getYRot(), p.getXRot()};
        }
    }
}
