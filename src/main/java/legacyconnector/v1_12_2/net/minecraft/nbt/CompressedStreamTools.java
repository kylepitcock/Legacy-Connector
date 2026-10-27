package legacyconnector.v1_12_2.net.minecraft.nbt;

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

/** NBT file/stream IO (the format did not change between 1.12.2 and 1.21). */
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

    /** {@code read(DataInputStream)}: the uncompressed stream form (the descriptor 1.12.2 mods call). */
    public static NBTTagCompound read(java.io.DataInputStream in) throws IOException {
        return read((DataInput) in);
    }

    /**
     * An uncompressed NBT file, as 1.12.2 wrote it with {@link #write(NBTTagCompound, File)} and {@link #safeWrite}; a gzipped one
     * (what earlier versions of this layer wrote) is read too.
     */
    public static NBTTagCompound read(File file) throws IOException {
        if (!file.exists()) return null;
        try (InputStream raw = new java.io.BufferedInputStream(new FileInputStream(file))) {
            raw.mark(2);
            int b0 = raw.read(), b1 = raw.read();
            raw.reset();
            if (b0 == 0x1f && b1 == 0x8b) return readCompressed(raw);
            return read((DataInput) new java.io.DataInputStream(raw));
        }
    }

    public static void write(NBTTagCompound tag, File file) throws IOException {
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(new FileOutputStream(file))) {
            write(tag, (DataOutput) out);
        }
    }

    public static void safeWrite(NBTTagCompound tag, File file) throws IOException {
        File tmp = new File(file.getAbsolutePath() + "_tmp");
        if (tmp.exists()) tmp.delete();
        write(tag, tmp);
        if (file.exists()) file.delete();
        if (file.exists()) throw new IOException("Failed to delete " + file);
        tmp.renameTo(file);
    }
}
