package legacyconnector.v1_12_2.net.minecraft.inventory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122;

/**
 * 1.12.2 container: the slots of an open GUI and their server-side logic. Hosted by a modern menu ({@code LegacyMenu1122}), which
 * clicks and syncs slots with modern code and calls back here for {@link #transferStackInSlot}, {@link #canInteractWith},
 * {@link #onContainerClosed}, {@link #detectAndSendChanges} (window properties) and {@link #enchantItem}. A container without a host
 * (a mod driving one directly) clicks with the verbatim 1.12.2 algorithm; {@link #mergeItemStack} is verbatim too.
 */
public abstract class Container {
    public NonNullList<ItemStack> inventoryItemStacks = NonNullList.create();
    public List<Slot> inventorySlots = Lists.newArrayList();
    public int windowId;
    private short transactionID;
    private int dragMode = -1;
    private int dragEvent;
    private final Set<Slot> dragSlots = Sets.newHashSet();
    protected List<IContainerListener> listeners = Lists.newArrayList();
    private final Set<EntityPlayer> playerList = Sets.newHashSet();
    /** The modern menu hosting this container (set by the menu bridge), or null. */
    public Object mcc$menu;

    public Container() {
    }

    protected Slot addSlotToContainer(Slot slot) {
        slot.slotNumber = inventorySlots.size();
        inventorySlots.add(slot);
        inventoryItemStacks.add(ItemStack.EMPTY);
        return slot;
    }

    public void addListener(IContainerListener listener) {
        if (listeners.contains(listener)) throw new IllegalArgumentException("Listener already listening");
        listeners.add(listener);
        listener.sendAllContents(this, getInventory());
        detectAndSendChanges();
    }

    public void removeListener(IContainerListener listener) {
        listeners.remove(listener);
    }

    public NonNullList<ItemStack> getInventory() {
        NonNullList<ItemStack> list = NonNullList.create();
        for (Slot slot : inventorySlots) list.add(slot.getStack());
        return list;
    }

    /** Tells the listeners which slots changed since the last call; mods override it to send their window properties too. */
    public void detectAndSendChanges() {
        for (int i = 0; i < inventorySlots.size(); ++i) {
            ItemStack current = inventorySlots.get(i).getStack();
            ItemStack previous = inventoryItemStacks.get(i);
            if (!ItemStack.areItemStacksEqual(previous, current)) {
                boolean clientStackChanged = !ItemStack.areItemStacksEqualUsingNBTShareTag(previous, current);
                previous = current.isEmpty() ? ItemStack.EMPTY : current.copy();
                inventoryItemStacks.set(i, previous);
                if (clientStackChanged) for (IContainerListener l : listeners) l.sendSlotContents(this, i, previous);
            }
        }
    }

    public boolean enchantItem(EntityPlayer player, int id) {
        return false;
    }

    public Slot getSlotFromInventory(IInventory inv, int slotIn) {
        for (Slot slot : inventorySlots) if (slot.isHere(inv, slotIn)) return slot;
        return null;
    }

    public Slot getSlot(int slotId) {
        return inventorySlots.get(slotId);
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        return slot != null ? slot.getStack() : ItemStack.EMPTY;
    }

