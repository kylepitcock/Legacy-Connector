package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.entity.LiveStacks1122;

/**
 * A view of the modern player inventory. The slot layout is the same (0-35 main with 0-8 the hotbar, 36-39 armor from the boots up,
 * 40 the off hand). The stacks handed out are live ({@link LiveStacks1122}): changing one changes the slot, and the
 * {@code mainInventory}/{@code armorInventory}/{@code offHandInventory} lists read and write the slots.
 */
public class InventoryPlayer implements IInventory {
    @Deprecated
    public NonNullList<ItemStack> mainInventory;
    @Deprecated
    public NonNullList<ItemStack> armorInventory;
    @Deprecated
    public NonNullList<ItemStack> offHandInventory;
    @Deprecated
    public int currentItem;
    /** Phantom: redirected to {@link #mcc$get$allInventories}. */
    @Deprecated
    public java.util.List<NonNullList<ItemStack>> allInventories;
    public EntityPlayer player;
    private NonNullList<ItemStack> mcc$main;
    private NonNullList<ItemStack> mcc$armor;
    private NonNullList<ItemStack> mcc$offhand;

    public InventoryPlayer(EntityPlayer player) {
        this.player = player;
    }

    private Inventory inv() {
        return player.mcc$player().getInventory();
    }

    private LiveStacks1122.Slot slot(int index) {
        Inventory inv = inv();
        return new LiveStacks1122.Slot() {
            @Override
            public net.minecraft.world.item.ItemStack get() {
                return inv.getItem(index);
            }

            @Override
            public void set(net.minecraft.world.item.ItemStack stack) {
                inv.setItem(index, stack);
            }
        };
    }

    private ItemStack view(int index) {
        return LiveStacks1122.view(player.mcc$entity, "inv:" + index, slot(index));
    }

    private void put(int index, ItemStack stack) {
        LiveStacks1122.put(player.mcc$entity, "inv:" + index, slot(index), stack == null ? ItemStack.EMPTY : stack);
    }

    /** A list over a range of the inventory's slots. */
    private final class SlotList extends java.util.AbstractList<ItemStack> {
        private final int start;
        private final int size;

        SlotList(int start, int size) {
            this.start = start;
            this.size = size;
        }

        @Override
        public ItemStack get(int index) {
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
            return view(start + index);
        }

        @Override
        public ItemStack set(int index, ItemStack element) {
            ItemStack old = get(index);
            put(start + index, element);
            return old;
        }

        @Override
        public int size() {
            return size;
        }
    }

    private static final class LiveList extends NonNullList<ItemStack> {
        LiveList(java.util.List<ItemStack> delegate) {
            super(delegate, ItemStack.EMPTY);
        }
    }

    public static NonNullList<ItemStack> mcc$get$mainInventory(InventoryPlayer inv) {
        if (inv.mcc$main == null) inv.mcc$main = new LiveList(inv.new SlotList(0, 36));
        return inv.mcc$main;
    }

    public static NonNullList<ItemStack> mcc$get$armorInventory(InventoryPlayer inv) {
        if (inv.mcc$armor == null) inv.mcc$armor = new LiveList(inv.new SlotList(36, 4));
        return inv.mcc$armor;
    }

    public static NonNullList<ItemStack> mcc$get$offHandInventory(InventoryPlayer inv) {
        if (inv.mcc$offhand == null) inv.mcc$offhand = new LiveList(inv.new SlotList(40, 1));
        return inv.mcc$offhand;
    }

    public static java.util.List<NonNullList<ItemStack>> mcc$get$allInventories(InventoryPlayer inv) {
        return java.util.List.of(mcc$get$mainInventory(inv), mcc$get$armorInventory(inv), mcc$get$offHandInventory(inv));
    }

