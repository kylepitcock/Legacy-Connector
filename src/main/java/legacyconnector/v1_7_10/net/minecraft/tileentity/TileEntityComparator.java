package legacyconnector.v1_7_10.net.minecraft.tileentity;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/** The comparator's output signal (0-15). A view of a vanilla comparator reads and writes the modern one. */
public class TileEntityComparator extends TileEntity {
    private int outputSignal;
    private final net.minecraft.world.level.block.entity.ComparatorBlockEntity mcc$comparator;

    public TileEntityComparator() {
        this.mcc$comparator = null;
    }

    /** View of a vanilla comparator. */
    public TileEntityComparator(net.minecraft.world.level.block.entity.ComparatorBlockEntity comparator) {
        this.mcc$comparator = comparator;
    }

    public int getOutputSignal() {
        if (mcc$comparator != null) return mcc$comparator.getOutputSignal();
        return outputSignal;
    }

    public void setOutputSignal(int signal) {
        if (mcc$comparator != null) VanillaOps.setInt(mcc$comparator, net.minecraft.world.level.block.entity.ComparatorBlockEntity.class, "output", signal);
        else outputSignal = signal;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("OutputSignal", getOutputSignal());
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        setOutputSignal(tag.getInteger("OutputSignal"));
    }
}
