package legacyconnector.v1_12_2.net.minecraft.world.chunk;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;

/** The palette of a section with up to 16 distinct states: an array searched in order. */
public class BlockStatePaletteLinear implements IBlockStatePalette {
    public final IBlockState[] states;
    public final IBlockStatePaletteResizer resizeHandler;
    public final int bits;
    public int arraySize;

    public BlockStatePaletteLinear(int bitsIn, IBlockStatePaletteResizer resizeHandlerIn) {
        this.states = new IBlockState[1 << bitsIn];
        this.bits = bitsIn;
        this.resizeHandler = resizeHandlerIn;
    }

    @Override
    public int idFor(IBlockState state) {
        for (int i = 0; i < arraySize; ++i) if (states[i] == state) return i;
        int j = arraySize;
        if (j < states.length) {
            states[j] = state;
            ++arraySize;
            return j;
        }
        return resizeHandler.onResize(bits + 1, state);
    }

    @Override
    public IBlockState getBlockState(int indexKey) {
        return indexKey >= 0 && indexKey < arraySize ? states[indexKey] : null;
    }

    @Override
    public void read(PacketBuffer buf) {
        arraySize = buf.readVarInt();
        for (int i = 0; i < arraySize; ++i) states[i] = Block.getStateById(buf.readVarInt());
    }

    @Override
    public void write(PacketBuffer buf) {
        buf.writeVarInt(arraySize);
        for (int i = 0; i < arraySize; ++i) buf.writeVarInt(Block.getStateId(states[i]));
    }

    @Override
    public int getSerializedSize() {
        int size = PacketBuffer.getVarIntSize(arraySize);
        for (int i = 0; i < arraySize; ++i) size += PacketBuffer.getVarIntSize(Block.getStateId(states[i]));
        return size;
    }
}
