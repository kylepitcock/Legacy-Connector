package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A block's new state, read from the world when made. */
public class SPacketBlockChange implements Packet<INetHandlerPlayClient> {
    private net.minecraft.world.level.block.state.BlockState state;
    private legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos blockPosition;

    public SPacketBlockChange() {
    }

    public SPacketBlockChange(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos posIn) {
        this.blockPosition = posIn;
        this.state = worldIn.mcc$level().getBlockState(new net.minecraft.core.BlockPos(posIn.getX(), posIn.getY(), posIn.getZ()));
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getBlockPosition() {
        return blockPosition;
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
        return new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(new net.minecraft.core.BlockPos(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ()), state);
    }
}
