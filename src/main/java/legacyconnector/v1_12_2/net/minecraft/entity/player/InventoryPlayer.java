package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * 1.12.2 player inventory: a view over the modern one, which keeps 1.12's layout (36 main slots, 4 armour, 1 off hand; the same
 * slot numbers). Stacks read from it are live: changing one writes it back to its slot.
 */
public class InventoryPlayer implements IInventory {
    public final NonNullList<ItemStack> mainInventory;
    public final NonNullList<ItemStack> armorInventory;
    public final NonNullList<ItemStack> offHandInventory;
    public final java.util.List<NonNullList<ItemStack>> allInventories;
    public EntityPlayer player;
    /** Phantom field: accesses are redirected to {@code mcc$get$currentItem}. */
    @Deprecated
    public int currentItem;
    private int mcc$local_currentItem;
    /** Phantom field: accesses are redirected to {@code mcc$get$itemStack} (the stack on the cursor). */
    @Deprecated
    public ItemStack itemStack;

    public InventoryPlayer(EntityPlayer player) {
        this.player = player;
        this.mainInventory = new SlotList(this, 0, 36);
        this.armorInventory = new SlotList(this, 36, 4);
        this.offHandInventory = new SlotList(this, 40, 1);
        this.allInventories = java.util.List.of(mainInventory, armorInventory, offHandInventory);
    }

    private net.minecraft.world.entity.player.Inventory mcc$modern() {
        var p = player == null ? null : player.mcc$player();
        return p == null ? null : p.getInventory();
    }

    /** A legacy list over a run of modern slots. */
    private static final class SlotList extends NonNullList<ItemStack> {
        SlotList(InventoryPlayer inventory, int offset, int size) {
            super(new java.util.AbstractList<>() {
                @Override
                public ItemStack get(int index) {
                    return inventory.getStackInSlot(offset + index);
                }

                @Override
                public ItemStack set(int index, ItemStack element) {
                    ItemStack old = get(index);
                    inventory.setInventorySlotContents(offset + index, element);
                    return old;
                }

                @Override
                public int size() {
                    return size;
                }
            }, ItemStack.EMPTY);
        }
    }

    public static int getHotbarSize() {
        return 9;
    }

    public static boolean isHotbar(int index) {
        return index >= 0 && index < 9;
    }

    public ItemStack getCurrentItem() {
        return isHotbar(mcc$get$currentItem(this)) ? getStackInSlot(mcc$get$currentItem(this)) : ItemStack.EMPTY;
    }

    public int getFirstEmptyStack() {
        var inv = mcc$modern();
        return inv == null ? -1 : inv.getFreeSlot();
    }

    public int getSlotFor(ItemStack stack) {
        var inv = mcc$modern();
        return inv == null ? -1 : inv.findSlotMatchingItem(Stacks1122.toModern(stack));
    }

    public boolean hasItemStack(ItemStack stack) {
        var inv = mcc$modern();
        if (inv == null || stack == null || stack.isEmpty()) return false;
        var modern = Stacks1122.toModern(stack);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(inv.getItem(i), modern)) return true;
        }
        return false;
    }

    /** Adds the stack where it fits and leaves what did not fit in it (its count shrinks), as 1.12.2 did. */
    public boolean addItemStackToInventory(ItemStack stack) {
        var inv = mcc$modern();
        if (inv == null || stack == null || stack.isEmpty()) return false;
        var modern = Stacks1122.toModern(stack);
        boolean added = inv.add(modern);
        stack.setCount(modern.getCount());
        return added;
    }

    public int clearMatchingItems(legacyconnector.v1_12_2.net.minecraft.item.Item item, int meta, int max, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        int removed = 0;
        for (int i = 0; i < getSizeInventory() && (max < 0 || removed < max); i++) {
            ItemStack s = getStackInSlot(i);
            if (s.isEmpty() || item != null && s.getItem() != item || meta > -1 && s.getMetadata() != meta) continue;
            int n = max < 0 ? s.getCount() : Math.min(max - removed, s.getCount());
            removed += n;
            if (max != 0) decrStackSize(i, n);
        }
        return removed;
    }

    public void changeCurrentItem(int direction) {
        int slot = mcc$get$currentItem(this) - Integer.signum(direction);
        while (slot < 0) slot += 9;
        mcc$set$currentItem(this, slot % 9);
    }

    public float getDestroySpeed(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        var inv = mcc$modern();
        return inv == null ? 1.0F : inv.getDestroySpeed(org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state));
    }

    public ItemStack armorItemInSlot(int slot) {
        return getStackInSlot(36 + slot);
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

    public void dropAllItems() {
        var inv = mcc$modern();
        if (inv != null) inv.dropAll();
    }

    public void markDirty() {
        var inv = mcc$modern();
        if (inv != null) inv.setChanged();
    }

    // ------------------------------------------------------------------ IInventory

    @Override
    public int getSizeInventory() {
        return 41;
    }

    @Override
    public boolean isEmpty() {
        var inv = mcc$modern();
        return inv == null || inv.isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        var inv = mcc$modern();
        if (inv == null || index < 0 || index >= inv.getContainerSize()) return ItemStack.EMPTY;
        return Stacks1122.fromModern(inv.getItem(index)).mcc$bind(s -> inv.setItem(index, Stacks1122.toModern(s)));
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        var inv = mcc$modern();
        return inv == null ? ItemStack.EMPTY : Stacks1122.fromModern(inv.removeItem(index, count));
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        var inv = mcc$modern();
        return inv == null ? ItemStack.EMPTY : Stacks1122.fromModern(inv.removeItemNoUpdate(index));
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        var inv = mcc$modern();
        if (inv != null && index >= 0 && index < inv.getContainerSize()) inv.setItem(index, Stacks1122.toModern(stack));
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return player != null && player == this.player;
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
        var inv = mcc$modern();
        if (inv != null) inv.clearContent();
    }

    @Override
    public String getName() {
        return "container.inventory";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    // ------------------------------------------------------------------ phantom field accessors

    public static int mcc$get$currentItem(InventoryPlayer i) {
        var inv = i.mcc$modern();
        return inv != null ? inv.selected : i.mcc$local_currentItem;
    }

    public static void mcc$set$currentItem(InventoryPlayer i, int v) {
        var inv = i.mcc$modern();
        if (inv != null) inv.selected = v;
        else i.mcc$local_currentItem = v;
    }

    public static ItemStack mcc$get$itemStack(InventoryPlayer i) {
        var p = i.player == null ? null : i.player.mcc$player();
        if (p == null) return ItemStack.EMPTY;
        var menu = p.containerMenu;
        return Stacks1122.fromModern(menu.getCarried()).mcc$bind(s -> menu.setCarried(Stacks1122.toModern(s)));
    }

    public static void mcc$set$itemStack(InventoryPlayer i, ItemStack v) {
        var p = i.player == null ? null : i.player.mcc$player();
        if (p != null) p.containerMenu.setCarried(Stacks1122.toModern(v));
    }

    public ItemStack getItemStack() {
        return mcc$get$itemStack(this);
    }

    public void setItemStack(ItemStack stack) {
        mcc$set$itemStack(this, stack);
    }
}
