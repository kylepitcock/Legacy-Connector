package legacyconnector.v1_12_2.net.minecraft.world.chunk;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.util.IntIdentityHashBiMap;

/** The palette of a section with 17 to 256 distinct states: an identity bimap whose {@code byId} array holds them. */
public class BlockStatePaletteHashMap implements IBlockStatePalette {
    public final IntIdentityHashBiMap<IBlockState> statePaletteMap;
    public final IBlockStatePaletteResizer paletteResizer;
    public final int bits;

    public BlockStatePaletteHashMap(int bitsIn, IBlockStatePaletteResizer resizerIn) {
        this.bits = bitsIn;
        this.paletteResizer = resizerIn;
        this.statePaletteMap = new IntIdentityHashBiMap<>(1 << bitsIn);
    }

    @Override
    public int idFor(IBlockState state) {
        int i = statePaletteMap.getId(state);
        if (i == -1) {
            i = statePaletteMap.add(state);
            if (i >= 1 << bits) i = paletteResizer.onResize(bits + 1, state);
        }
        return i;
    }

    @Override
    public IBlockState getBlockState(int indexKey) {
        return statePaletteMap.get(indexKey);
    }

    @Override
    public void read(PacketBuffer buf) {
        statePaletteMap.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; ++i) statePaletteMap.add(Block.getStateById(buf.readVarInt()));
    }

    @Override
    public void write(PacketBuffer buf) {
        int n = statePaletteMap.size();
        buf.writeVarInt(n);
        for (int i = 0; i < n; ++i) buf.writeVarInt(Block.getStateId(statePaletteMap.get(i)));
    }

    @Override
    public int getSerializedSize() {
        int size = PacketBuffer.getVarIntSize(statePaletteMap.size());
        for (int i = 0; i < statePaletteMap.size(); ++i) size += PacketBuffer.getVarIntSize(Block.getStateId(statePaletteMap.get(i)));
        return size;
    }
}
