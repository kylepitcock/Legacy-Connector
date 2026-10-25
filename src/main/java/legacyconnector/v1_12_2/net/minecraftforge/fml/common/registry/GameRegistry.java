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
        org.kylepitcock.legacyconnector.v1_12_2.bridge.RecipeBridge1122.addSmelting(input, output, xp);
    }

    public static void addSmelting(Item input, legacyconnector.v1_12_2.net.minecraft.item.ItemStack output, float xp) {
        addSmelting(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(input, 1, 32767), output, xp);
    }

    public static void addSmelting(Block input, legacyconnector.v1_12_2.net.minecraft.item.ItemStack output, float xp) {
        addSmelting(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(input, 1, 32767), output, xp);
    }

    public static void registerFuelHandler(Object handler) {
    }

    public static void registerWorldGenerator(Object generator, int modGenerationWeight) {
    }
}
