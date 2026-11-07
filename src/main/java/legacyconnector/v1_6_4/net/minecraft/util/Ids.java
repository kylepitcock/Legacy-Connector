package legacyconnector.v1_6_4.net.minecraft.util;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge;

/**
 * The runtime id tables of 1.6.4 mods: one {@link IdRegistry} (filled with the vanilla ids on first use, then with what mods
 * construct) plus the typed arrays {@code Block.blocksList} / {@code Item.itemsList} the legacy code indexes. Mods write their
 * ids as constructor arguments; the facades of Block, Item, ItemStack and World (packages below
 * {@code legacyconnector.v1_6_4}) call in here.
 * <p>The arrays are mirrors kept in step with the registry at every claim. A mod that nulls an entry ({@code blocksList[id] = null}
 * to replace a vanilla block) frees the id for its own claim. Air is id 0 in the registry but {@code blocksList[0]} stays null, as in 1.6.4.
 * Objects that nobody claimed (blocks of 1.7.10 mods, modern blocks) get an id the first time one is asked for.
 */
public final class Ids {
    private static IdRegistry registry;
    private static Block[] blocks;
    private static Item[] items;
    /** Block ids whose ItemBlock was not there yet when the block was claimed. */
    private static final List<Integer> pendingItemBlocks = new ArrayList<>();

    private Ids() {}

    private static synchronized IdRegistry reg() {
        if (registry == null) {
            IdRegistry r = IdRegistry.vanilla(new ShimResolver());
            Block[] b = new Block[IdRegistry.Kind.BLOCK.size];
            Item[] it = new Item[IdRegistry.Kind.ITEM.size];
            for (int id = 1; id < b.length; id++) b[id] = (Block) r.block(id);
            for (int id = 0; id < it.length; id++) it[id] = (Item) r.item(id);
            blocks = b;
            items = it;
            registry = r;
            // chunk generators that work on block ids (MapGenBase.recursiveGenerate with a byte[]) read the same tables
            legacyconnector.v1_7_10.net.minecraft.world.gen.MapGenBase.mcc$blockById = Ids::block;
            legacyconnector.v1_7_10.net.minecraft.world.gen.MapGenBase.mcc$idOfBlock = Ids::claimedBlockId;
        }
        return registry;
    }

    /** Test hook: replaces the tables (a registry without the game). */
    static synchronized void useForTest(IdRegistry r, Block[] b, Item[] i) {
        registry = r;
        blocks = b;
        items = i;
    }

    // ------------------------------------------------------------------ blocks

    /** {@code Block.blocksList}, the live mirror. */
    public static synchronized Block[] blocksList() {
        reg();
        return blocks;
    }

    /** {@code Block.blocksList[id]}: null for air, an unused id or an id out of range. */
    public static synchronized Block block(int id) {
        reg();
        return id > 0 && id < blocks.length ? blocks[id] : null;
    }

    /** Id of a block (0 for null and air); a block nobody claimed gets the next free one. */
    public static synchronized int blockId(Block block) {
        if (block == null) return 0;
        IdRegistry r = reg();
        int id = r.blockId(block);
        if (id >= 0) return id;
        return claimBlockInternal(block, -1);
    }

    /** Id of a block that has one already, else -1 (never claims: for lookups on hot paths over every block). */
    public static synchronized int claimedBlockId(Block block) {
        if (block == null) return 0;
        return reg().blockId(block);
    }

    /** The claim a Block constructor makes: {@code super(id, material)}. */
    public static synchronized void claimBlock(Block block, int requestedId) {
        reg();
        if (r().blockId(block) >= 0) return;
        claimBlockInternal(block, requestedId);
    }

    private static IdRegistry r() {
        return registry;
    }

    private static int claimBlockInternal(Block block, int requested) {
        // a mod that cleared the entry (to replace a vanilla block) frees its id
        if (requested > 0 && requested < blocks.length && blocks[requested] == null && registry.release(IdRegistry.Kind.BLOCK, requested)) {
            items[requested] = null; // its ItemBlock leaves with it
        }
        int id = registry.claimBlock(block, requested, ownerLabel());
        if (id >= 0) {
            blocks[id] = block;
            pendingItemBlocks.add(id);
        }
        return id;
    }

    // ------------------------------------------------------------------ items

