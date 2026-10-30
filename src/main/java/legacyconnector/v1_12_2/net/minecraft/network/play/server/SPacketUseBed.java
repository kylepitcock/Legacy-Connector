package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/**
 * A player went to bed. The modern game syncs sleeping through the player's entity data (its pose and bed position), so this packet
 * has no modern form: sending it marks the player as sleeping in that bed on the server, which every watcher then sees.
 */
public class SPacketUseBed implements Packet<INetHandlerPlayClient> {
    private int playerID;
    private BlockPos bedPos;

    public SPacketUseBed() {
    }

    public SPacketUseBed(EntityPlayer player, BlockPos posIn) {
        this.playerID = player.getEntityId();
        this.bedPos = posIn;
        var m = player.mcc$player();
        if (m != null && posIn != null && !m.level().isClientSide() && !m.isSleeping()) m.startSleeping(posIn.mcc$modern());
    }

    public BlockPos getBedPosition() {
        return bedPos;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        playerID = buf.readVarInt();
        bedPos = buf.readBlockPos();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(playerID);
        buf.writeBlockPos(bedPos);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }
}
