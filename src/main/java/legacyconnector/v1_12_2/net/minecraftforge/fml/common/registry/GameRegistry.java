package legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122;

public class GameRegistry {
    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, String id) {
        TileEntityBridge1122.registerClass(tileEntityClass, id);
    }

    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, ResourceLocation key) {
        TileEntityBridge1122.registerClass(tileEntityClass, key.toString());
        TileEntityBridge1122.registerClass(tileEntityClass, key.getPath());
    }

    @SuppressWarnings("unchecked")
    public static <T extends IForgeRegistryEntry<T>> IForgeRegistry<T> findRegistry(Class<T> registryType) {
        for (IForgeRegistry<?> reg : new IForgeRegistry<?>[] {ForgeRegistries.BLOCKS, ForgeRegistries.ITEMS, ForgeRegistries.POTIONS, ForgeRegistries.ENCHANTMENTS,
                ForgeRegistries.SOUND_EVENTS, ForgeRegistries.RECIPES, ForgeRegistries.BIOMES, ForgeRegistries.ENTITIES, ForgeRegistries.VILLAGER_PROFESSIONS,
                ForgeRegistries.POTION_TYPES}) {
            if (reg.getRegistrySuperType() == registryType) return (IForgeRegistry<T>) reg;
        }
        for (IForgeRegistry<?> reg : org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.customRegistries()) {
            if (reg.getRegistrySuperType() == registryType) return (IForgeRegistry<T>) reg;
        }
        for (IForgeRegistry<?> reg : org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.customRegistries()) {
            if (reg.getRegistrySuperType() != null && reg.getRegistrySuperType().getName().equals(registryType.getName())) return (IForgeRegistry<T>) reg;
        }
        String synthPrefix = "legacyconnector_synth.v1_12_2.";
        if (registryType.getName().startsWith(synthPrefix)) {
            // a registry typed by a synthesized class: typed by its legacy name when first asked for
            String legacy = registryType.getName().substring(synthPrefix.length());
            for (var reg : new org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.Reg<?>[] {
                    org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.VILLAGER_PROFESSIONS, org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.POTION_TYPES}) {
                if (reg.getRegistrySuperType() == null) reg.mcc$type(legacy);
                if (reg.getRegistrySuperType() != null && reg.getRegistrySuperType().getName().equals(registryType.getName())) return (IForgeRegistry<T>) reg;
            }
        }
        for (IForgeRegistry<?> reg : new IForgeRegistry<?>[] {ForgeRegistries.VILLAGER_PROFESSIONS, ForgeRegistries.POTION_TYPES}) {
            if (reg.getRegistrySuperType() != null && reg.getRegistrySuperType().getName().equals(registryType.getName())) return (IForgeRegistry<T>) reg;
        }
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/Registries").warn("No registry for {}", registryType.getName());
        return null;
    }

    /** Fills a static field with the registry object of that name once the registry events ran. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface ObjectHolder {
        String value();
    }

    public static void addShapedRecipe(ResourceLocation name, ResourceLocation group, legacyconnector.v1_12_2.net.minecraft.item.ItemStack output, Object... params) {
        var recipe = new legacyconnector.v1_12_2.net.minecraftforge.oredict.ShapedOreRecipe(group, output, params);
        recipe.setRegistryName(name);
        ForgeRegistries.RECIPES.register(recipe);
    }

    public static void addShapelessRecipe(ResourceLocation name, ResourceLocation group, legacyconnector.v1_12_2.net.minecraft.item.ItemStack output,
            legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient... components) {
        var list = legacyconnector.v1_12_2.net.minecraft.util.NonNullList.<legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient>create();
        for (var c : components) list.add(c);
        var recipe = new legacyconnector.v1_12_2.net.minecraft.item.crafting.ShapelessRecipes(group == null ? "" : group.toString(), output, list);
        recipe.setRegistryName(name);
        ForgeRegistries.RECIPES.register(recipe);
    }

    public static void addSmelting(legacyconnector.v1_12_2.net.minecraft.item.ItemStack input, legacyconnector.v1_12_2.net.minecraft.item.ItemStack output, float xp) {
        legacyconnector.v1_12_2.net.minecraft.item.crafting.FurnaceRecipes.instance().addSmeltingRecipe(input, output, xp);
    }

    public static void addSmelting(Item input, legacyconnector.v1_12_2.net.minecraft.item.ItemStack output, float xp) {
        addSmelting(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(input, 1, 32767), output, xp);
    }

    public static void addSmelting(Block input, legacyconnector.v1_12_2.net.minecraft.item.ItemStack output, float xp) {
        addSmelting(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(input, 1, 32767), output, xp);
    }

    private static final java.util.List<legacyconnector.v1_12_2.net.minecraftforge.fml.common.IFuelHandler> FUEL_HANDLERS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final java.util.List<legacyconnector.v1_12_2.net.minecraftforge.fml.common.IWorldGenerator> WORLD_GENERATORS = new java.util.ArrayList<>();
    private static final java.util.Map<legacyconnector.v1_12_2.net.minecraftforge.fml.common.IWorldGenerator, Integer> WORLD_GENERATOR_WEIGHTS = new java.util.IdentityHashMap<>();

    /** The handler is asked for every fuel the modern furnaces look up (see {@code Fuel1122}). */
    public static void registerFuelHandler(legacyconnector.v1_12_2.net.minecraftforge.fml.common.IFuelHandler handler) {
        if (handler != null) FUEL_HANDLERS.add(handler);
    }

    /** The highest burn time any registered fuel handler gives the stack. */
    public static int getFuelValue(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        int value = 0;
        for (var h : FUEL_HANDLERS) value = Math.max(value, h.getBurnTime(stack));
        return value;
    }

    public static boolean mcc$hasFuelHandlers() {
        return !FUEL_HANDLERS.isEmpty();
    }

    /** Kept in Forge's order (by weight, then registration); they run once the layer generates 1.12 chunks. */
    public static synchronized void registerWorldGenerator(legacyconnector.v1_12_2.net.minecraftforge.fml.common.IWorldGenerator generator, int modGenerationWeight) {
        if (generator == null || WORLD_GENERATOR_WEIGHTS.containsKey(generator)) return;
        WORLD_GENERATOR_WEIGHTS.put(generator, modGenerationWeight);
        WORLD_GENERATORS.add(generator);
        WORLD_GENERATORS.sort(java.util.Comparator.comparingInt(WORLD_GENERATOR_WEIGHTS::get));
    }

    public static synchronized java.util.List<legacyconnector.v1_12_2.net.minecraftforge.fml.common.IWorldGenerator> mcc$worldGenerators() {
        return new java.util.ArrayList<>(WORLD_GENERATORS);
    }

    /** A stack of a named item ("modid:name"), with metadata, size and an NBT tag in JSON form (null or empty for none). */
    public static legacyconnector.v1_12_2.net.minecraft.item.ItemStack makeItemStack(String itemName, int meta, int stackSize, String nbtString) {
        if (itemName == null) throw new IllegalArgumentException("The itemName cannot be null");
        Item item = Item.getByNameOrId(itemName);
        if (item == null) throw new IllegalArgumentException("Unable to find item with name " + itemName);
        var stack = new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, stackSize, meta);
        if (nbtString != null && !nbtString.isEmpty()) {
            try {
                stack.setTagCompound(legacyconnector.v1_12_2.net.minecraft.nbt.JsonToNBT.getTagFromJson(nbtString));
            } catch (legacyconnector.v1_12_2.net.minecraft.nbt.NBTException e) {
                throw new RuntimeException("Encountered an exception parsing ItemStack NBT string " + nbtString, e);
            }
        }
        return stack;
    }

    private static final java.util.Map<String, legacyconnector.v1_12_2.net.minecraftforge.fml.common.IEntitySelectorFactory> ENTITY_SELECTORS =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Registers a mod's own target selector arguments (a selector with them is filtered by the factory's predicates). */
    public static void registerEntitySelector(legacyconnector.v1_12_2.net.minecraftforge.fml.common.IEntitySelectorFactory factory, String... args) {
        for (String arg : args) ENTITY_SELECTORS.put(arg, factory);
    }

    /** The factories of the mods' selector arguments, by argument. */
    public static java.util.Map<String, legacyconnector.v1_12_2.net.minecraftforge.fml.common.IEntitySelectorFactory> mcc$entitySelectors() {
        return ENTITY_SELECTORS;
    }
}
