package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.ItemStackHelper;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/**
 * 1.12.2's shulker box: 27 slots kept when it is broken (saved into the dropped item), and its lid animation. A shulker box of the game
 * is seen through a view over its slots and animation.
 */
public class TileEntityShulkerBox extends TileEntityLockableLoot implements org.kylepitcock.legacyconnector.v1_12_2.bridge.TileViews1122.Refreshable {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private boolean hasBeenCleared;
    private int openCount;
    private AnimationStatus animationStatus = AnimationStatus.CLOSED;
    private float progress;
    private float progressOld;
    private boolean destroyedByCreativePlayer;
    private final net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity mcc$box;

    public TileEntityShulkerBox() {
        this.mcc$box = null;
    }

    /** A view of a game shulker box. */
    public TileEntityShulkerBox(net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity box) {
        this.mcc$box = box;
        this.items = new org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernStackList1122(box);
        mcc$refresh();
    }

    @Override
    public void mcc$refresh() {
        if (mcc$box == null) return;
        animationStatus = AnimationStatus.valueOf(mcc$box.getAnimationStatus().name());
        progressOld = progress;
        progress = mcc$box.getProgress(1.0F);
    }

    @Override
    public int getSizeInventory() {
        return items.size();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public String getName() {
        return hasCustomName() ? customName : "container.shulkerBox";
    }

    public AnimationStatus getAnimationStatus() {
        return animationStatus;
    }

    public float getProgress(float p_190585_1_) {
        return progressOld + (progress - progressOld) * p_190585_1_;
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        if (id == 1) {
            openCount = type;
            if (type == 0) animationStatus = AnimationStatus.CLOSING;
            if (type == 1) animationStatus = AnimationStatus.OPENING;
            return true;
        }
        return super.receiveClientEvent(id, type);
    }

    @Override
    public void openInventory(EntityPlayer player) {
        if (player == null || player.isSpectator()) return;
        if (mcc$box != null) {
            if (player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) mcc$box.startOpen(p);
            return;
        }
        if (openCount < 0) openCount = 0;
        ++openCount;
        world.addBlockEvent(pos, getBlockType(), 1, openCount);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        if (player == null || player.isSpectator()) return;
        if (mcc$box != null) {
            if (player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) mcc$box.stopOpen(p);
            return;
        }
        --openCount;
        world.addBlockEvent(pos, getBlockType(), 1, openCount);
    }

    @Override
    public String getGuiID() {
        return "minecraft:shulker_box";
    }

    /** 1.12's ContainerShulkerBox is not part of this layer yet. */
    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        loadFromNbt(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        return saveToNbt(compound);
    }

    public void loadFromNbt(NBTTagCompound compound) {
        if (mcc$box == null) items = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
        if (!checkLootAndRead(compound) && compound.hasKey("Items", 9)) ItemStackHelper.loadAllItems(compound, items);
        if (compound.hasKey("CustomName", 8)) customName = compound.getString("CustomName");
    }

    /** The contents as saved into the dropped box (empty slots left out). */
    public NBTTagCompound saveToNbt(NBTTagCompound compound) {
        if (!checkLootAndWrite(compound)) ItemStackHelper.saveAllItems(compound, items, false);
        if (hasCustomName()) compound.setString("CustomName", customName);
        if (!compound.hasKey("Lock") && isLocked()) compound.setString("Lock", "");
        return compound;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    public boolean isCleared() {
        return hasBeenCleared;
    }

    public void setDestroyedByCreativePlayer(boolean p_190579_1_) {
        destroyedByCreativePlayer = p_190579_1_;
    }

    /** Whether breaking it drops the box (not when a creative player broke an empty one). */
    public boolean shouldDrop() {
        return !isDestroyedByCreativePlayer() || !isEmpty() || hasCustomName() || lootTable != null;
    }

    public boolean isDestroyedByCreativePlayer() {
        return destroyedByCreativePlayer;
    }

    @Override
    public void clear() {
        hasBeenCleared = true;
        super.clear();
    }

    public enum AnimationStatus {
        CLOSED,
        OPENING,
        OPENED,
        CLOSING
    }
}
