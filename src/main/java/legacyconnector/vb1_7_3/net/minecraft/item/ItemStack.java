package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;

/** Beta 1.7.3 {@code ItemStack} ({@code iz}): item id, count and damage (the subtype for items with subtypes); no NBT. */
public class ItemStack {
    public int count;
    public int bobbingAnimationTime;
    public int itemId;
    private int damage;

    public ItemStack(Block block) {
        this(block, 1);
    }

    public ItemStack(Block block, int count) {
        this(block.id, count, 0);
    }

    public ItemStack(Block block, int count, int damage) {
        this(block.id, count, damage);
    }

    public ItemStack(Item item) {
        this(item.id, 1, 0);
    }

    public ItemStack(Item item, int count) {
        this(item.id, count, 0);
    }

    public ItemStack(Item item, int count, int damage) {
        this(item.id, count, damage);
    }

    public ItemStack(int itemId, int count, int damage) {
        this.itemId = itemId;
        this.count = count;
        this.damage = damage;
    }

    public ItemStack(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound nbt) {
        readNbt(nbt);
    }

    public legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound writeNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound nbt) {
        nbt.putShort("id", (short) itemId);
        nbt.putByte("Count", (byte) count);
        nbt.putShort("Damage", (short) damage);
        return nbt;
    }

    public void readNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound nbt) {
        itemId = nbt.getShort("id");
        count = nbt.getByte("Count");
        damage = nbt.getShort("Damage");
    }

    public ItemStack split(int amount) {
        this.count -= amount;
        return new ItemStack(itemId, amount, damage);
    }

    public Item getItem() {
        return itemId >= 0 && itemId < Item.ITEMS.length ? Item.ITEMS[itemId] : null;
    }

    /** MCP {@code onCrafting}. */
    public void onCraft(legacyconnector.vb1_7_3.net.minecraft.world.World world, legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player) {
        Item item = getItem();
        if (item != null) item.onCraft(this, world, player);
    }

    public int getTextureId() {
        Item item = getItem();
        return item == null ? 0 : item.getTextureId(this);
    }

    public int getMaxCount() {
        Item item = getItem();
        return item == null ? 64 : item.getMaxCount();
    }

    public boolean isStackable() {
        return getMaxCount() > 1 && (!isDamageable() || !isDamaged());
    }

    public boolean isDamageable() {
        Item item = getItem();
        return item != null && item.getMaxDamage() > 0;
    }

    public boolean hasSubtypes() {
        Item item = getItem();
        return item != null && item.hasSubtypes();
    }

    public boolean isDamaged() {
        return isDamageable() && damage > 0;
    }

    public int getDamage2() {
        return damage;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getMaxDamage() {
        Item item = getItem();
        return item == null ? 0 : item.getMaxDamage();
    }

    public boolean isSuitableFor(Block block) {
        Item item = getItem();
        return item != null && item.isSuitableFor(block);
    }

    public ItemStack copy() {
        return new ItemStack(itemId, count, damage);
    }

    public static boolean areEqual(ItemStack a, ItemStack b) {
        if (a == null && b == null) return true;
        return a != null && b != null && a.equals2(b);
    }

    private boolean equals2(ItemStack other) {
        return count == other.count && itemId == other.itemId && damage == other.damage;
    }

    public boolean isItemEqual(ItemStack other) {
        return itemId == other.itemId && damage == other.damage;
    }

    public String getTranslationKey() {
        Item item = getItem();
        return item == null ? "item.null" : item.getTranslationKey(this);
    }

    public static ItemStack clone(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    @Override
    public String toString() {
        Item item = getItem();
        return count + "x" + (item == null ? "null" : item.getTranslationKey()) + "@" + damage;
    }

    public boolean equals(ItemStack other) {
        return itemId == other.itemId && count == other.count && damage == other.damage;
    }
}
