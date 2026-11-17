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
        // vanilla accepts a null player (an AI proxy of a fake player builds a detached inventory before it has one)
        this.mcc$inv = player == null ? new Inventory(null) : player.mcc$player().getInventory();
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
        ItemStack held = ItemStack.mcc$wrap(mcc$inv.getSelected());
        var site = org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites.CURRENT_ITEM; // coremod hook site (no-op unless listed)
        return site.listed() && site.active() ? (ItemStack) site.modify(0, held, this) : held;
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

    /** Removes up to {@code count} (all when negative) of an item, ignoring the metadata when {@code meta} is negative; returns how many went. */
    public int clearInventory(legacyconnector.v1_7_10.net.minecraft.item.Item item, int meta) {
        int removed = 0;
        for (int i = 0; i < getSizeInventory(); i++) {
            legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack = getStackInSlot(i);
            if (stack == null) continue;
            if (item != null && stack.getItem() != item) continue;
            if (meta > -1 && stack.getItemDamage() != meta) continue;
            removed += legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$get$stackSize(stack);
            setInventorySlotContents(i, null);
        }
        return removed;
    }

    /** The slot of the first stack of the item with exactly that damage value (metadata), or -1. */
    public int getInventorySlotContainItemAndDamage(Item item, int meta) {
        for (int i = 0; i < 36; i++) {
            ItemStack s = ItemStack.mcc$wrap(mcc$inv.items.get(i));
            if (s != null && s.getItem() == item && s.getItemDamage() == meta) return i;
        }
        return -1;
    }

    /**
     * Picks the item for the player's hand (pick block): selects its hotbar slot, and in creative puts a new stack of it into the first
     * empty hotbar slot (or the selected one) when none is there.
     */
    public void setCurrentItem(Item item, int meta, boolean useMeta, boolean creative) {
        int slot = useMeta ? getInventorySlotContainItemAndDamage(item, meta) : getInventorySlotContainItem(item);
        if (slot >= 0 && slot < 9) {
            mcc$inv.selected = slot;
        } else if (creative && item != null) {
            int empty = getFirstEmptyStack();
            if (empty >= 0 && empty < 9) mcc$inv.selected = empty;
            mcc$inv.items.set(mcc$inv.selected, new ItemStack(item, 1, useMeta ? meta : 0).mcc$modern());
        }
    }

    /** Damages every worn piece of armour by a quarter of the damage taken (at least 1). */
    public void damageArmor(float damage) {
        damage /= 4.0F;
        if (damage < 1.0F) damage = 1.0F;
        for (int i = 0; i < 4; i++) {
            ItemStack piece = armorItemInSlot(i);
            if (piece != null && piece.getItem() instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemArmor) {
                piece.damageItem((int) damage, player);
            }
        }
    }

    /** How fast the held item mines the block (1 with an empty hand). */
    public float getStrVsBlock(Block block) {
        ItemStack held = getCurrentItem();
        return held == null ? 1.0F : held.getStrVsBlock(block);
    }
}
