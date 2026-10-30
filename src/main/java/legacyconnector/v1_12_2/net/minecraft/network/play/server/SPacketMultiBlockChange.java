package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;

/** Several blocks of one chunk changed: their new states, read from the chunk when made; sent as modern block updates. */
public class SPacketMultiBlockChange implements Packet<INetHandlerPlayClient> {
    private ChunkPos chunkPos;
    private BlockUpdateData[] changedBlocks;

    public SPacketMultiBlockChange() {
    }

    /** {@code offsets} are 1.12's packed chunk-local positions: x << 12 | z << 8 | y. */
    public SPacketMultiBlockChange(int count, short[] offsets, Chunk chunk) {
        this.chunkPos = new ChunkPos(chunk.x, chunk.z);
        this.changedBlocks = new BlockUpdateData[count];
        for (int i = 0; i < count; i++) changedBlocks[i] = new BlockUpdateData(offsets[i], chunk);
    }

    public BlockUpdateData[] getChangedBlocks() {
        return changedBlocks;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packets
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packets
    }

    /** Handled by the modern client as the modern packets. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        if (changedBlocks == null || changedBlocks.length == 0) return null;
        java.util.List<net.minecraft.network.protocol.Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener>> updates = new java.util.ArrayList<>();
        for (BlockUpdateData d : changedBlocks) {
            if (d.state == null) continue;
            BlockPos p = d.getPos();
            updates.add(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()), d.state));
        }
        return updates.isEmpty() ? null : new net.minecraft.network.protocol.game.ClientboundBundlePacket(updates);
    }

    public class BlockUpdateData {
        private final short offset;
        private final net.minecraft.world.level.block.state.BlockState state;

        public BlockUpdateData(short offsetIn, IBlockState stateIn) {
            this.offset = offsetIn;
            this.state = stateIn == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(stateIn);
        }

        public BlockUpdateData(short offsetIn, Chunk chunkIn) {
            this.offset = offsetIn;
            var access = chunkIn == null ? null : chunkIn.mcc$access();
            BlockPos p = getPos();
            this.state = access == null ? null : access.getBlockState(new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()));
        }

        public BlockPos getPos() {
            return chunkPos.getBlock(offset >> 12 & 15, offset & 255, offset >> 8 & 15);
        }

        public short getOffset() {
            return offset;
        }

        public IBlockState getBlockState() {
            return state == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(state);
        }
    }
}
