package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;

/** Beta 1.7.3 {@code ItemStack} ({@code iz}): item id, count and damage (the subtype for items with subtypes); no NBT. */
public class ItemStack {
    public int count;
    public int bobbingAnimationTime;
    public int itemId;
    private int damage;
    private net.minecraft.world.item.ItemStack mcc$nativeSnapshot;
    private int mcc$nativeId;

    /** Native component patches are bridge state, never a replacement for Beta's mutable id/count/damage. */
    public net.minecraft.world.item.ItemStack mcc$nativeSnapshot() {
        if (mcc$nativeId != itemId) mcc$nativeSnapshot = null;
        return mcc$nativeSnapshot;
    }
    public void mcc$nativeSnapshot(net.minecraft.world.item.ItemStack snapshot) {
        mcc$nativeSnapshot = snapshot == null || snapshot.isEmpty() ? null : snapshot.copy();
        mcc$nativeId = itemId;
    }

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
        org.kylepitcock.legacyconnector.vb1_7_3.StacksB173.writeNative(this, nbt);
        return nbt;
    }

    /** Original vanilla removal notification is empty; subclasses retain virtual override behavior. */
    public void onRemoved(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player){}

    public void readNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound nbt) {
        itemId = nbt.getShort("id");
        count = nbt.getByte("Count");
        damage = nbt.getShort("Damage");
        mcc$nativeSnapshot = null;
        org.kylepitcock.legacyconnector.vb1_7_3.StacksB173.readNative(this, nbt);
    }

    public ItemStack split(int amount) {
        this.count -= amount;
        ItemStack split = new ItemStack(itemId, amount, damage);
        split.mcc$nativeSnapshot(mcc$nativeSnapshot());
        return split;
    }

    public Item getItem() {
        return itemId >= 0 && itemId < Item.ITEMS.length ? Item.ITEMS[itemId] : null;
    }

    /** MCP {@code onCrafting}. */
    public void onCraft(legacyconnector.vb1_7_3.net.minecraft.world.World world, legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player) {
        Item item = getItem();
        if (item != null) item.onCraft(this, world, player);
    }

    public void useOnEntity(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity target) {
        Item item = getItem();
        if (item != null) item.useOnEntity(this, target);
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

    /** Beta breaks after exceeding the maximum, not on reaching it. Subtype-only items do not wear. */
    public void damage(int amount, legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity) {
        if (!isDamageable() || amount <= 0 || count <= 0) return;
        if (entity != null && entity.mcc$modern instanceof net.minecraft.world.entity.player.Player p && p.getAbilities().instabuild) return;
        long next = (long) damage + amount;
        if (next > getMaxDamage()) {
            count = Math.max(0, count - 1);
            damage = 0;
            if (entity != null && entity.mcc$modern instanceof net.minecraft.world.entity.LivingEntity living)
                living.onEquippedItemBroken(living.getMainHandItem().getItem(), net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        } else damage = (int) next;
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
        ItemStack copy = new ItemStack(itemId, count, damage);
        copy.mcc$nativeSnapshot(mcc$nativeSnapshot());
        return copy;
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
