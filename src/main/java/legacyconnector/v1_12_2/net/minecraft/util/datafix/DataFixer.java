package legacyconnector.v1_12_2.net.minecraft.util.datafix;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** Legacy save-data upgrades: legacy worlds are converted by the layer itself, so registered walkers and fixes are accepted and unused. */
public class DataFixer implements IDataFixer {
    public DataFixer() {
    }

    public DataFixer(int version) {
    }

    @Override
    public NBTTagCompound process(IFixType type, NBTTagCompound compound, int versionIn) {
        return compound;
    }

    public void registerWalker(FixTypes type, IDataWalker walker) {
    }

    public void registerFix(IFixType type, IFixableData fixable) {
    }
}
