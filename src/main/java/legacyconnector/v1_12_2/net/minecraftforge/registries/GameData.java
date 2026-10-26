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

    /** {@code GameRegistry.register}'s implementation: the entry goes into the registry of its type (an unknown type is logged, not thrown). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <V extends IForgeRegistryEntry<V>> V register_impl(V value) {
        if (value == null) throw new IllegalArgumentException("Attempted to register a null object");
        if (value.getRegistryName() == null) throw new IllegalArgumentException("Attempted to register an object without a registry name: " + value);
        IForgeRegistry registry = legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.GameRegistry.findRegistry((Class) value.getRegistryType());
        if (registry != null) registry.register(value); // findRegistry logged a registry the layer does not have
        return value;
    }
}
