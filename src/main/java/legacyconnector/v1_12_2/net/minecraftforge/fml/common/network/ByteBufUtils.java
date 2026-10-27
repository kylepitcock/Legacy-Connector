package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;

/** FML 1.12.2's buffer helpers for custom messages: var ints, UTF-8 strings, tags and item stacks in the vanilla wire forms. */
public class ByteBufUtils {
    private ByteBufUtils() {}

    public static int varIntByteCount(int toCount) {
        return (toCount & 0xFFFFFF80) == 0 ? 1 : ((toCount & 0xFFFFC000) == 0 ? 2 : ((toCount & 0xFFE00000) == 0 ? 3 : ((toCount & 0xF0000000) == 0 ? 4 : 5)));
    }

    public static int readVarInt(ByteBuf buf, int maxSize) {
        if (maxSize <= 0 || maxSize > 5) throw new IllegalArgumentException("Varint length is between 1 and 5, not " + maxSize);
        int i = 0, j = 0;
        byte b0;
        do {
            b0 = buf.readByte();
            i |= (b0 & 127) << j++ * 7;
            if (j > maxSize) throw new RuntimeException("VarInt too big");
        } while ((b0 & 128) == 128);
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

    public static void writeVarShort(ByteBuf buf, int toWrite) {
        int low = toWrite & 0x7FFF;
        int high = (toWrite & 0x7F8000) >> 15;
        if (high != 0) low = low | 0x8000;
        buf.writeShort(low);
        if (high != 0) buf.writeByte(high);
    }

    public static void writeVarInt(ByteBuf to, int toWrite, int maxSize) {
        if (varIntByteCount(toWrite) > maxSize) throw new IllegalArgumentException("Integer is too big for " + maxSize + " bytes");
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
        byte[] utf8Bytes = string.getBytes(StandardCharsets.UTF_8);
        if (varIntByteCount(utf8Bytes.length) >= 3) throw new IllegalArgumentException("The string is too long for this encoding.");
        writeVarInt(to, utf8Bytes.length, 2);
        to.writeBytes(utf8Bytes);
    }

    public static void writeItemStack(ByteBuf to, ItemStack stack) {
        new PacketBuffer(to).writeItemStack(stack);
    }

    public static ItemStack readItemStack(ByteBuf from) {
        try {
            return new PacketBuffer(from).readItemStack();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeTag(ByteBuf to, NBTTagCompound tag) {
        new PacketBuffer(to).writeCompoundTag(tag);
    }

    public static NBTTagCompound readTag(ByteBuf from) {
        try {
            return new PacketBuffer(from).readCompoundTag();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** A registry entry by its registry name. */
    public static <T extends legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry<T>> void writeRegistryEntry(ByteBuf out, T entry) {
        writeUTF8String(out, String.valueOf(entry.getRegistryName()));
    }

    public static <T extends legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry<T>> T readRegistryEntry(ByteBuf in,
            legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistry<T> registry) {
        String name = readUTF8String(in);
        T entry = registry.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(name));
        if (entry == null) throw new IllegalArgumentException("Invalid or unknown registry entry " + name);
        return entry;
    }

    public static String getContentDump(ByteBuf buffer) {
        int currentLength = buffer.readableBytes();
        StringBuilder returnString = new StringBuilder((currentLength * 3) + (currentLength / 16 * 18) + 2);
        int i;
        for (i = 0; i < currentLength; i++) {
            if ((i != 0) && (i % 16 == 0)) returnString.append('\n');
            returnString.append(String.format("%02x ", buffer.getUnsignedByte(i + buffer.readerIndex())));
        }
        return returnString.toString();
    }
}
