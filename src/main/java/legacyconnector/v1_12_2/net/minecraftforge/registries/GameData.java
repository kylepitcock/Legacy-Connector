package legacyconnector.v1_12_2.net.minecraftforge.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;

/** Forge's registry bookkeeping; mods read the block to item association (Quark swaps entries in it). */
public class GameData {
    /** The registry names (RegistryManager.ACTIVE.getRegistry(GameData.RECIPES): Cucumber). */
    public static final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation BLOCKS = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft:blocks");
    public static final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation ITEMS = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft:items");
    public static final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation POTIONS = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft:potions");
    public static final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation BIOMES = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft:biomes");
    public static final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation SOUNDEVENTS = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft:soundevents");
    public static final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation POTIONTYPES = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft:potiontypes");
    public static final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation ENCHANTMENTS = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft:enchantments");
    public static final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation ENTITIES = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft:entities");
    public static final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation RECIPES = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft:recipes");
    public static final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation PROFESSIONS = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("minecraft:villagerprofessions");

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

    /** Block state ids ({@code Block.getStateId}/{@code getStateById}) as 1.12.2's id map. */
    private static final legacyconnector.v1_12_2.net.minecraft.util.ObjectIntIdentityMap<legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState> BLOCK_STATE_IDS =
            new legacyconnector.v1_12_2.net.minecraft.util.ObjectIntIdentityMap<>() {
                @Override
                public int get(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
                    return state == null ? -1 : Block.getStateId(state);
                }

                @Override
                public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getByValue(int id) {
                    return Block.getStateById(id);
                }

                @Override
                public void put(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState key, int value) {
                    // the ids are computed from the block's id and metadata
                }
            };

    public static legacyconnector.v1_12_2.net.minecraft.util.ObjectIntIdentityMap<legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState> getBlockStateIDMap() {
        return BLOCK_STATE_IDS;
    }
}
