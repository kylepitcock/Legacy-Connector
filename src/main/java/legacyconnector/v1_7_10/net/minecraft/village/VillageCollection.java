package legacyconnector.v1_7_10.net.minecraft.village;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7.10's per-world village registry. Modern villages are POI based, so this reports none: mods that list or search
 * villages (OpenBlocks' highlighter, mob AI) see an empty world instead of a null collection.
 */
public class VillageCollection {
    private final List<Object> villageList = new ArrayList<>();

    public List<Object> getVillageList() {
        return villageList;
    }

    public Object findNearestVillage(int x, int y, int z, int radius) {
        return null;
    }

    public void addVillagerPosition(int x, int y, int z) {
    }

    public void tick() {
    }
}
