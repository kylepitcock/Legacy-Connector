package legacyconnector.v1_12_2.net.minecraft.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.ItemStackHelper;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * 1.12.2's container tile entity that can hold an unopened loot table (dungeon chests, mods' loot containers): the loot is rolled into
 * the inventory the first time it is opened or read, with the modern loot table of that name. The inventory methods work on
 * {@link #getItems}; the custom name comes from an anvil-renamed item.
 */
public abstract class TileEntityLockableLoot extends TileEntityLockable {
    protected ResourceLocation lootTable;
    protected long lootTableSeed;
    protected String customName;

    public TileEntityLockableLoot() {
    }

    protected abstract NonNullList<ItemStack> getItems();

    /** Reads the loot table instead of the items when one is set (the items are rolled later). */
    protected boolean checkLootAndRead(NBTTagCompound compound) {
        if (compound.hasKey("LootTable", 8)) {
            lootTable = new ResourceLocation(compound.getString("LootTable"));
            lootTableSeed = compound.getLong("LootTableSeed");
            return true;
        }
        return false;
    }

    protected boolean checkLootAndWrite(NBTTagCompound compound) {
        if (lootTable != null) {
            compound.setString("LootTable", lootTable.toString());
            if (lootTableSeed != 0L) compound.setLong("LootTableSeed", lootTableSeed);
            return true;
        }
        return false;
    }

    /** Rolls the loot table into the inventory (once; the table is cleared), luck and killer from the player when one opens it. */
    public void fillWithLoot(EntityPlayer player) {
        if (lootTable == null || world == null) return;
        var level = world.mcc$level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return;
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, lootTable.mcc$modern());
        var table = server.getServer().reloadableRegistries().getLootTable(key);
        long seed = lootTableSeed;
        lootTable = null;
        var builder = new net.minecraft.world.level.storage.loot.LootParams.Builder(server).withParameter(
                net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                net.minecraft.world.phys.Vec3.atCenterOf(new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ())));
        if (player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) {
            builder.withLuck(p.getLuck()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, p);
        }
        var params = builder.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        var items = seed == 0L ? table.getRandomItems(params) : table.getRandomItems(params, seed);
        Random rand = seed == 0L ? new Random() : new Random(seed);
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < getSizeInventory(); i++) if (getItems().get(i).isEmpty()) empty.add(i);
        Collections.shuffle(empty, rand);
        int next = 0;
        for (var stack : items) {
            if (next >= empty.size()) break;
            if (!stack.isEmpty()) getItems().set(empty.get(next++), Stacks1122.fromModern(stack));
        }
        markDirty();
    }

    public ResourceLocation getLootTable() {
        return lootTable;
    }

    public void setLootTable(ResourceLocation tableIn, long seed) {
        this.lootTable = tableIn;
        this.lootTableSeed = seed;
    }

    @Override
    public boolean hasCustomName() {
        return customName != null && !customName.isEmpty();
    }

    public void setCustomName(String name) {
        this.customName = name;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        fillWithLoot(null);
        return getItems().get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        fillWithLoot(null);
        ItemStack out = ItemStackHelper.getAndSplit(getItems(), index, count);
        if (!out.isEmpty()) markDirty();
        return out;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        fillWithLoot(null);
        return ItemStackHelper.getAndRemove(getItems(), index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        fillWithLoot(null);
        getItems().set(index, stack == null ? ItemStack.EMPTY : stack);
        if (stack != null && stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
        markDirty();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : getItems()) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        if (world == null || world.getTileEntity(pos) != this) return false;
        return player != null && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        fillWithLoot(null);
        getItems().clear();
    }
}
