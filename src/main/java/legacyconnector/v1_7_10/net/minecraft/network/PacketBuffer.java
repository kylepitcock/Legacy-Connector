package legacyconnector.v1_7_10.net.minecraft.network;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.CompressedStreamTools;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.FriendlyByteBuf;

/** 1.7.10's packet buffer: a ByteBuf with the vanilla helpers for var ints, strings, NBT and stacks. */
public class PacketBuffer extends FriendlyByteBuf {
    public PacketBuffer(ByteBuf wrapped) {
        super(wrapped);
    }

    public static int getVarIntSize(int value) {
        for (int i = 1; i < 5; i++) if ((value & -1 << i * 7) == 0) return i;
        return 5;
    }

    public int readVarIntFromBuffer() {
        return readVarInt();
    }

    public void writeVarIntToBuffer(int value) {
        writeVarInt(value);
    }

    public void writeStringToBuffer(String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 32767) throw new IOException("String too big (was " + text.length() + " bytes encoded, max 32767)");
        writeVarInt(bytes.length);
        writeBytes(bytes);
    }

    public String readStringFromBuffer(int maxLength) throws IOException {
        int length = readVarInt();
        if (length > maxLength * 4) throw new IOException("The received encoded string buffer length is longer than maximum allowed (" + length + " > " + maxLength * 4 + ")");
        if (length < 0) throw new IOException("The received encoded string buffer length is less than zero! Weird string!");
        byte[] bytes = new byte[length];
        readBytes(bytes);
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.length() > maxLength) throw new IOException("The received string length is longer than maximum allowed (" + length + " > " + maxLength + ")");
        return text;
    }

    public void writeNBTTagCompoundToBuffer(NBTTagCompound tag) throws IOException {
        if (tag == null) {
            writeShort(-1);
        } else {
            byte[] data = CompressedStreamTools.compress(tag);
            writeShort((short) data.length);
            writeBytes(data);
        }
    }

    public NBTTagCompound readNBTTagCompoundFromBuffer() throws IOException {
        short length = readShort();
        if (length < 0) return null;
        byte[] data = new byte[length];
        readBytes(data);
        return CompressedStreamTools.decompress(data);
    }

    public void writeItemStackToBuffer(ItemStack stack) throws IOException {
        if (stack == null) {
            writeShort(-1);
        } else {
            writeShort(Item.getIdFromItem(stack.getItem()));
            writeByte(stack.mcc$count());
            writeShort(stack.getItemDamage());
            writeNBTTagCompoundToBuffer(stack.getTagCompound());
        }
    }

    public ItemStack readItemStackFromBuffer() throws IOException {
        short id = readShort();
        if (id < 0) return null;
        int count = readByte();
        int damage = readShort();
        ItemStack stack = new ItemStack(Item.getItemById(id), count, damage);
        stack.setTagCompound(readNBTTagCompoundFromBuffer());
        return stack;
    }
}
