package legacyconnector.v1_12_2.net.minecraft.tileentity;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.ItemStackHelper;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** 1.12.2's dispenser: nine slots, a random filled slot dispensed. A dispenser of the game is seen through a view over its slots. */
public class TileEntityDispenser extends TileEntityLockableLoot {
    private static final Random RNG = new Random();
    private NonNullList<ItemStack> stacks = NonNullList.withSize(9, ItemStack.EMPTY);
    protected final net.minecraft.world.level.block.entity.DispenserBlockEntity mcc$dispenser;

    public TileEntityDispenser() {
        this.mcc$dispenser = null;
    }

    /** A view of a game dispenser (or dropper). */
    public TileEntityDispenser(net.minecraft.world.level.block.entity.DispenserBlockEntity dispenser) {
        this.mcc$dispenser = dispenser;
        this.stacks = new org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernStackList1122(dispenser);
    }

    @Override
    public int getSizeInventory() {
        return 9;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : stacks) if (!s.isEmpty()) return false;
        return true;
    }

    /** A random filled slot (-1 when empty), each filled slot equally likely. */
    public int getDispenseSlot() {
        fillWithLoot(null);
        int i = -1;
        int j = 1;
        for (int k = 0; k < stacks.size(); ++k) {
            if (!stacks.get(k).isEmpty() && RNG.nextInt(j++) == 0) i = k;
        }
        return i;
    }

    /** Puts the stack into the first empty slot; the slot, or -1 when full. */
    public int addItemStack(ItemStack stack) {
        for (int i = 0; i < stacks.size(); ++i) {
            if (stacks.get(i).isEmpty()) {
                setInventorySlotContents(i, stack);
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getName() {
        return hasCustomName() ? customName : "container.dispenser";
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (mcc$dispenser == null) stacks = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
        if (!checkLootAndRead(compound)) ItemStackHelper.loadAllItems(compound, stacks);
        if (compound.hasKey("CustomName", 8)) customName = compound.getString("CustomName");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (!checkLootAndWrite(compound)) ItemStackHelper.saveAllItems(compound, stacks);
        if (hasCustomName()) compound.setString("CustomName", customName);
        return compound;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public String getGuiID() {
        return "minecraft:dispenser";
    }

    /** 1.12's ContainerDispenser is not part of this layer yet. */
    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return null;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return stacks;
    }
}