    /** {@code Item.itemsList}, the live mirror (ItemBlocks of blocks registered since the last call are filled in). */
    public static synchronized Item[] itemsList() {
        reg();
        resolvePendingItemBlocks();
        return items;
    }

    public static synchronized Item item(int id) {
        reg();
        if (id < 0 || id >= items.length) return null;
        if (items[id] == null && id < blocks.length && id > 0) resolvePendingItemBlocks();
        return items[id];
    }

    /** Id of an item (0 for null); an ItemBlock has its block's id, any other unclaimed item gets a free one. */
    public static synchronized int itemId(Item item) {
        if (item == null) return 0;
        IdRegistry r = reg();
        int id = r.itemId(item);
        if (id >= 0) return id;
        if (item instanceof ItemBlock ib && ib.mcc$block() != null) {
            int blockId = blockId(ib.mcc$block());
            if (blockId > 0 && r.claimBlockItem(blockId, item)) {
                items[blockId] = item;
                return blockId;
            }
        }
        return claimItemInternal(item, -1);
    }

    /** The claim an Item constructor makes: {@code super(id)} puts the item at {@code 256 + id} (an ItemBlock at its block's id). */
    public static synchronized void claimItem(Item item, int constructorId) {
        IdRegistry r = reg();
        if (r.itemId(item) >= 0) return;
        int requested = constructorId + 256;
        if (requested < blocks.length && r.isTaken(IdRegistry.Kind.BLOCK, requested) && item instanceof ItemBlock) {
            if (r.claimBlockItem(requested, item)) {
                items[requested] = item;
                return;
            }
        }
        claimItemInternal(item, requested);
    }

    private static int claimItemInternal(Item item, int requested) {
        if (requested > 0 && requested < items.length && items[requested] == null) registry.release(IdRegistry.Kind.ITEM, requested);
        int id = registry.claimItem(item, requested, ownerLabel());
        if (id >= 0) items[id] = item;
        return id;
    }

    /** ItemBlocks of blocks claimed since the last call: the bridge creates them when the block is registered, after the claim. */
    private static void resolvePendingItemBlocks() {
        if (pendingItemBlocks.isEmpty()) return;
        List<Integer> still = new ArrayList<>();
        for (int blockId : pendingItemBlocks) {
            if (items[blockId] != null && registry.get(IdRegistry.Kind.ITEM, blockId) != null) continue;
            Block b = blocks[blockId];
            Item ib = null;
            try {
                ib = b == null ? null : ItemBridge.fromBlock(b);
            } catch (RuntimeException ignored) {
                // not registered yet
            }
            if (ib != null && registry.claimBlockItem(blockId, ib)) items[blockId] = ib;
            else if (ib == null) still.add(blockId);
        }
        pendingItemBlocks.clear();
        pendingItemBlocks.addAll(still);
    }

    private static String ownerLabel() {
        return "a 1.6.4 mod";
    }

    // ------------------------------------------------------------------ config ids

    /** Whether a block id is unused (Forge's {@code Configuration.getBlock} avoids taken ids). A mod that nulled the entry counts as free. */
    public static synchronized boolean blockIdFree(int id) {
        IdRegistry r = reg();
        if (id <= 0 || id >= blocks.length) return false;
        return r.isFreeId(IdRegistry.Kind.BLOCK, id) || (blocks[id] == null && r.get(IdRegistry.Kind.BLOCK, id) != null && id != 0);
    }

    public static synchronized boolean itemIdFree(int id) {
        IdRegistry r = reg();
        if (id <= 0 || id >= items.length) return false;
        return r.isFreeId(IdRegistry.Kind.ITEM, id);
    }

    public static int blockTableSize() {
        return IdRegistry.Kind.BLOCK.size;
    }

    public static int itemTableSize() {
        return IdRegistry.Kind.ITEM.size;
    }

    // ------------------------------------------------------------------ misc

    /** A vanilla block by 1.7.10 registry name, for the statics the shim's {@code Blocks} lacks; null if unknown. */
    public static Object vanillaBlock(String name) {
        try {
            return org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.byName(name);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Object vanillaItem(String name) {
        try {
            return ItemBridge.byName(name);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The object when it is an instance of {@code type}, else null (a vanilla wrapper of another class than the 1.6.4 field type). */
    public static <T> T as(Class<T> type, Object value) {
        return type.isInstance(value) ? type.cast(value) : null;
    }
}
