package legacyconnector.v1_12_2.net.minecraft.util.datafix;

public class DataFixesManager {
    public static DataFixer createFixer() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.util.CompoundDataFixer.INSTANCE;
    }

    public static DataFixer getDataFixer() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.util.CompoundDataFixer.INSTANCE;
    }

    /**
     * The fixes of an item stack tag at {@code key} (area F): 1.12 data walkers ran the registered fixes; legacy stacks are read
     * by the layer's own conversion, so the tag is given back as it is.
     */
    public static legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound processItemStack(IDataFixer fixer, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound compound, int version, String key) {
        return compound;
    }

    public static legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound processInventory(IDataFixer fixer, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound compound, int version, String key) {
        return compound;
    }
}
