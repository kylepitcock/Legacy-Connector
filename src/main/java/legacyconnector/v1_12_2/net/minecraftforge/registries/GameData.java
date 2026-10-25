package legacyconnector.v1_12_2.net.minecraftforge.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;

/** Forge's registry bookkeeping; mods read the block to item association (Quark swaps entries in it). */
public class GameData {
    private static final BiMap<Block, Item> BLOCK_ITEM = HashBiMap.create();

    /** An item block was created for a block. */
    public static void mcc$blockItem(Block block, Item item) {
        synchronized (BLOCK_ITEM) {
            BLOCK_ITEM.forcePut(block, item);
        }
    }

    public static BiMap<Block, Item> getBlockItemMap() {
        return BLOCK_ITEM;
    }
}
