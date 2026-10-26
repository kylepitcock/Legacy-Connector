package legacyconnector.v1_12_2.net.minecraft.util.datafix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/**
 * Save-data upgrades: fixes (by data version) and walkers (that descend into nested data) per kind of data. Only what mods register runs;
 * vanilla's own 1.12 fixes are not needed (data older than 1.12 is converted by the layer itself).
 */
public class DataFixer implements IDataFixer {
    /** The data version of 1.12.2 saves. */
    public static final int VERSION = 1343;
    private final Map<IFixType, List<IDataWalker>> walkers = new HashMap<>();
    private final Map<IFixType, List<IFixableData>> fixes = new HashMap<>();
    protected final int version;

    public DataFixer() {
        this(VERSION);
    }

    public DataFixer(int version) {
        this.version = version;
    }

    /** Upgrades data of the version its {@code DataVersion} tag names (old if absent) to the current one. */
    public NBTTagCompound process(IFixType type, NBTTagCompound compound) {
        int v = compound.hasKey("DataVersion", 99) ? compound.getInteger("DataVersion") : -1;
        return v >= VERSION ? compound : process(type, compound, v);
    }

    @Override
    public NBTTagCompound process(IFixType type, NBTTagCompound compound, int versionIn) {
        if (versionIn < version) {
            compound = processFixes(type, compound, versionIn);
            compound = processWalkers(type, compound, versionIn);
        }
        return compound;
    }

    private synchronized List<IFixableData> fixesOf(IFixType type) {
        return new ArrayList<>(fixes.getOrDefault(type, List.of()));
    }

    private synchronized List<IDataWalker> walkersOf(IFixType type) {
        return new ArrayList<>(walkers.getOrDefault(type, List.of()));
    }

    private NBTTagCompound processFixes(IFixType type, NBTTagCompound compound, int versionIn) {
        for (IFixableData fix : fixesOf(type)) {
            if (fix.getFixVersion() > versionIn) compound = fix.fixTagCompound(compound);
        }
        return compound;
    }

    private NBTTagCompound processWalkers(IFixType type, NBTTagCompound compound, int versionIn) {
        for (IDataWalker walker : walkersOf(type)) compound = walker.process(this, compound, versionIn);
        return compound;
    }

    public void registerWalker(FixTypes type, IDataWalker walker) {
        registerVanillaWalker(type, walker);
    }

    public synchronized void registerVanillaWalker(IFixType type, IDataWalker walker) {
        walkers.computeIfAbsent(type, k -> new ArrayList<>()).add(walker);
    }

    /** Fixes run in version order; one for a version newer than the fixer's own is ignored, as vanilla did. */
    public synchronized void registerFix(IFixType type, IFixableData fixable) {
        int v = fixable.getFixVersion();
        if (v > version) return;
        List<IFixableData> list = fixes.computeIfAbsent(type, k -> new ArrayList<>());
        int i = list.size();
        while (i > 0 && list.get(i - 1).getFixVersion() > v) i--;
        list.add(i, fixable);
    }
}
