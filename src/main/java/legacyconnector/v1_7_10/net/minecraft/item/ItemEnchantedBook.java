package legacyconnector.v1_7_10.net.minecraft.item;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.item.ItemEnchantedBook}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class ItemEnchantedBook extends legacyconnector.v1_7_10.net.minecraft.item.Item {
    public ItemEnchantedBook(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected ItemEnchantedBook() {
        super();
    }

    public void addEnchantment(ItemStack stack, legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentData data) {
        legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
            stack.setTagCompound(tag);
        }
        legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList list = tag.hasKey("StoredEnchantments", 9)
                ? tag.getTagList("StoredEnchantments", 10) : new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList();
        for (int i = 0; i < list.tagCount(); i++) {
            legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound e = list.getCompoundTagAt(i);
            if (e.getShort("id") == data.enchantmentobj.effectId) {
                if (e.getShort("lvl") < data.enchantmentLevel) e.setShort("lvl", (short) data.enchantmentLevel);
                tag.setTag("StoredEnchantments", list);
                stack.setTagCompound(tag);
                return;
            }
        }
        legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound e = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
        e.setShort("id", (short) data.enchantmentobj.effectId);
        e.setShort("lvl", (short) data.enchantmentLevel);
        list.appendTag(e);
        tag.setTag("StoredEnchantments", list);
        stack.setTagCompound(tag);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.syncEnchantments(stack, true);
    }

    public ItemStack getEnchantedItemStack(legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentData data) {
        ItemStack stack = new ItemStack(this);
        addEnchantment(stack, data);
        return stack;
    }

    /** {@code func_92113_a}: adds one enchanted book per level of an enchantment (minimum to maximum level) to a creative list. */
    public void func_92113_a(legacyconnector.v1_7_10.net.minecraft.enchantment.Enchantment enchantment, java.util.List list) {
        if (enchantment == null || list == null) return;
        for (int level = enchantment.getMinLevel(); level <= enchantment.getMaxLevel(); level++) {
            list.add(getEnchantedItemStack(new legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentData(enchantment, level)));
        }
    }

    public legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList func_92110_g(ItemStack stack) {
        legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey("StoredEnchantments", 9) ? tag.getTagList("StoredEnchantments", 10) : new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList();
    }

    /** {@code func_92114_b}: a random loot entry holding one enchanted book (level-30 enchantments), weight 1. */
    public legacyconnector.v1_7_10.net.minecraft.util.WeightedRandomChestContent func_92114_b(java.util.Random random) {
        return func_92112_a(random, 1, 1, 1);
    }

    /** {@code func_92112_a}: a loot entry holding a randomly enchanted book with the given stack range and weight. */
    public legacyconnector.v1_7_10.net.minecraft.util.WeightedRandomChestContent func_92112_a(java.util.Random random, int min, int max, int weight) {
        ItemStack book = new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.book, 1, 0);
        legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper.addRandomEnchantment(random, book, 30);
        return new legacyconnector.v1_7_10.net.minecraft.util.WeightedRandomChestContent(book, min, max, weight);
    }
}
