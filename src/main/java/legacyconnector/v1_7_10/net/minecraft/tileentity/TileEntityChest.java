package legacyconnector.v1_7_10.net.minecraft.tileentity;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * Legacy view of a vanilla chest. {@link #chestContents} must stay the first declared field: mods read it by index
 * through {@code ObfuscationReflectionHelper}. It is a snapshot refreshed whenever the view is fetched, and written
 * back into the real chest before the block at this position changes (so code that nulls entries and then
 * breaks the chest does not duplicate items).
 */
public class TileEntityChest extends TileEntity implements IInventory {
    private ItemStack[] chestContents = new ItemStack[27];
    public boolean adjacentChestChecked;
    public TileEntityChest adjacentChestZNeg;
    public TileEntityChest adjacentChestXPos;
    public TileEntityChest adjacentChestXNeg;
    public TileEntityChest adjacentChestZPos;
    public float lidAngle;
    public float prevLidAngle;
    public int numPlayersUsing;
    private final net.minecraft.world.Container mcc$inv;
    /** Set while a chunk's load event is posted: the chunk cannot be fetched yet. */
    public static final ThreadLocal<Boolean> MCC_CHUNK_LOADING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public TileEntityChest() {
        this.mcc$inv = null;
    }

    public TileEntityChest(net.minecraft.world.Container modern) {
        this.mcc$inv = modern;
        this.chestContents = new ItemStack[modern.getContainerSize()];
        mcc$refresh();
    }

    public void mcc$refresh() {
        if (mcc$inv == null) return;
        // Reading a chest with a pending loot table fills it and calls setChanged, which fetches the chunk and blocks the
        // server thread when the chunk is still being generated or loaded: keep the old snapshot until the chunk is ready.
        if (mcc$inv instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity r && r.getLootTable() != null) {
            var level = r.getLevel();
            if (level == null || MCC_CHUNK_LOADING.get()) return;
            if (!level.isClientSide && level.getChunkSource().getChunkNow(r.getBlockPos().getX() >> 4, r.getBlockPos().getZ() >> 4) == null) return;
        }
        if (chestContents.length != mcc$inv.getContainerSize()) chestContents = new ItemStack[mcc$inv.getContainerSize()];
        for (int i = 0; i < chestContents.length; i++) chestContents[i] = ItemStack.mcc$wrap(mcc$inv.getItem(i));
    }

    /** Writes array edits made through reflection back into the real chest. */
    public void mcc$flush() {
        if (mcc$inv == null) return;
        for (int i = 0; i < chestContents.length && i < mcc$inv.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack want = ItemStack.mcc$toModern(chestContents[i]);
            if (mcc$inv.getItem(i) != want) mcc$inv.setItem(i, want);
        }
    }

    public void checkForAdjacentChests() {
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
        adjacentChestChecked = false;
    }

    public int func_145980_j() {
        return 0;
    }

    @Override
    public int getSizeInventory() {
        return chestContents.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        mcc$refresh();
        return chestContents[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack out = mcc$inv == null ? null : ItemStack.mcc$wrap(mcc$inv.removeItem(slot, amount));
        mcc$refresh();
        return out;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack out = mcc$inv == null ? null : ItemStack.mcc$wrap(mcc$inv.removeItemNoUpdate(slot));
        mcc$refresh();
        return out;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (mcc$inv != null) mcc$inv.setItem(slot, ItemStack.mcc$toModern(stack));
        mcc$refresh();
    }

    @Override
    public String getInventoryName() {
        return "container.chest";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
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
