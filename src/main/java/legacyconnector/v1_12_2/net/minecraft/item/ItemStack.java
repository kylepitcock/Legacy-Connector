package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** 1.12.2 {@code ItemStack}: an item, a count, a damage/metadata value and a tag. Converted to the modern stack at the boundaries. */
public final class ItemStack {
    public static final ItemStack EMPTY = new ItemStack((Item) null);

    private Item item;
    private int stackSize;
    private int itemDamage;
    private NBTTagCompound stackTagCompound;

    public ItemStack(Item item) {
        this(item, 1);
    }

    public ItemStack(Item item, int amount) {
        this(item, amount, 0);
    }

    public ItemStack(Item item, int amount, int meta) {
        this.item = item;
        this.stackSize = item == null ? 0 : amount;
        this.itemDamage = meta;
    }

    public ItemStack(legacyconnector.v1_12_2.net.minecraft.block.Block block) {
        this(block, 1);
    }

    public ItemStack(legacyconnector.v1_12_2.net.minecraft.block.Block block, int amount) {
        this(block, amount, 0);
    }

    public ItemStack(legacyconnector.v1_12_2.net.minecraft.block.Block block, int amount, int meta) {
        this(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.itemOf(block), amount, meta);
    }

    public ItemStack(NBTTagCompound tag) {
        this((Item) null);
        if (tag != null && tag.hasKey("id")) {
            var found = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ITEMS.getValue(
                    new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(tag.getString("id")));
            if (found != null) {
                this.item = found;
                this.stackSize = tag.getByte("Count");
                this.itemDamage = Math.max(0, tag.getShort("Damage"));
                if (tag.hasKey("tag", 10)) this.stackTagCompound = tag.getCompoundTag("tag");
            }
        }
    }

    public boolean isEmpty() {
        return this == EMPTY || item == null || stackSize <= 0;
    }

    /** The item of the stack; an empty stack has the air item (mods read the registry name of any stack's item). */
    public Item getItem() {
        return item != null ? item : legacyconnector.v1_12_2.net.minecraft.init.Items.AIR;
    }

    public int getCount() {
        return isEmpty() ? 0 : stackSize;
    }

    public void setCount(int count) {
        this.stackSize = count;
    }

    public void grow(int count) {
        this.stackSize += count;
    }

    public void shrink(int count) {
        this.stackSize -= count;
    }

    public int getMetadata() {
        return itemDamage;
    }

    public int getItemDamage() {
        return itemDamage;
    }

    public void setItemDamage(int damage) {
        this.itemDamage = damage;
    }

    public int getMaxStackSize() {
        return item == null ? 64 : item.getItemStackLimit(this);
    }

    public int getMaxDamage() {
        return item == null ? 0 : item.getMaxDamage(this);
    }

    public boolean isItemStackDamageable() {
        return item != null && item.getMaxDamage(this) > 0;
    }

    public boolean isStackable() {
        return getMaxStackSize() > 1 && (!isItemStackDamageable() || !isItemDamaged());
    }

    public boolean isItemDamaged() {
        return isItemStackDamageable() && itemDamage > 0;
    }

    public boolean hasTagCompound() {
        return stackTagCompound != null;
    }

    public NBTTagCompound getTagCompound() {
        return stackTagCompound;
    }

    public void setTagCompound(NBTTagCompound tag) {
        this.stackTagCompound = tag;
    }

    public void setTagInfo(String key, legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase value) {
        if (stackTagCompound == null) stackTagCompound = new NBTTagCompound();
        stackTagCompound.setTag(key, value);
    }

    /** The display name as a translation key (display.LocName), translated where it is shown. */
    public ItemStack setTranslatableName(String key) {
        getOrCreateSubCompound("display").setString("LocName", key);
        return this;
    }

    /** The {@code ench} list: compounds of a numeric enchantment id and a level. */
    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList getEnchantmentTagList() {
        return stackTagCompound != null ? stackTagCompound.getTagList("ench", 10) : new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList();
    }

    public void addEnchantment(legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment enchantment, int level) {
        if (stackTagCompound == null) stackTagCompound = new NBTTagCompound();
        if (!stackTagCompound.hasKey("ench", 9)) stackTagCompound.setTag("ench", new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList());
        legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList list = stackTagCompound.getTagList("ench", 10);
        NBTTagCompound e = new NBTTagCompound();
        e.setShort("id", (short) legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment.getEnchantmentID(enchantment));
        e.setShort("lvl", (short) (byte) level);
        list.appendTag(e);
    }

    public NBTTagCompound getOrCreateSubCompound(String key) {
        if (stackTagCompound == null) stackTagCompound = new NBTTagCompound();
        if (!stackTagCompound.hasKey(key, 10)) stackTagCompound.setTag(key, new NBTTagCompound());
        return stackTagCompound.getCompoundTag(key);
    }

    public ItemStack copy() {
        if (isEmpty()) return EMPTY;
        ItemStack copy = new ItemStack(item, stackSize, itemDamage);
        if (stackTagCompound != null) copy.stackTagCompound = (NBTTagCompound) stackTagCompound.copy();
        return copy;
    }

    public ItemStack splitStack(int amount) {
        int n = Math.min(amount, stackSize);
        ItemStack out = copy();
        out.setCount(n);
        shrink(n);
        return out;
    }

    public boolean isItemEqual(ItemStack other) {
        return !other.isEmpty() && item == other.item && itemDamage == other.itemDamage;
    }

    public static boolean areItemsEqual(ItemStack a, ItemStack b) {
        return a.isEmpty() ? b.isEmpty() : !b.isEmpty() && a.isItemEqual(b);
    }

    public static boolean areItemStackTagsEqual(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return a.stackTagCompound == null ? b.stackTagCompound == null : a.stackTagCompound.equals(b.stackTagCompound);
    }

    public static boolean areItemStacksEqual(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        return !a.isEmpty() && !b.isEmpty() && a.stackSize == b.stackSize && a.item == b.item && a.itemDamage == b.itemDamage && areItemStackTagsEqual(a, b);
    }

    public String getUnlocalizedName() {
        return item == null ? "" : item.getUnlocalizedName(this);
    }

    public String getTranslationKey() {
        return item == null ? "" : item.getTranslationKey(this);
    }

    public boolean getHasSubtypes() {
        return getItem().getHasSubtypes();
    }

    /** Whether the stack carries a non-empty {@code ench} list. */
    public boolean isItemEnchanted() {
        return stackTagCompound != null && stackTagCompound.hasKey("ench", 9) && stackTagCompound.getTagList("ench", 10).tagCount() > 0;
    }

    public String getDisplayName() {
        if (item == null) return "";
        String n = item.getItemStackDisplayName(this);
        return n != null ? n : getUnlocalizedName();
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        var name = item == null ? null : item.getRegistryName();
        tag.setString("id", name == null ? "minecraft:air" : name.toString());
        tag.setByte("Count", (byte) stackSize);
        tag.setShort("Damage", (short) itemDamage);
        if (stackTagCompound != null) tag.setTag("tag", stackTagCompound);
        return tag;
    }

    @Override
    public String toString() {
        return stackSize + "x" + (item == null ? "air" : item.getUnlocalizedName()) + "@" + itemDamage;
    }

    public NBTTagCompound serializeNBT() {
        return writeToNBT(new NBTTagCompound());
    }

    public void deserializeNBT(NBTTagCompound tag) {
        ItemStack parsed = new ItemStack(tag);
        this.item = parsed.item;
        this.stackSize = parsed.stackSize;
        this.itemDamage = parsed.itemDamage;
        this.stackTagCompound = parsed.stackTagCompound;
    }
}
