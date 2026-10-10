package mccompat.v1_7_10.cpw.mods.fml.common.network;

import io.netty.buffer.ByteBuf;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

public class ByteBufUtils {
    public static int varIntByteCount(int toCount) {
        return (toCount & 0xFFFFFF80) == 0 ? 1 : (toCount & 0xFFFFC000) == 0 ? 2 : (toCount & 0xFFE00000) == 0 ? 3 : (toCount & 0xF0000000) == 0 ? 4 : 5;
    }

    public static int readVarInt(ByteBuf buf, int maxSize) {
        int i = 0, j = 0;
        byte b;
        do {
            b = buf.readByte();
            i |= (b & 127) << j++ * 7;
            if (j > maxSize) throw new RuntimeException("VarInt too big");
        } while ((b & 128) == 128);
        return i;
    }

    public static int readVarShort(ByteBuf buf) {
        int low = buf.readUnsignedShort();
        int high = 0;
        if ((low & 0x8000) != 0) {
            low = low & 0x7FFF;
            high = buf.readUnsignedByte();
        }
        return ((high & 0xFF) << 15) | low;
    }

    public static void writeVarShort(ByteBuf buf, int value) {
        int low = value & 0x7FFF;
        int high = (value & 0x7F8000) >> 15;
        if (high != 0) low = low | 0x8000;
        buf.writeShort(low);
        if (high != 0) buf.writeByte(high);
    }

    public static void writeVarInt(ByteBuf to, int toWrite, int maxSize) {
        while ((toWrite & -128) != 0) {
            to.writeByte(toWrite & 127 | 128);
            toWrite >>>= 7;
        }
        to.writeByte(toWrite);
    }

    public static String readUTF8String(ByteBuf from) {
        int len = readVarInt(from, 2);
        String str = from.toString(from.readerIndex(), len, StandardCharsets.UTF_8);
        from.readerIndex(from.readerIndex() + len);
        return str;
    }

    public static void writeUTF8String(ByteBuf to, String string) {
        byte[] utf8 = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(to, utf8.length, 2);
        to.writeBytes(utf8);
    }

    public static void writeItemStack(ByteBuf to, ItemStack stack) {
        NBTTagCompound tag = new NBTTagCompound();
        if (stack != null) stack.writeToNBT(tag);
        writeTag(to, stack == null ? null : tag);
    }

    public static ItemStack readItemStack(ByteBuf from) {
        NBTTagCompound tag = readTag(from);
        return tag == null ? null : ItemStack.loadItemStackFromNBT(tag);
    }

    public static void writeTag(ByteBuf to, NBTTagCompound tag) {
        if (tag == null) {
            to.writeShort(-1);
            return;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.write(tag.mcc$modern(), new DataOutputStream(bytes));
            to.writeShort(bytes.size());
            to.writeBytes(bytes.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static NBTTagCompound readTag(ByteBuf from) {
        short len = from.readShort();
        if (len < 0) return null;
        byte[] data = new byte[len];
        from.readBytes(data);
        try {
            CompoundTag tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(data)), NbtAccounter.create(2097152L));
            return new NBTTagCompound(tag);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
