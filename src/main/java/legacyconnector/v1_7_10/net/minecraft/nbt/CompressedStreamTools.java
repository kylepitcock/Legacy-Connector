package legacyconnector.v1_7_10.net.minecraft.nbt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/** NBT file/stream IO (the format did not change between 1.7.10 and 1.21). */
public class CompressedStreamTools {
    public static NBTTagCompound readCompressed(InputStream in) throws IOException {
        return new NBTTagCompound(NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
    }

    public static void writeCompressed(NBTTagCompound tag, OutputStream out) throws IOException {
        NbtIo.writeCompressed(tag.mcc$modern(), out);
    }

    public static byte[] compress(NBTTagCompound tag) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeCompressed(tag, out);
        return out.toByteArray();
    }

    public static NBTTagCompound decompress(byte[] data) throws IOException {
        return readCompressed(new ByteArrayInputStream(data));
    }

    public static NBTTagCompound read(DataInput in) throws IOException {
        return new NBTTagCompound(NbtIo.read(in));
    }

    public static void write(NBTTagCompound tag, DataOutput out) throws IOException {
        NbtIo.write(tag.mcc$modern(), out);
    }

    public static NBTTagCompound read(File file) throws IOException {
        if (!file.exists()) return null;
        try (InputStream in = new FileInputStream(file)) {
            return readCompressed(in);
        }
    }

    public static void safeWrite(NBTTagCompound tag, File file) throws IOException {
        File tmp = new File(file.getAbsolutePath() + "_tmp");
        if (tmp.exists()) tmp.delete();
        try (OutputStream out = new FileOutputStream(tmp)) {
            writeCompressed(tag, out);
        }
        if (file.exists()) file.delete();
        if (file.exists()) throw new IOException("Failed to delete " + file);
        tmp.renameTo(file);
    }

    public static NBTTagCompound decompress(byte[] data, NBTSizeTracker tracker) throws IOException {
        return decompress(data);
    }

    public static NBTTagCompound read(DataInput in, NBTSizeTracker tracker) throws IOException {
        return read(in);
    }

    /** 1.7.10's MCP mappings never named this one, so mods call it by its SRG name (DragonAPI's NBT files). */
    public static NBTTagCompound func_152456_a(DataInput in, NBTSizeTracker tracker) throws IOException {
        return read(in);
    }

    public static NBTTagCompound readFromFile(File file, NBTSizeTracker tracker) throws IOException {
        return read(file);
    }

    public static NBTTagCompound read(java.io.DataInputStream in) throws IOException {
        return read((DataInput) in);
    }

    public static void write(NBTTagCompound tag, File file) throws IOException {
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(new FileOutputStream(file))) {
            write(tag, (DataOutput) out);
        }
    }
}
