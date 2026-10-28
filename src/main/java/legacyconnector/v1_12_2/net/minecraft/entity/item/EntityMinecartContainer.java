package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 minecart with an inventory (chest and hopper carts): its slots are the modern cart's (a mod's own cart keeps its own), a loot
 * table fills it on first open, and it drops its contents when broken. */
public abstract class EntityMinecartContainer extends EntityMinecart implements legacyconnector.v1_12_2.net.minecraft.inventory.IInventory {
    public EntityMinecartContainer(World world) {
        super(world);
    }

    public EntityMinecartContainer(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected EntityMinecartContainer(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> mcc$items;
    @Deprecated
    public boolean dropContentsWhenDead = true;
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation lootTable;
    @Deprecated
    public long lootTableSeed;

    private net.minecraft.world.entity.vehicle.AbstractMinecartContainer mcc$container() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.vehicle.AbstractMinecartContainer c ? c : null;
    }

    private legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> mcc$items() {
        if (mcc$items == null) mcc$items = legacyconnector.v1_12_2.net.minecraft.util.NonNullList.withSize(getSizeInventory(), legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY);
        return mcc$items;
    }

    public static legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation mcc$get$lootTable(EntityMinecartContainer c) {
        return c.lootTable2();
    }

    private legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation lootTable2() {
        var c = mcc$container();
        if (c != null) {
            var key = c.getLootTable();
            return key == null ? null : new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(key.location().getNamespace(), key.location().getPath());
        }
        return mcc$table;
    }

    private legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation mcc$table;
    private long mcc$seed;

    public static void mcc$set$lootTable(EntityMinecartContainer c, legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation v) {
        c.setLootTable(v, mcc$get$lootTableSeed(c));
    }

    public static long mcc$get$lootTableSeed(EntityMinecartContainer c) {
        var m = c.mcc$container();
        return m != null ? m.getLootTableSeed() : c.mcc$seed;
    }

    public static void mcc$set$lootTableSeed(EntityMinecartContainer c, long v) {
        c.setLootTable(c.lootTable2(), v);
    }

    public void setLootTable(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation table, long seed) {
        mcc$table = table;
        mcc$seed = seed;
        var c = mcc$container();
        if (c != null) c.setLootTable(table == null ? null : net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                table.mcc$modern()), seed);
    }

    public abstract int getSizeInventory();

    public boolean isEmpty() {
        var c = mcc$container();
        if (c != null) return c.isEmpty();
        for (var s : mcc$items()) if (!s.isEmpty()) return false;
        return true;
    }

    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack getStackInSlot(int index) {
        var c = mcc$container();
        if (c != null) return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.view(c.getItem(index));
        return index >= 0 && index < mcc$items().size() ? mcc$items().get(index) : legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY;
    }

    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack decrStackSize(int index, int count) {
        var c = mcc$container();
        if (c != null) return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(c.removeItem(index, count));
        return legacyconnector.v1_12_2.net.minecraft.inventory.ItemStackHelper.getAndSplit(mcc$items(), index, count);
    }

    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack removeStackFromSlot(int index) {
        var c = mcc$container();
        if (c != null) return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(c.removeItemNoUpdate(index));
        var s = getStackInSlot(index);
        if (index >= 0 && index < mcc$items().size()) mcc$items().set(index, legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY);
        return s;
    }

    public void setInventorySlotContents(int index, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        var c = mcc$container();
        if (c != null) {
            c.setItem(index, org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModernLive(stack));
            return;
        }
        if (index >= 0 && index < mcc$items().size()) mcc$items().set(index, stack);
    }

    public int getInventoryStackLimit() {
        return 64;
    }

    public void markDirty() {
    }

    public boolean isUsableByPlayer(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return !mcc$get$isDead(this) && player.getDistanceSq(this) <= 64.0D;
    }

    public void openInventory(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
    }

    public void closeInventory(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
    }

    public boolean isItemValidForSlot(int index, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        return true;
    }

    public int getField(int id) {
        return 0;
    }

    public void setField(int id, int value) {
    }

    public int getFieldCount() {
        return 0;
    }

    public void clear() {
        var c = mcc$container();
        if (c != null) c.clearContent();
        else mcc$items().clear();
    }

    /** Opening a mod's container cart shows its slots as a chest-like GUI. */
    @Override
    public boolean processInitialInteract(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        if (!mcc$hosted()) return super.processInitialInteract(player, hand);
        if (mcc$entity != null && !mcc$entity.level().isClientSide() && this instanceof legacyconnector.v1_12_2.net.minecraft.inventory.IInventory inv) {
            player.displayGUIChest(inv);
        }
        return true;
    }

    @Override
    public void setDead() {
        if (mcc$hosted() && dropContentsWhenDead && mcc$entity != null && !mcc$entity.level().isClientSide()) {
            for (int i = 0; i < getSizeInventory(); ++i) {
                var s = getStackInSlot(i);
                if (!s.isEmpty()) entityDropItem(s.copy(), 0.0F);
            }
            clear();
        }
        super.setDead();
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$container() != null) return;
        if (mcc$table != null) {
            tag.setString("LootTable", mcc$table.toString());
            if (mcc$seed != 0L) tag.setLong("LootTableSeed", mcc$seed);
        } else {
            legacyconnector.v1_12_2.net.minecraft.inventory.ItemStackHelper.saveAllItems(tag, mcc$items());
        }
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$container() != null) return;
        mcc$items = null;
        if (tag.hasKey("LootTable", 8)) {
            mcc$table = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(tag.getString("LootTable"));
            mcc$seed = tag.getLong("LootTableSeed");
        } else {
            legacyconnector.v1_12_2.net.minecraft.inventory.ItemStackHelper.loadAllItems(tag, mcc$items());
        }
    }
}
