package mccompat.v1_7_10.net.minecraft.item;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.item.ItemEnchantedBook}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class ItemEnchantedBook extends mccompat.v1_7_10.net.minecraft.item.Item {
    public ItemEnchantedBook(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected ItemEnchantedBook() {
        super();
    }

    public void addEnchantment(ItemStack stack, mccompat.v1_7_10.net.minecraft.enchantment.EnchantmentData data) {
        mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound();
            stack.setTagCompound(tag);
        }
        mccompat.v1_7_10.net.minecraft.nbt.NBTTagList list = tag.hasKey("StoredEnchantments", 9)
                ? tag.getTagList("StoredEnchantments", 10) : new mccompat.v1_7_10.net.minecraft.nbt.NBTTagList();
        for (int i = 0; i < list.tagCount(); i++) {
            mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound e = list.getCompoundTagAt(i);
            if (e.getShort("id") == data.enchantmentobj.effectId) {
                if (e.getShort("lvl") < data.enchantmentLevel) e.setShort("lvl", (short) data.enchantmentLevel);
                tag.setTag("StoredEnchantments", list);
                stack.setTagCompound(tag);
                return;
            }
        }
        mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound e = new mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound();
        e.setShort("id", (short) data.enchantmentobj.effectId);
        e.setShort("lvl", (short) data.enchantmentLevel);
        list.appendTag(e);
        tag.setTag("StoredEnchantments", list);
        stack.setTagCompound(tag);
    }

    public ItemStack getEnchantedItemStack(mccompat.v1_7_10.net.minecraft.enchantment.EnchantmentData data) {
        ItemStack stack = new ItemStack(this);
        addEnchantment(stack, data);
        return stack;
    }
}
