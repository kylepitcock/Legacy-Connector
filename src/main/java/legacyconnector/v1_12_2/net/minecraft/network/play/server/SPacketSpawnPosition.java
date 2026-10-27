package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** The world spawn the compass points to. */
public class SPacketSpawnPosition implements Packet<INetHandlerPlayClient> {
    private legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos spawnBlockPos;

    public SPacketSpawnPosition() {
    }

    public SPacketSpawnPosition(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos spawnBlockPosIn) {
        this.spawnBlockPos = spawnBlockPosIn;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos mcc$spawnBlockPos() {
        return spawnBlockPos;
    }


    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packet
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket(new net.minecraft.core.BlockPos(spawnBlockPos.getX(), spawnBlockPos.getY(), spawnBlockPos.getZ()), 0.0F);
    }
}
