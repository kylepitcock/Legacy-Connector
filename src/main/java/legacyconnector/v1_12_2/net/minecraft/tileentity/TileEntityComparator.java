package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** 1.12.2's comparator tile entity: the signal it outputs. A comparator of the game is seen through a view of its output. */
public class TileEntityComparator extends TileEntity {
    private int outputSignal;
    private final net.minecraft.world.level.block.entity.ComparatorBlockEntity mcc$comparator;

    public TileEntityComparator() {
        this.mcc$comparator = null;
    }

    /** A view of a game comparator. */
    public TileEntityComparator(net.minecraft.world.level.block.entity.ComparatorBlockEntity comparator) {
        this.mcc$comparator = comparator;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("OutputSignal", getOutputSignal());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        outputSignal = compound.getInteger("OutputSignal");
    }

    public int getOutputSignal() {
        return mcc$comparator != null ? mcc$comparator.getOutputSignal() : outputSignal;
    }

    public void setOutputSignal(int outputSignalIn) {
        this.outputSignal = outputSignalIn;
        if (mcc$comparator != null) mcc$comparator.setOutputSignal(outputSignalIn);
    }
}
