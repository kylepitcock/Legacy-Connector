package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.items.CapabilityItemHandler;
import legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler;
import legacyconnector.v1_12_2.net.minecraftforge.items.wrapper.InvWrapper;

/**
 * 1.12.2's base of tile entities with an inventory and a GUI (chests, furnaces, hoppers, dispensers): its display name, the lock key
 * players could set with {@code /blockdata} (no lock is ever set on this game's blocks) and Forge's item handler capability over the
 * inventory.
 */
public abstract class TileEntityLockable extends TileEntity implements IInventory, legacyconnector.v1_12_2.net.minecraft.world.ILockableContainer {
    private String mcc$lock = "";
    protected IItemHandler itemHandler;

    public TileEntityLockable() {
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        mcc$lock = compound.hasKey("Lock", 8) ? compound.getString("Lock") : "";
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (!mcc$lock.isEmpty()) compound.setString("Lock", mcc$lock);
        return compound;
    }

    public boolean isLocked() {
        return !mcc$lock.isEmpty();
    }

    public legacyconnector.v1_12_2.net.minecraft.world.LockCode getLockCode() {
        return mcc$lock.isEmpty() ? legacyconnector.v1_12_2.net.minecraft.world.LockCode.EMPTY_CODE : new legacyconnector.v1_12_2.net.minecraft.world.LockCode(mcc$lock);
    }

    public void setLockCode(legacyconnector.v1_12_2.net.minecraft.world.LockCode code) {
        mcc$lock = code == null || code.getLock() == null ? "" : code.getLock();
    }

    /** The container the GUI of this inventory shows (mods' subclasses build theirs). */
    public abstract Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn);

    /** The id of the GUI ("minecraft:chest"...). */
    public abstract String getGuiID();

    @Override
    public ITextComponent getDisplayName() {
        return hasCustomName() ? new TextComponentString(getName()) : new TextComponentTranslation(getName());
    }

    protected IItemHandler createUnSidedHandler() {
        return new InvWrapper(this);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (itemHandler == null) itemHandler = createUnSidedHandler();
            return (T) itemHandler;
        }
        return super.getCapability(capability, facing);
    }
}
