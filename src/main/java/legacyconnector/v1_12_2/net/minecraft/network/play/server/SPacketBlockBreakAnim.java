package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** The cracks on a block being broken (progress 0-9, other values remove them). */
public class SPacketBlockBreakAnim implements Packet<INetHandlerPlayClient> {
    private int breakerId;
    private legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos position;
    private int progress;

    public SPacketBlockBreakAnim() {
    }

    public SPacketBlockBreakAnim(int breakerIdIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos positionIn, int progressIn) {
        this.breakerId = breakerIdIn;
        this.position = positionIn;
        this.progress = progressIn;
    }

    public int mcc$breakerId() {
        return breakerId;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos mcc$position() {
        return position;
    }

    public int mcc$progress() {
        return progress;
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
        return new net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket(breakerId, new net.minecraft.core.BlockPos(position.getX(), position.getY(), position.getZ()), progress);
    }
}
