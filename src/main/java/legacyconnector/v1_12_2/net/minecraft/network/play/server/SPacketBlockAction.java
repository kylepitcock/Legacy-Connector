package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** A block event for the clients around it (piston, chest lid, note block, a mod's own animation): sent as the modern block event packet. */
public class SPacketBlockAction implements Packet<INetHandlerPlayClient> {
    private BlockPos blockPosition;
    private int instrument;
    private int pitch;
    private Block block;

    public SPacketBlockAction() {
    }

    public SPacketBlockAction(BlockPos pos, Block blockIn, int eventId, int eventParam) {
        this.blockPosition = pos;
        this.block = blockIn;
        this.instrument = eventId;
        this.pitch = eventParam;
    }

    public BlockPos getBlockPosition() {
        return blockPosition;
    }

    public int getData1() {
        return instrument;
    }

    public int getData2() {
        return pitch;
    }

    public Block getBlockType() {
        return block;
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
        var modern = block == null ? null : block.mcc$modern instanceof net.minecraft.world.level.block.Block b ? b : null;
        if (modern == null || blockPosition == null) return null;
        return new net.minecraft.network.protocol.game.ClientboundBlockEventPacket(new net.minecraft.core.BlockPos(blockPosition.getX(), blockPosition.getY(),
                blockPosition.getZ()), modern, instrument, pitch);
    }
}
