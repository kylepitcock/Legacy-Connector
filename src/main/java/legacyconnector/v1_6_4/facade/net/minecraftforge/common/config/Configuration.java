package legacyconnector.v1_6_4.facade.net.minecraftforge.common.config;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;
import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraftforge.common.config.Property;

/**
 * Facade of the 1.7.10 shim's {@code Configuration} for 1.6.4 mods: {@code getBlock}, {@code getItem} and {@code getTerrainBlock}, the calls
 * that read a mod's block and item ids from its config file. As in Forge 9.11: a value in the file wins; else the default id if it is free
 * (and not handed out before from this file), else the next free one, which is written back. Item defaults below 256 are shifted by 256 and
 * the shift is removed from the stored value (mods subtract 256 themselves in {@code new Item(id - 256)}). See {@code Hierarchy.facadeOf}.
 */
public final class Configuration {
    private Configuration() {}

    private static final Map<legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration, Map<String, BitSet>> MARKERS = new WeakHashMap<>();

    private static synchronized BitSet markers(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String category, int size) {
        BitSet b = MARKERS.computeIfAbsent(self, k -> new HashMap<>()).computeIfAbsent(category, k -> new BitSet(size));
        return b;
    }

    /**
     * Ids handed out by any config file so far, per table (block, item): mods read their ids in preInit but construct the blocks later, so
     * the registry alone does not show that another mod's config already took an id (two 1.4.7 mods with the default 220 each found it free).
     * Forge's own markers were per file and the clash was a crash; here the later default moves to a free id.
     */
    private static final Map<String, BitSet> HANDED_OUT = new HashMap<>();
    private static final Map<String, String> HANDED_BY = new HashMap<>();

    private static BitSet handedOut(int size, boolean block) {
        return HANDED_OUT.computeIfAbsent(block ? "block" : "item", k -> new BitSet(size));
    }

    private static void noteSharedId(String what, legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String category, String key, int id,
            String message) {
        try {
            org.kylepitcock.legacyconnector.core.degraded.Degraded.note(org.kylepitcock.legacyconnector.core.degraded.Degraded.Category.ID_CONFLICT,
                    self.toString(), what + " " + id + " " + category + "." + key, message);
        } catch (RuntimeException | LinkageError e) {
            // no runtime collector (unit tests)
        }
    }

    /** {@code getBlock(key, defaultId, comment)}. */
    public static Property getBlock(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String key, int defaultId, String comment) {
        return getBlock(self, "block", key, defaultId, comment);
    }

    /** {@code getItem(key, defaultId, comment)}. */
    public static Property getItem(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String key, int defaultId, String comment) {
        return getItem(self, "item", key, defaultId, comment);
    }

    /** Forge 1.4's names of the block and item id readers. */
    public static Property getOrCreateBlockIdProperty(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String key, int defaultId) {
        return getBlock(self, "block", key, defaultId, null);
    }

    public static Property getOrCreateIntProperty(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String key, String category, int defaultValue) {
        return self.get(category, key, defaultValue);
    }

    public static Property getOrCreateBooleanProperty(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String key, String category,
            boolean defaultValue) {
        return self.get(category, key, defaultValue);
    }

    public static Property getOrCreateProperty(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String key, String category,
            String defaultValue) {
        return self.get(category, key, defaultValue);
    }

    public static Property getBlock(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String key, int defaultId) {
        return getBlock(self, "block", key, defaultId, null);
    }



    public static Property getBlock(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String category, String key, int defaultId) {
        return getBlock(self, category, key, defaultId, null);
    }

    public static Property getBlock(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String category, String key, int defaultId,
            String comment) {
        return pick(self, category, key, defaultId, comment, Ids.blockTableSize(), 256, 0, Ids::blockIdFree);
    }

    public static Property getTerrainBlock(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String category, String key,
            int defaultId, String comment) {
        return pick(self, category, key, defaultId, comment, Ids.blockTableSize(), 1, 0, Ids::blockIdFree);
    }

    public static Property getItem(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String key, int defaultId) {
        return getItem(self, "item", key, defaultId, null);
    }

    public static Property getItem(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String category, String key, int defaultId) {
        return getItem(self, category, key, defaultId, null);
    }

    public static Property getItem(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String category, String key, int defaultId,
            String comment) {
        int shift = defaultId < 256 ? 256 : 0;
        return pick(self, category, key, defaultId + shift, comment, Ids.itemTableSize(), defaultId + shift + 1, shift, Ids::itemIdFree);
    }

    /**
     * @param searchFrom where the search for a free id starts when the default is taken
     * @param shift      removed from the id before it is stored
     */
    private static Property pick(legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration self, String category, String key, int defaultId,
            String comment, int size, int searchFrom, int shift, IntPredicate free) {
        Property prop = self.get(category, key, -1, comment);
        BitSet used = markers(self, category, size);
        boolean block = size == Ids.blockTableSize();
        String what = block ? "block" : "item";
        String me = self + "|" + category + "." + key;
        int stored = prop.getInt();
        synchronized (Configuration.class) {
            BitSet all = handedOut(size, block);
            if (stored != -1) {
                if (stored + shift >= 0) {
                    used.set(stored + shift);
                    String by = HANDED_BY.get(what + (stored + shift));
                    if (by != null && !by.equals(me)) {
                        noteSharedId(what, self, category, key, stored + shift, "the " + what + " id " + (stored + shift) + " of " + self + " (" + category + "." + key
                                + ") is also in " + by.replace('|', ' ') + ": change one of the two ids in the config files, else a mod that checks its own id"
                                + " finds the other's block or item and fails to load");
                    }
                    all.set(stored + shift);
                    HANDED_BY.putIfAbsent(what + (stored + shift), me);
                }
                return prop;
            }
            if (defaultId > 0 && defaultId < size && free.test(defaultId) && !used.get(defaultId) && !all.get(defaultId)) {
                prop.setValue(defaultId - shift);
                used.set(defaultId);
                all.set(defaultId);
                HANDED_BY.putIfAbsent(what + defaultId, me);
                return prop;
            }
            for (int j = Math.max(searchFrom, 1); j < size; j++) {
                if (free.test(j) && !used.get(j) && !all.get(j)) {
                    if (defaultId > 0 && defaultId < size && all.get(defaultId) && free.test(defaultId)) {
                        noteSharedId(what, self, category, key, defaultId, "the default " + what + " id " + defaultId + " of " + self + " (" + category + "." + key
                                + ") was already handed to " + String.valueOf(HANDED_BY.get(what + defaultId)).replace('|', ' ') + "; moved to " + j
                                + " and written to the config file");
                    }
                    prop.setValue(j - shift);
                    used.set(j);
                    all.set(j);
                    HANDED_BY.putIfAbsent(what + j, me);
                    return prop;
                }
            }
        }
        throw new RuntimeException("No more ids available for " + key);
    }
}
