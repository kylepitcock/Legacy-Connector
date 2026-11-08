package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentData;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;

/** 1.12.2 enchanted book: the static helpers mods use to read and write the {@code StoredEnchantments} list. */
public class ItemEnchantedBook extends Item {
    public ItemEnchantedBook() {
        setMaxStackSize(1);
    }

    /** The stored enchantments list (compounds of {@code id} and {@code lvl}); empty when the stack has none. */
    public static NBTTagList getEnchantments(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? tag.getTagList("StoredEnchantments", 10) : new NBTTagList();
    }

    /** Stores the enchantment on the book; the same enchantment already stored is raised to the higher level. */
    public static void addEnchantment(ItemStack book, EnchantmentData data) {
        NBTTagList list = getEnchantments(book);
        int id = Enchantment.getEnchantmentID(data.enchantment);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            if (e.getShort("id") == id) {
                if (e.getShort("lvl") < data.enchantmentLevel) e.setShort("lvl", (short) data.enchantmentLevel);
                book.setTagInfo("StoredEnchantments", list);
                return;
            }
        }
        NBTTagCompound e = new NBTTagCompound();
        e.setShort("id", (short) id);
        e.setShort("lvl", (short) data.enchantmentLevel);
        list.appendTag(e);
        book.setTagInfo("StoredEnchantments", list);
    }

    /** A book storing the enchantment. */
    public static ItemStack getEnchantedItemStack(EnchantmentData data) {
        return legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.mcc$enchantedBook(data);
    }
}
