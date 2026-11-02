package legacyconnector.v1_12_2.net.minecraft.world.chunk;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;

/** The palette of a section with more than 256 distinct states: the id is the state's global id ({@code Block.BLOCK_STATE_IDS}). */
public class BlockStatePaletteRegistry implements IBlockStatePalette {
    @Override
    public int idFor(IBlockState state) {
        int i = Block.BLOCK_STATE_IDS.get(state);
        return i == -1 ? 0 : i;
    }

    @Override
    public IBlockState getBlockState(int indexKey) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(indexKey);
        return state == null ? org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState() : state;
    }

    @Override
    public void read(PacketBuffer buf) {
        buf.readVarInt();
    }

    @Override
    public void write(PacketBuffer buf) {
        buf.writeVarInt(0);
    }

    @Override
    public int getSerializedSize() {
        return PacketBuffer.getVarIntSize(0);
    }
}
