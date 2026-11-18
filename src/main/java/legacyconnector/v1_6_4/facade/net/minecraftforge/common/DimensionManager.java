package legacyconnector.v1_6_4.facade.net.minecraftforge.common;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/**
 * Facade of the 1.7.10 shim's {@code DimensionManager} for 1.6.4 mods: the dimension id bit set Forge saved with the world
 * ({@code DimensionArray}, one bit per id in use). See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class DimensionManager {
    private DimensionManager() {}

    /** 1.6.4 {@code saveDimensionDataMap()}: the tag holding a bit set of the registered dimension ids ({@code DimensionArray}, 32 ids per int). */
    public static NBTTagCompound saveDimensionDataMap() {
        java.util.BitSet used = new java.util.BitSet();
        for (Integer id : legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager.getIDs()) if (id >= 0) used.set(id);
        int[] data = new int[(used.length() + 31) / 32];
        for (int i = used.nextSetBit(0); i >= 0; i = used.nextSetBit(i + 1)) data[i / 32] |= 1 << (i % 32);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("DimensionArray", data);
        return tag;
    }

    /**
     * 1.6.4 {@code loadDimensionDataMap(tag)}: Forge reserved the ids a save had used so a new dimension did not take one. Registered dimensions are
     * known from the mods here and unregistered ids stay free, so there is nothing to apply.
     */
    public static void loadDimensionDataMap(NBTTagCompound tag) {
    }
}
