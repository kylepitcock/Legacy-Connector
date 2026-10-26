package legacyconnector.v1_12_2.net.minecraft.enchantment;

import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;

/** The enchantments of a stack, read from and written to its {@code ench} list (an enchanted book's {@code StoredEnchantments}). */
public class EnchantmentHelper {
    private static boolean isBook(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.ENCHANTED_BOOK;
    }

    private static NBTTagList list(ItemStack stack) {
        if (isBook(stack)) {
            NBTTagCompound tag = stack.getTagCompound();
            return tag != null ? tag.getTagList("StoredEnchantments", 10) : new NBTTagList();
        }
        return stack.getEnchantmentTagList();
    }

    public static int getEnchantmentLevel(Enchantment enchantment, ItemStack stack) {
        if (stack.isEmpty() || enchantment == null) return 0;
        NBTTagList list = stack.getEnchantmentTagList();
        int id = Enchantment.getEnchantmentID(enchantment);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            if (e.getShort("id") == id) return e.getShort("lvl");
        }
        return 0;
    }

    public static Map<Enchantment, Integer> getEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> out = new LinkedHashMap<>();
        NBTTagList list = list(stack);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            Enchantment enchantment = Enchantment.getEnchantmentByID(e.getShort("id"));
            if (enchantment != null) out.put(enchantment, (int) e.getShort("lvl"));
        }
        return out;
    }

    public static void setEnchantments(Map<Enchantment, Integer> enchantments, ItemStack stack) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            if (entry.getKey() == null) continue;
            NBTTagCompound e = new NBTTagCompound();
            e.setShort("id", (short) Enchantment.getEnchantmentID(entry.getKey()));
            e.setShort("lvl", (short) (int) entry.getValue());
            list.appendTag(e);
        }
        if (list.tagCount() == 0) {
            if (stack.hasTagCompound()) stack.getTagCompound().removeTag(isBook(stack) ? "StoredEnchantments" : "ench");
        } else {
            stack.setTagInfo(isBook(stack) ? "StoredEnchantments" : "ench", list);
        }
    }

    /** {@code ItemEnchantedBook.getEnchantedItemStack}: an enchanted book storing the enchantment. */
    public static ItemStack mcc$enchantedBook(EnchantmentData data) {
        ItemStack book = new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.ENCHANTED_BOOK);
        Map<Enchantment, Integer> stored = new LinkedHashMap<>();
        stored.put(data.enchantment, data.enchantmentLevel);
        setEnchantments(stored, book);
        return book;
    }
}
