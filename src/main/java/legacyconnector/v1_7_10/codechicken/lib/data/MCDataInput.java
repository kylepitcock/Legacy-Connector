package legacyconnector.v1_7_10.codechicken.lib.data;

import legacyconnector.v1_7_10.codechicken.lib.vec.BlockCoord;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack;

/** CodeChickenLib's read side of a data stream (packets, multipart descriptions). */
public interface MCDataInput {
    long readLong();

    int readInt();

    short readShort();

    int readUShort();

    byte readByte();

    short readUByte();

    double readDouble();

    float readFloat();

    boolean readBoolean();

    char readChar();

    int readVarShort();

    int readVarInt();

    byte[] readByteArray(int length);

    String readString();

    default BlockCoord readCoord() {
        return new BlockCoord(readInt(), readInt(), readInt());
    }

    default NBTTagCompound readNBTTagCompound() {
        return DataCodec.readTag(this);
    }

    default ItemStack readItemStack() {
        return readItemStack(true);
    }

    default ItemStack readItemStack(boolean large) {
        return DataCodec.readItemStack(this, large);
    }

    default FluidStack readFluidStack() {
        return DataCodec.readFluidStack(this);
    }
}
