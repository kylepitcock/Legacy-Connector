package legacyconnector.v1_12_2.net.minecraft.util.datafix.walkers;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixesManager;
import legacyconnector.v1_12_2.net.minecraft.util.datafix.IDataFixer;

/** Runs the item fixes over the item lists (inventories) under these keys of one entity or tile entity class's data. */
public class ItemStackDataLists extends Filtered {
    private final String[] matchingTags;

    public ItemStackDataLists(Class<?> type, String... matchingTags) {
        super(type);
        this.matchingTags = matchingTags;
    }

    @Override
    NBTTagCompound filteredProcess(IDataFixer fixer, NBTTagCompound compound, int versionIn) {
        for (String tag : matchingTags) compound = DataFixesManager.processInventory(fixer, compound, versionIn, tag);
        return compound;
    }
}
