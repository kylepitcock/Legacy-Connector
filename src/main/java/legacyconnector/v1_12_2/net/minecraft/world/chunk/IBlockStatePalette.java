package legacyconnector.v1_12_2.net.minecraft.world.chunk;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;

/** 1.12.2's mapping between the small ids a section's {@code BitArray} stores and block states. */
public interface IBlockStatePalette {
    int idFor(IBlockState state);

    IBlockState getBlockState(int indexKey);

    void read(PacketBuffer buf);

    void write(PacketBuffer buf);

    int getSerializedSize();
}
