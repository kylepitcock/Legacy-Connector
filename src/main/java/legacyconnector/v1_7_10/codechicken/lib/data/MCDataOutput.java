package legacyconnector.v1_7_10.codechicken.lib.data;

import legacyconnector.v1_7_10.codechicken.lib.vec.BlockCoord;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack;

/** CodeChickenLib's write side of a data stream; every method returns the stream so calls chain. */
public interface MCDataOutput {
    MCDataOutput writeBoolean(boolean b);

    MCDataOutput writeByte(int b);

    MCDataOutput writeShort(int s);

    MCDataOutput writeInt(int i);

    MCDataOutput writeFloat(float f);

    MCDataOutput writeDouble(double d);

    MCDataOutput writeLong(long l);

    MCDataOutput writeChar(char c);

    /** A short in one or two bytes (the high bit of the first says a second follows). */
    default MCDataOutput writeVarShort(int s) {
        int low = s & 0x7FFF;
        int high = (s & 0x7F8000) >> 15;
        if (high != 0) low |= 0x8000;
        writeShort(low);
        if (high != 0) writeByte(high);
        return this;
    }

    /** An int in 1 to 5 bytes, seven bits at a time. */
    default MCDataOutput writeVarInt(int i) {
        while ((i & 0xFFFFFF80) != 0) {
            writeByte(i & 0x7F | 0x80);
            i >>>= 7;
        }
        return writeByte(i);
    }

    /** The bytes as they are (no length: the reader says how many it expects). */
    default MCDataOutput writeByteArray(byte[] barray) {
        for (byte b : barray) writeByte(b);
        return this;
    }

    /** A length (var int) and the UTF-8 bytes. */
    default MCDataOutput writeString(String s) {
        byte[] b = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        writeVarInt(b.length);
        return writeByteArray(b);
    }

    default MCDataOutput writeCoord(int x, int y, int z) {
        return writeInt(x).writeInt(y).writeInt(z);
    }

    default MCDataOutput writeCoord(BlockCoord c) {
        return writeCoord(c.x, c.y, c.z);
    }

    default MCDataOutput writeNBTTagCompound(NBTTagCompound tag) {
        return DataCodec.writeTag(this, tag);
    }

    default MCDataOutput writeItemStack(ItemStack stack) {
        return writeItemStack(stack, true);
    }

    default MCDataOutput writeItemStack(ItemStack stack, boolean large) {
        return DataCodec.writeItemStack(this, stack, large);
    }

    default MCDataOutput writeFluidStack(FluidStack fluid) {
        return DataCodec.writeFluidStack(this, fluid);
    }
}
