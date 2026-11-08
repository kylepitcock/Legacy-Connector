package legacyconnector.v1_7_10.codechicken.lib.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/** Shared wire formats of the composite values (NBT, item and fluid stacks) the data streams read and write. */
final class DataCodec {
    private DataCodec() {}

    static MCDataOutput writeTag(MCDataOutput out, NBTTagCompound tag) {
        if (tag == null) {
            out.writeBoolean(false);
            return out;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.write(tag.mcc$modern(), new DataOutputStream(bytes));
            out.writeBoolean(true);
            out.writeVarInt(bytes.size());
            out.writeByteArray(bytes.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    static NBTTagCompound readTag(MCDataInput in) {
        if (!in.readBoolean()) return null;
        byte[] data = in.readByteArray(in.readVarInt());
        try {
            CompoundTag tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(data)), NbtAccounter.create(2097152L));
            return new NBTTagCompound(tag);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static MCDataOutput writeItemStack(MCDataOutput out, ItemStack stack, boolean large) {
        if (stack == null) {
            out.writeBoolean(false);
            return out;
        }
        out.writeBoolean(true);
        NBTTagCompound tag = new NBTTagCompound();
        stack.writeToNBT(tag);
        return writeTag(out, tag);
    }

    static ItemStack readItemStack(MCDataInput in, boolean large) {
        if (!in.readBoolean()) return null;
        NBTTagCompound tag = readTag(in);
        return tag == null ? null : ItemStack.loadItemStackFromNBT(tag);
    }

    static MCDataOutput writeFluidStack(MCDataOutput out, FluidStack fluid) {
        if (fluid == null) {
            out.writeBoolean(false);
            return out;
        }
        out.writeBoolean(true);
        return writeTag(out, fluid.writeToNBT(new NBTTagCompound()));
    }

    static FluidStack readFluidStack(MCDataInput in) {
        if (!in.readBoolean()) return null;
        return FluidStack.loadFluidStackFromNBT(readTag(in));
    }
}
