package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer;
import legacyconnector.v1_12_2.net.minecraft.util.datafix.IDataWalker;
import legacyconnector.v1_12_2.net.minecraft.util.datafix.IFixType;
import legacyconnector.v1_12_2.net.minecraft.util.datafix.IFixableData;

/**
 * Forge's data fixer: vanilla's fixes and walkers plus each mod's own fixes, run for data saved with an older version of that mod (the versions
 * are kept in the data's {@code ForgeDataVersion} tag).
 */
public class CompoundDataFixer extends DataFixer {
    public static final CompoundDataFixer INSTANCE = new CompoundDataFixer();
    private final Map<String, ModFixs> modFixers = new LinkedHashMap<>();

    public synchronized ModFixs init(String modid, int version) {
        // Forge threw on a second init of the same mod; the first one is kept instead
        return modFixers.computeIfAbsent(modid, k -> new ModFixs(modid, version));
    }

    @Override
    public NBTTagCompound process(IFixType type, NBTTagCompound compound) {
        Map<String, Integer> versions = new LinkedHashMap<>();
        int vanilla = compound.hasKey("DataVersion", 99) ? compound.getInteger("DataVersion") : -1;
        if (compound.hasKey("ForgeDataVersion", 10)) {
            NBTTagCompound forge = compound.getCompoundTag("ForgeDataVersion");
            for (String mod : forge.getKeySet()) versions.put(mod, forge.getInteger(mod));
        }
        compound = vanilla >= VERSION ? compound : super.process(type, compound, vanilla);
        ModFixs[] mods;
        synchronized (this) {
            mods = modFixers.values().toArray(new ModFixs[0]);
        }
        for (ModFixs mod : mods) {
            int saved = versions.getOrDefault(mod.mod, -1);
            if (saved >= mod.version) continue;
            for (IFixableData fix : mod.getFixes(type)) {
                if (fix.getFixVersion() > saved) compound = fix.fixTagCompound(compound);
            }
        }
        return compound;
    }

    @Override
    public synchronized void registerVanillaWalker(IFixType type, IDataWalker walker) {
        super.registerVanillaWalker(type, walker);
    }

    /** Writes the current data versions (vanilla and every mod's) into a compound that is about to be saved. */
    public synchronized void writeVersionData(NBTTagCompound compound) {
        NBTTagCompound forge = new NBTTagCompound();
        for (ModFixs mod : modFixers.values()) forge.setInteger(mod.mod, mod.version);
        compound.setTag("ForgeDataVersion", forge);
    }
}
