package legacyconnector.v1_12_2.net.minecraft.util.datafix;

public class DataFixesManager {
    public static DataFixer createFixer() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.util.CompoundDataFixer.INSTANCE;
    }

    public static DataFixer getDataFixer() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.util.CompoundDataFixer.INSTANCE;
    }

    /**
     * Runs the item fixes mods registered over the item stack tag at {@code key} (vanilla's own item upgrades are the layer's
     * conversion of legacy stacks).
     */
    public static legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound processItemStack(IDataFixer fixer, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound compound, int version, String key) {
        if (fixer != null && compound != null && compound.hasKey(key, 10)) {
            compound.setTag(key, fixer.process(FixTypes.ITEM_INSTANCE, compound.getCompoundTag(key), version));
        }
        return compound;
    }

    /** Runs the item fixes mods registered over each stack of the item list at {@code key}. */
    public static legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound processInventory(IDataFixer fixer, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound compound, int version, String key) {
        if (fixer != null && compound != null && compound.hasKey(key, 9)) {
            var list = compound.getTagList(key, 10);
            for (int i = 0; i < list.tagCount(); i++) list.set(i, fixer.process(FixTypes.ITEM_INSTANCE, list.getCompoundTagAt(i), version));
        }
        return compound;
    }
}
