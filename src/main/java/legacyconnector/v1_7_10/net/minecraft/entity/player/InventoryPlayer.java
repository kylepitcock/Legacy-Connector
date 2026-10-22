package legacyconnector.v1_7_10.net.minecraft.entity.player;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import net.minecraft.world.entity.player.Inventory;

/**
 * View over the modern player {@link Inventory}. Slot layout is identical (36 main + 4 armor in 1.7.10; modern
 * adds the offhand as slot 40, which legacy code never addresses). {@code mainInventory}/{@code armorInventory}
 * are snapshot arrays of live stack views: element reads and stack mutations work, assigning array elements
 * does not (use {@link #setInventorySlotContents}).
 */
public class InventoryPlayer implements IInventory {
    @Deprecated
    public ItemStack[] mainInventory;
    @Deprecated
    public ItemStack[] armorInventory;
    @Deprecated
    public int currentItem;
    public EntityPlayer player;
    public boolean inventoryChanged;

    private final Inventory mcc$inv;

    public InventoryPlayer(EntityPlayer player) {
        this.player = player;
        this.mcc$inv = player.mcc$player().getInventory();
    }

    public static ItemStack[] mcc$get$mainInventory(InventoryPlayer inv) {
        ItemStack[] out = new ItemStack[36];
        for (int i = 0; i < 36; i++) out[i] = ItemStack.mcc$wrap(inv.mcc$inv.items.get(i));
        return out;
    }

    public static ItemStack[] mcc$get$armorInventory(InventoryPlayer inv) {
        ItemStack[] out = new ItemStack[4];
        for (int i = 0; i < 4; i++) out[i] = ItemStack.mcc$wrap(inv.mcc$inv.armor.get(i));
        return out;
    }

    public static int mcc$get$currentItem(InventoryPlayer inv) {
        return inv.mcc$inv.selected;
    }

    public static void mcc$set$currentItem(InventoryPlayer inv, int slot) {
        inv.mcc$inv.selected = slot;
    }

    public ItemStack getCurrentItem() {
        return ItemStack.mcc$wrap(mcc$inv.getSelected());
    }

    public static int getHotbarSize() {
        return 9;
    }

    public int getFirstEmptyStack() {
        return mcc$inv.getFreeSlot();
    }

    public int getInventorySlotContainItem(Item item) {
        for (int i = 0; i < 36; i++) {
            ItemStack s = ItemStack.mcc$wrap(mcc$inv.items.get(i));
            if (s != null && s.getItem() == item) return i;
        }
        return -1;
    }

    public boolean consumeInventoryItem(Item item) {
        int slot = getInventorySlotContainItem(item);
        if (slot < 0) return false;
        mcc$inv.items.get(slot).shrink(1);
        return true;
    }

    public boolean hasItem(Item item) {
        return getInventorySlotContainItem(item) >= 0;
    }

    public boolean hasItemStack(ItemStack stack) {
        return stack != null && mcc$inv.contains(stack.mcc$modern());
    }

    public boolean addItemStackToInventory(ItemStack stack) {
        if (stack == null) return false;
        return mcc$inv.add(stack.mcc$modern());
    }

    public boolean func_146025_b(Block block) {
        return true;
    }

    public ItemStack armorItemInSlot(int slot) {
        return ItemStack.mcc$wrap(mcc$inv.armor.get(slot));
    }

    public int getTotalArmorValue() {
        return player.mcc$player().getArmorValue();
    }

    public void dropAllItems() {
        mcc$inv.dropAll();
    }

    public void decrementAnimations() {
    }

    public void changeCurrentItem(int direction) {
        mcc$inv.swapPaint(direction);
    }

    public ItemStack getItemStack() {
        return ItemStack.mcc$wrap(player.mcc$player().containerMenu.getCarried());
    }

    public void setItemStack(ItemStack stack) {
        player.mcc$player().containerMenu.setCarried(ItemStack.mcc$toModern(stack));
    }

    public NBTTagList writeToNBT(NBTTagList list) {
        mcc$inv.save(list.mcc$modern());
        return list;
    }

    public void readFromNBT(NBTTagList list) {
        mcc$inv.load(list.mcc$modern());
    }

    public void copyInventory(InventoryPlayer other) {
        mcc$inv.replaceWith(other.mcc$inv);
    }

    // ------------------------------------------------------------------ IInventory

    @Override
    public int getSizeInventory() {
        return 40;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.mcc$wrap(mcc$inv.getItem(slot));
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        return ItemStack.mcc$wrap(mcc$inv.removeItem(slot, amount));
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return ItemStack.mcc$wrap(mcc$inv.removeItemNoUpdate(slot));
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        mcc$inv.setItem(slot, ItemStack.mcc$toModern(stack));
    }

    @Override
    public String getInventoryName() {
        return "container.inventory";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return mcc$inv.getMaxStackSize();
    }

    @Override
    public void markDirty() {
        mcc$inv.setChanged();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer p) {
        return mcc$inv.stillValid(p.mcc$player());
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }
}