    public void damageArmor(float damage) {
        var p = player == null ? null : player.mcc$player();
        if (p == null || damage <= 0) return;
        // as 1.12.2: a quarter of the damage, at least 1, to every damageable armour piece
        int amount = Math.max(1, (int) (damage / 4.0F));
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.HEAD}) {
            var stack = p.getItemBySlot(slot);
            if (stack.isDamageableItem()) stack.hurtAndBreak(amount, p, slot);
        }
    }

    public static int mcc$get$currentItem(InventoryPlayer inv) {
        return inv.inv().selected;
    }

    public static void mcc$set$currentItem(InventoryPlayer inv, int slot) {
        if (Inventory.isHotbarSlot(slot)) inv.inv().selected = slot;
    }

    public ItemStack getCurrentItem() {
        int sel = inv().selected;
        return isHotbar(sel) ? view(sel) : ItemStack.EMPTY;
    }

    public static int getHotbarSize() {
        return 9;
    }

    public static boolean isHotbar(int index) {
        return index >= 0 && index < 9;
    }

    public int getFirstEmptyStack() {
        return inv().getFreeSlot();
    }

    public int getSlotFor(ItemStack stack) {
        return stack == null ? -1 : inv().findSlotMatchingItem(Stacks1122.toModern(stack));
    }

    public int findSlotMatchingUnusedItem(ItemStack stack) {
        return stack == null ? -1 : inv().findSlotMatchingUnusedItem(Stacks1122.toModern(stack));
    }

    public int getBestHotbarSlot() {
        return inv().getSuitableHotbarSlot();
    }

    public void setPickedItemStack(ItemStack stack) {
        inv().setPickedItem(Stacks1122.toModern(stack));
    }

    public void pickItem(int index) {
        inv().pickSlot(index);
    }

    public boolean canMergeStacks(ItemStack a, ItemStack b) {
        return !a.isEmpty() && ItemStack.areItemsEqual(a, b) && ItemStack.areItemStackTagsEqual(a, b) && a.isStackable() && a.getCount() < a.getMaxStackSize()
                && a.getCount() < getInventoryStackLimit();
    }

    public boolean hasItemStack(ItemStack stack) {
        return stack != null && !stack.isEmpty() && inv().contains(Stacks1122.toModern(stack));
    }

    public boolean addItemStackToInventory(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var modern = Stacks1122.toModern(stack);
        boolean added = inv().add(modern);
        // vanilla shrank the passed stack by what fitted
        stack.setCount(modern.getCount());
        return added;
    }

    public void placeItemBackInInventory(legacyconnector.v1_12_2.net.minecraft.world.World world, ItemStack stack) {
        if (stack != null && !stack.isEmpty()) inv().placeItemBackInInventory(Stacks1122.toModern(stack));
    }

    public void deleteStack(ItemStack stack) {
        if (stack == null) return;
        Inventory inv = inv();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = view(i);
            if (s == stack) {
                put(i, ItemStack.EMPTY);
                return;
            }
        }
        inv.removeItem(Stacks1122.toModern(stack));
    }

    public ItemStack armorItemInSlot(int slot) {
        return slot >= 0 && slot < 4 ? view(36 + slot) : ItemStack.EMPTY;
    }

    public int clearMatchingItems(Item item, int meta, int removeCount, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        int removed = 0;
        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack s = view(i);
            if (s.isEmpty() || item != null && s.getItem() != item || meta > -1 && s.getMetadata() != meta) continue;
            int take = removeCount < 0 ? s.getCount() : Math.min(removeCount - removed, s.getCount());
            removed += take;
            if (removeCount != 0) {
                s.shrink(take);
                put(i, s.isEmpty() ? ItemStack.EMPTY : s);
            }
            if (removeCount > 0 && removed >= removeCount) break;
        }
        return removed;
    }

    public void dropAllItems() {
        inv().dropAll();
    }

    public void decrementAnimations() {
    }

    public void changeCurrentItem(int direction) {
        inv().swapPaint(direction);
    }

    /** The stack on the mouse cursor in the open container. */
    public ItemStack getItemStack() {
        var menu = player.mcc$player().containerMenu;
        return LiveStacks1122.view(player.mcc$entity, "carried", new LiveStacks1122.Slot() {
            @Override
            public net.minecraft.world.item.ItemStack get() {
                return menu.getCarried();
            }

            @Override
            public void set(net.minecraft.world.item.ItemStack stack) {
                menu.setCarried(stack);
            }
        });
    }

    public void setItemStack(ItemStack stack) {
        var menu = player.mcc$player().containerMenu;
        LiveStacks1122.put(player.mcc$entity, "carried", new LiveStacks1122.Slot() {
            @Override
            public net.minecraft.world.item.ItemStack get() {
                return menu.getCarried();
            }

            @Override
            public void set(net.minecraft.world.item.ItemStack s) {
                menu.setCarried(s);
            }
        }, stack == null ? ItemStack.EMPTY : stack);
    }

    public NBTTagList writeToNBT(NBTTagList list) {
        LiveStacks1122.flushAll();
        inv().save(list.mcc$modern());
        return list;
    }

    public void readFromNBT(NBTTagList list) {
        LiveStacks1122.flushAll();
        inv().load(list.mcc$modern());
    }

    public void copyInventory(InventoryPlayer other) {
        LiveStacks1122.flushAll();
        inv().replaceWith(other.inv());
    }

    public int getTimesChanged() {
        return inv().getTimesChanged();
    }

    public float getDestroySpeed(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return inv().getDestroySpeed(org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state));
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName() {
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation(getName());
    }

    /** The player's inventory as a Forge item handler (PlayerInvWrapper: 41 slots). */
    public legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandlerModifiable mcc$itemHandler(legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        InventoryPlayer self = this;
        return new legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandlerModifiable() {
            @Override
            public int getSlots() {
                return self.getSizeInventory();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return self.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
                ItemStack existing = self.getStackInSlot(slot);
                int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
                if (!existing.isEmpty()) {
                    if (!ItemStack.areItemsEqual(existing, stack) || !ItemStack.areItemStackTagsEqual(existing, stack)) return stack;
                    limit -= existing.getCount();
                }
                if (limit <= 0) return stack;
                boolean reachedLimit = stack.getCount() > limit;
                if (!simulate) {
                    if (existing.isEmpty()) {
                        ItemStack put = stack.copy();
                        put.setCount(reachedLimit ? limit : stack.getCount());
                        self.setInventorySlotContents(slot, put);
                    } else {
                        existing.grow(reachedLimit ? limit : stack.getCount());
                        self.setInventorySlotContents(slot, existing);
                    }
                }
                if (!reachedLimit) return ItemStack.EMPTY;
                ItemStack rest = stack.copy();
                rest.setCount(stack.getCount() - limit);
                return rest;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (amount <= 0) return ItemStack.EMPTY;
                ItemStack existing = self.getStackInSlot(slot);
                if (existing.isEmpty()) return ItemStack.EMPTY;
                int take = Math.min(amount, existing.getMaxStackSize());
                if (simulate) {
                    ItemStack out = existing.copy();
                    out.setCount(Math.min(take, existing.getCount()));
                    return out;
                }
                return self.decrStackSize(slot, take);
            }

            @Override
            public int getSlotLimit(int slot) {
                return self.getInventoryStackLimit();
            }

            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                self.setInventorySlotContents(slot, stack);
            }
        };
    }

    // ------------------------------------------------------------------ IInventory

    @Override
    public int getSizeInventory() {
        return inv().getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inv().isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index < 0 || index >= getSizeInventory() ? ItemStack.EMPTY : view(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        LiveStacks1122.flushAll();
        return Stacks1122.fromModern(inv().removeItem(index, count));
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        LiveStacks1122.flushAll();
        return Stacks1122.fromModern(inv().removeItemNoUpdate(index));
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index >= 0 && index < getSizeInventory()) put(index, stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return inv().getMaxStackSize();
    }

    @Override
    public void markDirty() {
        inv().setChanged();
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer p) {
        return p != null && inv().stillValid(p.mcc$player());
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
        inv().clearContent();
    }

    @Override
    public String getName() {
        return "container.inventory";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }
}
