package legacyconnector.v1_6_4.net.minecraft.util;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The 1.6.4 numeric id facade: sparse id tables the way 1.6.4 had them ({@code Block.blocksList[4096]},
 * {@code Item.itemsList[32000]}, {@code BiomeGenBase.biomeList[256]}, {@code Potion.potionTypes[32]},
 * {@code Enchantment.enchantmentsList[256]}), filled with the vanilla ids from {@link VanillaIds} and then with the ids
 * mods request (their config files). A request that is taken, reserved by vanilla or out of range is logged and the object
 * moves to a free id (searched downwards from the top of the table, so later explicit requests of other mods rarely hit
 * an automatic one). Objects are opaque ({@code Object}): the runtime ones are the 1.7.10 shim's Block/Item/... instances, tests use strings.
 * <p>Block ids and item ids share the range below 4096 the way 1.6.4 did: block {@code n} owns the ItemBlock slot {@code n}
 * ({@link #claimBlockItem}); a plain item cannot take it.
 * <p>Self-contained: nothing here touches game classes; {@link Resolver} supplies the vanilla objects.
 */
public final class IdRegistry {
    public enum Kind {
        BLOCK(4096, 1), ITEM(32000, 4096), BIOME(256, 1), POTION(32, 1), ENCHANTMENT(256, 1);

        public final int size;
        /** Lowest id the automatic search will hand out (below that only vanilla and explicit requests). */
        public final int firstModdedId;

        Kind(int size, int firstModdedId) {
            this.size = size;
            this.firstModdedId = firstModdedId;
        }
    }

    /** Supplies the runtime object of a vanilla entry; may return null (id stays reserved but empty). */
    public interface Resolver {
        Object block(String registryName1710);

        Object item(String registryName1710);

        Object biome(String field);

        Object potion(String field);

        Object enchantment(String field);
    }

    private static final Object RESERVED = new Object() {
        @Override
        public String toString() {
            return "<reserved>";
        }
    };

    private static final class Table {
        final Kind kind;
        final Object[] slots;
        final String[] owner;
        final Map<Object, Integer> ids = new IdentityHashMap<>();
        int count;

        Table(Kind kind) {
            this.kind = kind;
            this.slots = new Object[kind.size];
            this.owner = new String[kind.size];
        }
    }

    private final Table[] tables = new Table[Kind.values().length];
    private Consumer<String> log = msg -> System.getLogger("legacyconnector.1.6.4.ids").log(System.Logger.Level.WARNING, msg);

    public IdRegistry() {
        for (Kind k : Kind.values()) tables[k.ordinal()] = new Table(k);
    }

    public synchronized IdRegistry logTo(Consumer<String> sink) {
        this.log = sink;
        return this;
    }

    /** A registry with every vanilla id of {@link VanillaIds} claimed (objects from {@code resolver}, null allowed). */
    public static IdRegistry vanilla(Resolver resolver) {
        IdRegistry r = new IdRegistry();
        r.fillVanilla(resolver);
        return r;
    }

    private void fillVanilla(Resolver rs) {
        VanillaIds v = VanillaIds.get();
        for (VanillaIds.Entry e : v.blocks) {
            Object o = e.name1710() == null ? null : rs.block(e.name1710());
            put(Kind.BLOCK, e.id(), o, "minecraft");
            // the block's ItemBlock keeps the same id; id 0 (air) has none
            if (e.id() != 0) {
                Object ib = e.name1710() == null ? null : rs.item(e.name1710());
                put(Kind.ITEM, e.id(), ib, "minecraft");
            }
        }
        for (VanillaIds.Entry e : v.items) {
            put(Kind.ITEM, e.id(), rs.item(e.name1710()), "minecraft");
        }
        for (VanillaIds.Entry e : v.biomes) put(Kind.BIOME, e.id(), rs.biome(e.name1710()), "minecraft");
        for (VanillaIds.Entry e : v.potions) put(Kind.POTION, e.id(), rs.potion(e.name1710()), "minecraft");
        for (VanillaIds.Entry e : v.enchantments) put(Kind.ENCHANTMENT, e.id(), rs.enchantment(e.name1710()), "minecraft");
    }

    /** Vanilla placement: the slot is taken even when {@code obj} is null. */
    private void put(Kind kind, int id, Object obj, String owner) {
        Table t = tables[kind.ordinal()];
        t.slots[id] = obj == null ? RESERVED : obj;
        t.owner[id] = owner;
        if (obj != null) t.ids.putIfAbsent(obj, id);
        t.count++;
    }

    // ------------------------------------------------------------------ claims

    /** Assigns an id to a mod block: the requested one when free, else a free one (logged). Re-claiming returns the old id. */
    public synchronized int claimBlock(Object block, int requestedId, String owner) {
        int id = claim(Kind.BLOCK, block, requestedId, owner, true);
        if (id >= 0) {
            // reserve the ItemBlock slot of the same id (a mod's own ItemBlock then fills it through claimBlockItem)
            Table it = tables[Kind.ITEM.ordinal()];
            if (it.slots[id] == null) {
                it.slots[id] = RESERVED;
                it.owner[id] = owner;
            }
        }
        return id;
    }

    /** Assigns an id to a mod item (never one below 4096 that a block owns). */
    public synchronized int claimItem(Object item, int requestedId, String owner) {
        return claim(Kind.ITEM, item, requestedId, owner, false);
    }

    /**
     * Frees an id whose object a mod removed from the table ({@code Block.blocksList[id] = null}); a reserved vanilla id with no
     * runtime object stays taken. Returns whether anything was freed.
     */
    public synchronized boolean release(Kind kind, int id) {
        Table t = tables[kind.ordinal()];
        if (id < 0 || id >= kind.size) return false;
        Object o = t.slots[id];
        if (o == null || o == RESERVED) return false;
        t.slots[id] = null;
        t.owner[id] = null;
        t.ids.remove(o);
        t.count--;
        if (kind == Kind.BLOCK) {
            // the ItemBlock slot goes with it unless a real ItemBlock sits there
            Table it = tables[Kind.ITEM.ordinal()];
            if (it.slots[id] == RESERVED) {
                it.slots[id] = null;
                it.owner[id] = null;
            }
        }
        return true;
    }

    /** Puts the ItemBlock of block {@code blockId} into its item slot (returns false if the slot holds another real item). */
    public synchronized boolean claimBlockItem(int blockId, Object itemBlock) {
        Table it = tables[Kind.ITEM.ordinal()];
        if (blockId < 0 || blockId >= Kind.BLOCK.size) return false;
        Object cur = it.slots[blockId];
        if (cur != null && cur != RESERVED && cur != itemBlock) return false;
        it.slots[blockId] = itemBlock;
        it.owner[blockId] = tables[Kind.BLOCK.ordinal()].owner[blockId];
        it.ids.putIfAbsent(itemBlock, blockId);
        if (cur == null) it.count++;
        return true;
    }

    public synchronized int claimBiome(Object biome, int requestedId, String owner) {
        return claim(Kind.BIOME, biome, requestedId, owner, false);
    }

    public synchronized int claimPotion(Object potion, int requestedId, String owner) {
        return claim(Kind.POTION, potion, requestedId, owner, false);
    }

    public synchronized int claimEnchantment(Object enchantment, int requestedId, String owner) {
        return claim(Kind.ENCHANTMENT, enchantment, requestedId, owner, false);
    }

    /**
     * @return the assigned id, or -1 when the table is full
     */
    private int claim(Kind kind, Object obj, int requested, String owner, boolean isBlock) {
        if (obj == null) throw new IllegalArgumentException("null " + kind + " claimed by " + owner);
        Table t = tables[kind.ordinal()];
        Integer have = t.ids.get(obj);
        if (have != null) return have;
        String what = kind.name().toLowerCase() + " " + obj;
        int id;
        if (requested >= 0 && requested < kind.size && isFree(t, requested)) {
            id = requested;
        } else {
            id = freeId(t);
            if (id < 0) {
                log.accept("No free " + kind.name().toLowerCase() + " id left for " + what + " of " + owner);
                return -1;
            }
            if (requested >= 0 && requested < kind.size) {
                log.accept("Id conflict: " + what + " of " + owner + " wanted " + requested + ", taken by "
                        + describe(t, requested) + "; moved to " + id);
            } else if (requested >= kind.size || requested < -1) {
                log.accept("Id out of range: " + what + " of " + owner + " wanted " + requested + " (limit " + kind.size + "); moved to " + id);
            }
        }
        t.slots[id] = obj;
        t.owner[id] = owner;
        t.ids.put(obj, id);
        t.count++;
        return id;
    }

    private boolean isFree(Table t, int id) {
        if (t.slots[id] != null) return false;
        // an id below 4096 is one slot shared by a block and its ItemBlock
        if (t.kind == Kind.ITEM && id < Kind.BLOCK.size) return tables[Kind.BLOCK.ordinal()].slots[id] == null;
        if (t.kind == Kind.BLOCK) return tables[Kind.ITEM.ordinal()].slots[id] == null;
        return true;
    }

    private int freeId(Table t) {
        for (int id = t.kind.size - 1; id >= t.kind.firstModdedId; id--) {
            if (isFree(t, id)) return id;
        }
        return -1;
    }

    private String describe(Table t, int id) {
        Object o = t.slots[id];
        if (o == null && t.kind == Kind.BLOCK) o = tables[Kind.ITEM.ordinal()].slots[id];
        if (o == null && t.kind == Kind.ITEM) o = tables[Kind.BLOCK.ordinal()].slots[id];
        return o + " (" + t.owner[id] + ")";
    }

    // ------------------------------------------------------------------ lookup

    /** Object at {@code id} or null (also for a vanilla id that is reserved but has no runtime object). */
    public synchronized Object get(Kind kind, int id) {
        Table t = tables[kind.ordinal()];
        if (id < 0 || id >= kind.size) return null;
        Object o = t.slots[id];
        return o == RESERVED ? null : o;
    }

    /** True when the id is taken, even by a reserved vanilla id with no object. */
    public synchronized boolean isTaken(Kind kind, int id) {
        return id >= 0 && id < kind.size && tables[kind.ordinal()].slots[id] != null;
    }

    /** Id of the object, or -1 when it was never claimed. */
    public synchronized int idOf(Kind kind, Object obj) {
        Integer i = tables[kind.ordinal()].ids.get(obj);
        return i == null ? -1 : i;
    }

    public synchronized String ownerOf(Kind kind, int id) {
        return id < 0 || id >= kind.size ? null : tables[kind.ordinal()].owner[id];
    }

    public synchronized int count(Kind kind) {
        return tables[kind.ordinal()].count;
    }

    public Object block(int id) { return get(Kind.BLOCK, id); }

    public Object item(int id) { return get(Kind.ITEM, id); }

    public Object biome(int id) { return get(Kind.BIOME, id); }

    public Object potion(int id) { return get(Kind.POTION, id); }

    public Object enchantment(int id) { return get(Kind.ENCHANTMENT, id); }

    public int blockId(Object block) { return idOf(Kind.BLOCK, block); }

    public int itemId(Object item) { return idOf(Kind.ITEM, item); }

    public int biomeId(Object biome) { return idOf(Kind.BIOME, biome); }
}
