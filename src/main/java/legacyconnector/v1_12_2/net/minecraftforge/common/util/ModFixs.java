package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.datafix.IFixType;
import legacyconnector.v1_12_2.net.minecraft.util.datafix.IFixableData;

/** One mod's data fixes, applied to data saved with an older version of that mod (see {@link CompoundDataFixer}). */
public class ModFixs {
    final String mod;
    final int version;
    private final Map<IFixType, List<IFixableData>> fixes = new HashMap<>();

    public ModFixs() {
        this("minecraft", 0);
    }

    ModFixs(String mod, int version) {
        this.mod = mod;
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public synchronized void registerFix(IFixType type, IFixableData fixer) {
        int v = fixer.getFixVersion();
        if (v > version) throw new IllegalArgumentException("Can not add fixer for version " + v + " of " + mod + ": the mod is at version " + version);
        List<IFixableData> list = fixes.computeIfAbsent(type, k -> new ArrayList<>());
        int i = list.size();
        while (i > 0 && list.get(i - 1).getFixVersion() > v) i--;
        list.add(i, fixer);
    }

    synchronized List<IFixableData> getFixes(IFixType type) {
        return new ArrayList<>(fixes.getOrDefault(type, List.of()));
    }
}