    /**
     * A click in the container. When a modern menu hosts the container, the click is the modern menu's (mods that override this to
     * swallow or rewrite clicks reach it through {@code super.slotClick}); otherwise the 1.12.2 algorithm runs here.
     */
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (mcc$menu instanceof org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyMenu1122 menu) {
            return menu.mcc$modernClick(slotId, dragType, clickType, player);
        }
        return mcc$legacyClick(slotId, dragType, clickType, player);
    }

    /** The 1.12.2 click algorithm over the legacy slots. */
    public ItemStack mcc$legacyClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        ItemStack itemstack = ItemStack.EMPTY;
        InventoryPlayer inventoryplayer = EntityPlayer.mcc$get$inventory(player);
        if (clickTypeIn == ClickType.QUICK_CRAFT) {
            int j1 = dragEvent;
            dragEvent = getDragEvent(dragType);
            if ((j1 != 1 || dragEvent != 2) && j1 != dragEvent) {
                resetDrag();
            } else if (inventoryplayer.getItemStack().isEmpty()) {
                resetDrag();
            } else if (dragEvent == 0) {
                dragMode = extractDragMode(dragType);
                if (isValidDragMode(dragMode, player)) {
                    dragEvent = 1;
                    dragSlots.clear();
                } else {
                    resetDrag();
                }
            } else if (dragEvent == 1) {
                Slot slot7 = inventorySlots.get(slotId);
                ItemStack carried = inventoryplayer.getItemStack();
                if (slot7 != null && canAddItemToSlot(slot7, carried, true) && slot7.isItemValid(carried)
                        && (dragMode == 2 || carried.getCount() > dragSlots.size()) && canDragIntoSlot(slot7)) {
                    dragSlots.add(slot7);
                }
            } else if (dragEvent == 2) {
                if (!dragSlots.isEmpty()) {
                    ItemStack itemstack9 = inventoryplayer.getItemStack().copy();
                    int k1 = inventoryplayer.getItemStack().getCount();
                    for (Slot slot8 : dragSlots) {
                        ItemStack carried = inventoryplayer.getItemStack();
                        if (slot8 != null && canAddItemToSlot(slot8, carried, true) && slot8.isItemValid(carried)
                                && (dragMode == 2 || carried.getCount() >= dragSlots.size()) && canDragIntoSlot(slot8)) {
                            ItemStack itemstack14 = itemstack9.copy();
                            int j3 = slot8.getHasStack() ? slot8.getStack().getCount() : 0;
                            computeStackSize(dragSlots, dragMode, itemstack14, j3);
                            int k3 = Math.min(itemstack14.getMaxStackSize(), slot8.getItemStackLimit(itemstack14));
                            if (itemstack14.getCount() > k3) itemstack14.setCount(k3);
                            k1 -= itemstack14.getCount() - j3;
                            slot8.putStack(itemstack14);
                        }
                    }
                    itemstack9.setCount(k1);
                    inventoryplayer.setItemStack(itemstack9);
                }
                resetDrag();
            } else {
                resetDrag();
            }
        } else if (dragEvent != 0) {
            resetDrag();
        } else if ((clickTypeIn == ClickType.PICKUP || clickTypeIn == ClickType.QUICK_MOVE) && (dragType == 0 || dragType == 1)) {
            if (slotId == -999) {
                if (!inventoryplayer.getItemStack().isEmpty()) {
                    if (dragType == 0) {
                        Inventories1122.dropFromPlayer(player, inventoryplayer.getItemStack());
                        inventoryplayer.setItemStack(ItemStack.EMPTY);
                    }
                    if (dragType == 1) {
                        ItemStack carried = inventoryplayer.getItemStack();
                        Inventories1122.dropFromPlayer(player, carried.splitStack(1));
                        inventoryplayer.setItemStack(carried);
                    }
                }
            } else if (clickTypeIn == ClickType.QUICK_MOVE) {
                if (slotId < 0) return ItemStack.EMPTY;
                Slot slot5 = inventorySlots.get(slotId);
                if (slot5 == null || !slot5.canTakeStack(player)) return ItemStack.EMPTY;
                for (ItemStack moved = transferStackInSlot(player, slotId); !moved.isEmpty() && ItemStack.areItemsEqual(slot5.getStack(), moved);
                        moved = transferStackInSlot(player, slotId)) {
                    itemstack = moved.copy();
                }
            } else {
                if (slotId < 0) return ItemStack.EMPTY;
                Slot slot6 = inventorySlots.get(slotId);
                if (slot6 != null) {
                    ItemStack inSlot = slot6.getStack();
                    ItemStack carried = inventoryplayer.getItemStack();
                    if (!inSlot.isEmpty()) itemstack = inSlot.copy();
                    if (inSlot.isEmpty()) {
                        if (!carried.isEmpty() && slot6.isItemValid(carried)) {
                            int i3 = dragType == 0 ? carried.getCount() : 1;
                            if (i3 > slot6.getItemStackLimit(carried)) i3 = slot6.getItemStackLimit(carried);
                            slot6.putStack(carried.splitStack(i3));
                            inventoryplayer.setItemStack(carried);
                        }
                    } else if (slot6.canTakeStack(player)) {
                        if (carried.isEmpty()) {
                            int l2 = dragType == 0 ? inSlot.getCount() : (inSlot.getCount() + 1) / 2;
                            inventoryplayer.setItemStack(slot6.decrStackSize(l2));
                            if (slot6.getStack().isEmpty()) slot6.putStack(ItemStack.EMPTY);
                            slot6.onTake(player, inventoryplayer.getItemStack());
                        } else if (slot6.isItemValid(carried)) {
                            if (inSlot.getItem() == carried.getItem() && inSlot.getMetadata() == carried.getMetadata()
                                    && ItemStack.areItemStackTagsEqual(inSlot, carried)) {
                                int k2 = dragType == 0 ? carried.getCount() : 1;
                                if (k2 > slot6.getItemStackLimit(carried) - inSlot.getCount()) k2 = slot6.getItemStackLimit(carried) - inSlot.getCount();
                                if (k2 > carried.getMaxStackSize() - inSlot.getCount()) k2 = carried.getMaxStackSize() - inSlot.getCount();
                                carried.shrink(k2);
                                inSlot.grow(k2);
                                slot6.putStack(inSlot);
                                inventoryplayer.setItemStack(carried);
                            } else if (carried.getCount() <= slot6.getItemStackLimit(carried)) {
                                slot6.putStack(carried);
                                inventoryplayer.setItemStack(inSlot);
                            }
                        } else if (inSlot.getItem() == carried.getItem() && carried.getMaxStackSize() > 1
                                && (!inSlot.getHasSubtypes() || inSlot.getMetadata() == carried.getMetadata())
                                && ItemStack.areItemStackTagsEqual(inSlot, carried) && !inSlot.isEmpty()) {
                            int j2 = inSlot.getCount();
                            if (j2 + carried.getCount() <= carried.getMaxStackSize()) {
                                carried.grow(j2);
                                inventoryplayer.setItemStack(carried);
                                ItemStack taken = slot6.decrStackSize(j2);
                                if (slot6.getStack().isEmpty()) slot6.putStack(ItemStack.EMPTY);
                                slot6.onTake(player, taken);
                            }
                        }
                    }
                    slot6.onSlotChanged();
                }
            }
        } else if (clickTypeIn == ClickType.SWAP && dragType >= 0 && dragType < 9) {
            Slot slot4 = inventorySlots.get(slotId);
            ItemStack hotbar = inventoryplayer.getStackInSlot(dragType);
            ItemStack inSlot = slot4.getStack();
            if (!hotbar.isEmpty() || !inSlot.isEmpty()) {
                if (hotbar.isEmpty()) {
                    if (slot4.canTakeStack(player)) {
                        inventoryplayer.setInventorySlotContents(dragType, inSlot);
                        slot4.onSwapCraft(inSlot.getCount());
                        slot4.putStack(ItemStack.EMPTY);
                        slot4.onTake(player, inSlot);
                    }
                } else if (inSlot.isEmpty()) {
                    if (slot4.isItemValid(hotbar)) {
                        int l1 = slot4.getItemStackLimit(hotbar);
                        if (hotbar.getCount() > l1) {
                            slot4.putStack(hotbar.splitStack(l1));
                            inventoryplayer.setInventorySlotContents(dragType, hotbar);
                        } else {
                            slot4.putStack(hotbar);
                            inventoryplayer.setInventorySlotContents(dragType, ItemStack.EMPTY);
                        }
                    }
                } else if (slot4.canTakeStack(player) && slot4.isItemValid(hotbar)) {
                    int i2 = slot4.getItemStackLimit(hotbar);
                    if (hotbar.getCount() > i2) {
                        slot4.putStack(hotbar.splitStack(i2));
                        inventoryplayer.setInventorySlotContents(dragType, hotbar);
                        slot4.onTake(player, inSlot);
                        if (!inventoryplayer.addItemStackToInventory(inSlot)) Inventories1122.dropFromPlayer(player, inSlot);
                    } else {
                        slot4.putStack(hotbar);
                        inventoryplayer.setInventorySlotContents(dragType, inSlot);
                        slot4.onTake(player, inSlot);
                    }
                }
            }
        } else if (clickTypeIn == ClickType.CLONE && PlayerCapabilities.mcc$get$isCreativeMode(EntityPlayer.mcc$get$capabilities(player))
                && inventoryplayer.getItemStack().isEmpty() && slotId >= 0) {
            Slot slot3 = inventorySlots.get(slotId);
            if (slot3 != null && slot3.getHasStack()) {
                ItemStack itemstack5 = slot3.getStack().copy();
                itemstack5.setCount(itemstack5.getMaxStackSize());
                inventoryplayer.setItemStack(itemstack5);
            }
        } else if (clickTypeIn == ClickType.THROW && inventoryplayer.getItemStack().isEmpty() && slotId >= 0) {
            Slot slot2 = inventorySlots.get(slotId);
            if (slot2 != null && slot2.getHasStack() && slot2.canTakeStack(player)) {
                ItemStack itemstack4 = slot2.decrStackSize(dragType == 0 ? 1 : slot2.getStack().getCount());
                slot2.onTake(player, itemstack4);
                Inventories1122.dropFromPlayer(player, itemstack4);
            }
        } else if (clickTypeIn == ClickType.PICKUP_ALL && slotId >= 0) {
            Slot slot = inventorySlots.get(slotId);
            ItemStack carried = inventoryplayer.getItemStack();
            if (!carried.isEmpty() && (slot == null || !slot.getHasStack() || !slot.canTakeStack(player))) {
                int i = dragType == 0 ? 0 : inventorySlots.size() - 1;
                int j = dragType == 0 ? 1 : -1;
                for (int k = 0; k < 2; ++k) {
                    for (int l = i; l >= 0 && l < inventorySlots.size() && carried.getCount() < carried.getMaxStackSize(); l += j) {
                        Slot slot1 = inventorySlots.get(l);
                        if (slot1.getHasStack() && canAddItemToSlot(slot1, carried, true) && slot1.canTakeStack(player) && canMergeSlot(carried, slot1)) {
                            ItemStack itemstack2 = slot1.getStack();
                            if (k != 0 || itemstack2.getCount() != itemstack2.getMaxStackSize()) {
                                int i1 = Math.min(carried.getMaxStackSize() - carried.getCount(), itemstack2.getCount());
                                ItemStack itemstack3 = slot1.decrStackSize(i1);
                                carried.grow(i1);
                                if (itemstack3.isEmpty()) slot1.putStack(ItemStack.EMPTY);
                                slot1.onTake(player, itemstack3);
                            }
                        }
                    }
                }
                inventoryplayer.setItemStack(carried);
            }
            detectAndSendChanges();
        }
        return itemstack;
    }

    /** Whether a double-click may gather stacks from this slot (result slots say no). */
    public boolean canMergeSlot(ItemStack stack, Slot slotIn) {
        return true;
    }

    /** Drops what the player carries on the cursor. */
    public void onContainerClosed(EntityPlayer player) {
        InventoryPlayer inventoryplayer = EntityPlayer.mcc$get$inventory(player);
        if (!inventoryplayer.getItemStack().isEmpty()) {
            Inventories1122.dropFromPlayer(player, inventoryplayer.getItemStack());
            inventoryplayer.setItemStack(ItemStack.EMPTY);
        }
    }

    /** Gives back (or drops) the contents of a temporary inventory, such as a crafting grid, when the GUI closes. */
    protected void clearContainer(EntityPlayer player, World world, IInventory inventory) {
        var modern = Inventories1122.player(player);
        boolean gone = modern == null || !modern.isAlive() || modern instanceof net.minecraft.server.level.ServerPlayer sp && sp.hasDisconnected();
        if (gone) {
            for (int j = 0; j < inventory.getSizeInventory(); ++j) Inventories1122.dropFromPlayer(player, inventory.removeStackFromSlot(j));
        } else {
            for (int i = 0; i < inventory.getSizeInventory(); ++i) {
                ItemStack s = inventory.removeStackFromSlot(i);
                if (!s.isEmpty()) Inventories1122.giveToPlayer(player, s);
            }
        }
    }

    public void onCraftMatrixChanged(IInventory inventory) {
        detectAndSendChanges();
    }

    public void putStackInSlot(int slotId, ItemStack stack) {
        getSlot(slotId).putStack(stack);
    }

    public void setAll(List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); ++i) getSlot(i).putStack(stacks.get(i));
    }

    /** Client side: a window property the server sent through {@link IContainerListener#sendWindowProperty}. */
    public void updateProgressBar(int id, int data) {
    }

    public short getNextTransactionID(InventoryPlayer inventory) {
        ++transactionID;
        return transactionID;
    }

    public boolean getCanCraft(EntityPlayer player) {
        return !playerList.contains(player);
    }

    public void setCanCraft(EntityPlayer player, boolean canCraft) {
        if (canCraft) playerList.remove(player);
        else playerList.add(player);
    }

    public abstract boolean canInteractWith(EntityPlayer playerIn);

    /** Verbatim 1.12.2 shift-click merge (with Forge's slot validity and stack limits): matching stacks first, then empty slots. */
    protected boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean flag = false;
        int i = reverseDirection ? endIndex - 1 : startIndex;
        if (stack.isStackable()) {
            while (!stack.isEmpty()) {
                if (reverseDirection ? i < startIndex : i >= endIndex) break;
                Slot slot = inventorySlots.get(i);
                ItemStack itemstack = slot.getStack();
                if (!itemstack.isEmpty() && itemstack.getItem() == stack.getItem() && (!stack.getHasSubtypes() || stack.getMetadata() == itemstack.getMetadata())
                        && ItemStack.areItemStackTagsEqual(stack, itemstack)) {
                    int j = itemstack.getCount() + stack.getCount();
                    int maxSize = Math.min(slot.getSlotStackLimit(), stack.getMaxStackSize());
                    if (j <= maxSize) {
                        stack.setCount(0);
                        itemstack.setCount(j);
                        slot.onSlotChanged();
                        flag = true;
                    } else if (itemstack.getCount() < maxSize) {
                        stack.shrink(maxSize - itemstack.getCount());
                        itemstack.setCount(maxSize);
                        slot.onSlotChanged();
                        flag = true;
                    }
                }
                i += reverseDirection ? -1 : 1;
            }
        }
        if (!stack.isEmpty()) {
            i = reverseDirection ? endIndex - 1 : startIndex;
            while (true) {
                if (reverseDirection ? i < startIndex : i >= endIndex) break;
                Slot slot1 = inventorySlots.get(i);
                ItemStack itemstack1 = slot1.getStack();
                if (itemstack1.isEmpty() && slot1.isItemValid(stack)) {
                    if (stack.getCount() > slot1.getSlotStackLimit()) slot1.putStack(stack.splitStack(slot1.getSlotStackLimit()));
                    else slot1.putStack(stack.splitStack(stack.getCount()));
                    slot1.onSlotChanged();
                    flag = true;
                    break;
                }
                i += reverseDirection ? -1 : 1;
            }
        }
        return flag;
    }

    public static int extractDragMode(int eventButton) {
        return eventButton >> 2 & 3;
    }

    public static int getDragEvent(int clickedButton) {
        return clickedButton & 3;
    }

    public static int getQuickcraftMask(int p_94534_0_, int p_94534_1_) {
        return p_94534_0_ & 3 | (p_94534_1_ & 3) << 2;
    }

    public static boolean isValidDragMode(int dragModeIn, EntityPlayer player) {
        if (dragModeIn == 0) return true;
        if (dragModeIn == 1) return true;
        return dragModeIn == 2 && PlayerCapabilities.mcc$get$isCreativeMode(EntityPlayer.mcc$get$capabilities(player));
    }

    protected void resetDrag() {
        dragEvent = 0;
        dragSlots.clear();
    }

    public static boolean canAddItemToSlot(Slot slotIn, ItemStack stack, boolean stackSizeMatters) {
        boolean flag = slotIn == null || !slotIn.getHasStack();
        if (!flag && stack.isItemEqual(slotIn.getStack()) && ItemStack.areItemStackTagsEqual(slotIn.getStack(), stack)) {
            return slotIn.getStack().getCount() + (stackSizeMatters ? 0 : stack.getCount()) <= stack.getMaxStackSize();
        }
        return flag;
    }

    public static void computeStackSize(Set<Slot> dragSlotsIn, int dragModeIn, ItemStack stack, int slotStackSize) {
        switch (dragModeIn) {
            case 0 -> stack.setCount(MathHelper.floor((float) stack.getCount() / (float) dragSlotsIn.size()));
            case 1 -> stack.setCount(1);
            case 2 -> stack.setCount(stack.getItem().getItemStackLimit(stack));
            default -> {
            }
        }
        stack.grow(slotStackSize);
    }

    public boolean canDragIntoSlot(Slot slotIn) {
        return true;
    }

    public static int calcRedstone(legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity te) {
        return te instanceof IInventory inv ? calcRedstoneFromInventory(inv) : 0;
    }

    public static int calcRedstoneFromInventory(IInventory inv) {
        if (inv == null) return 0;
        int i = 0;
        float f = 0.0F;
        for (int j = 0; j < inv.getSizeInventory(); ++j) {
            ItemStack itemstack = inv.getStackInSlot(j);
            if (!itemstack.isEmpty()) {
                f += (float) itemstack.getCount() / (float) Math.min(inv.getInventoryStackLimit(), itemstack.getMaxStackSize());
                ++i;
            }
        }
        f = f / (float) inv.getSizeInventory();
        return MathHelper.floor(f * 14.0F) + (i > 0 ? 1 : 0);
    }

    /** Recomputes the result slot of a crafting grid and sends it to the player's client. */
    protected void slotChangedCraftingGrid(World world, EntityPlayer player, InventoryCrafting inv, InventoryCraftResult result) {
        if (world.isRemote) return;
        ItemStack itemstack = ItemStack.EMPTY;
        legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe recipe = legacyconnector.v1_12_2.net.minecraft.item.crafting.CraftingManager.findMatchingRecipe(inv, world);
        if (recipe != null) {
            result.setRecipeUsed(recipe);
            itemstack = recipe.getCraftingResult(inv);
        } else {
            result.setRecipeUsed(null);
            itemstack = org.kylepitcock.legacyconnector.v1_12_2.bridge.Crafting1122.modernResult(inv, world);
        }
        result.setInventorySlotContents(0, itemstack);
        if (Inventories1122.player(player) instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(windowId, 0, 0,
                    org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(itemstack)));
        }
    }
}
